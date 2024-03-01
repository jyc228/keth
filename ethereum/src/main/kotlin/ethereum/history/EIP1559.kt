package ethereum.history

import ethereum.config.ForkConfig
import ethereum.type.BlockHeader
import java.math.BigInteger

object EIP1559 {
    val initialBaseFee: BigInteger = 1000000000.toBigInteger()
    val elasticityMultiplier: BigInteger = BigInteger.TWO
    private val baseFeeChangeDenominator: BigInteger = BigInteger.valueOf(8)
    val gasLimitBoundDivisor: BigInteger = BigInteger.valueOf(1024)
    val minGasLimit: BigInteger = BigInteger.valueOf(5000)

    fun verifyHeader(config: ForkConfig, parent: BlockHeader, header: BlockHeader) {
        val parentGasLimit =
            parent.gasLimit.takeIf { config.london.forked(parent.number) } ?: (parent.gasLimit * elasticityMultiplier)
        verifyGaslimit(parentGasLimit, header.gasLimit)
        if (header.baseFee != computeBaseFee(config, parent)) {
            error("invalid baseFee: have %s, want %s, parentBaseFee %s, parentGasUsed %d")
        }
    }

    /**
     * [computeGasLimit] computes the gas limit of the next block after parent.
     * It aims to keep the baseline gas close to the provided target,
     * and increase it towards the target if the baseline gas is lower.
     */
    fun computeGasLimit(parentGasLimit: BigInteger, desiredLimit: BigInteger): BigInteger {
        val delta = parentGasLimit / gasLimitBoundDivisor
        val validDesiredLimit = minGasLimit.max(desiredLimit)
        return when {
            parentGasLimit < validDesiredLimit -> desiredLimit.max(parentGasLimit + delta)
            parentGasLimit > validDesiredLimit -> desiredLimit.max(parentGasLimit - delta)
            else -> parentGasLimit
        }
    }

    fun computeBaseFee(config: ForkConfig, parent: BlockHeader): BigInteger {
        if (!config.london.forked(parent.number)) return initialBaseFee
        requireNotNull(parent.baseFee)
        val parentGasTarget = parent.gasLimit / elasticityMultiplier
        return when {
            // If the parent block used more gas than its target, the baseFee should increase.
            // max(1, parentBaseFee * gasUsedDelta / parentGasTarget / baseFeeChangeDenominator)
            parent.gasUsed > parentGasTarget -> {
                val gasUsedDelta = parent.gasUsed - parentGasTarget
                val baseFee = parent.baseFee * gasUsedDelta / parentGasTarget / baseFeeChangeDenominator
                parent.baseFee + BigInteger.ONE.max(baseFee)
            }

            // Otherwise if the parent block used less gas than its target, the baseFee should decrease.
            // max(0, parentBaseFee * gasUsedDelta / parentGasTarget / baseFeeChangeDenominator)
            parent.gasUsed < parentGasTarget -> {
                val gasUsedDelta = parentGasTarget - parent.gasUsed
                val baseFee = parent.baseFee * gasUsedDelta / parentGasTarget / baseFeeChangeDenominator
                BigInteger.ZERO.max(parent.baseFee - baseFee)
            }

            // If the parent gasUsed is the same as the target, the baseFee remains unchanged.
            else -> parent.baseFee
        }
    }

    fun verifyGaslimit(parentGasLimit: BigInteger, headerGasLimit: BigInteger) {
        // Verify that the gas limit remains within allowed bounds
        val diff = (parentGasLimit - headerGasLimit).abs()
        val limit = parentGasLimit / gasLimitBoundDivisor
        if (diff >= limit) {
            error("invalid gas limit: have $headerGasLimit, want $parentGasLimit +-= ${limit - BigInteger.ONE}")
        }
        if (headerGasLimit < minGasLimit) {
            error("invalid gas limit below 5000")
        }
    }
}