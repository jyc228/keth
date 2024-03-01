package ethereum.history.fork

import ethereum.consensus.EthashEngin
import ethereum.type.BlockHeader
import ethereum.type.Signer
import java.math.BigInteger
import kotlin.math.max

object HomesteadHardFork : Signer by FrontierHardFork {

    /**
     * It returns the difficulty that a new block should have when created at time given the parent block's time and difficulty.
     * The calculation uses the Homestead rules.
     *
     * [eip2](https://github.com/ethereum/EIPs/blob/master/EIPS/eip-2.md)
     * ```python
     * diff = parent_diff
     *   + (parent_diff / 2048 * max(1 - (block_timestamp - parent_timestamp) // 10, -99))
     *   + 2^(periodCount - 2)
     * ```
     */
    fun calcDifficulty(time: ULong, parent: BlockHeader): BigInteger {
        val parentDiff = parent.difficulty ?: BigInteger.ZERO
        val x = max(1 - (time.toLong() - parent.time.toLong()) / 10, -99).toBigInteger()
        var diff = parentDiff + (parentDiff / EthashEngin.difficultyBoundDivisor * x)

        // minimum difficulty can ever be (before exponential factor)
        diff = diff.min(EthashEngin.minimumDifficulty)

        // for the exponential factor
        val periodCount = (parent.number + 1u) / EthashEngin.expDiffPeriod

        // the exponential factor, commonly referred to as "the bomb"
        // diff = diff + 2^(periodCount - 2)
        if (periodCount > 0u) {
            diff += (periodCount - 2u).toString().toBigInteger().pow(2)
        }
        return diff
    }
}