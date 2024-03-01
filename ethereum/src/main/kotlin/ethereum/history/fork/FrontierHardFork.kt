package ethereum.history.fork

import ethereum.collections.Hash
import ethereum.crypto.ECDSASignature
import ethereum.rlp.RLPEncoder
import ethereum.type.BlockHeader
import ethereum.type.LegacyTransaction
import ethereum.type.Signer
import ethereum.type.Transaction
import java.math.BigInteger

object FrontierHardFork : Signer {
    // Per byte of data attached to a transaction that is not equal to zero. NOTE: Not payable on data of calls between transactions.
    val txDataNonZeroGas: BigInteger = 68.toBigInteger()
    val blockReward: BigInteger = 5e+18.toBigDecimal().toBigIntegerExact()

    /**
     * It returns the difficulty that a new block should have when created at time given the parent block's time and difficulty.
     *
     * The calculation uses the Frontier rules.
     */
    fun calcDifficulty(time: ULong, parent: BlockHeader): BigInteger {
//        diff := new(big.Int)
//        adjust := new(big.Int).Div(parent.Difficulty, params.DifficultyBoundDivisor)
//        bigTime := new(big.Int)
//        bigParentTime := new(big.Int)
//
//        bigTime.SetUint64(time)
//        bigParentTime.SetUint64(parent.Time)
//
//        if bigTime.Sub(bigTime, bigParentTime).Cmp(params.DurationLimit) < 0 {
//            diff.Add(parent.Difficulty, adjust)
//        } else {
//            diff.Sub(parent.Difficulty, adjust)
//        }
//        if diff.Cmp(params.MinimumDifficulty) < 0 {
//            diff.Set(params.MinimumDifficulty)
//        }
//
//        periodCount := new(big.Int).Add(parent.Number, big1)
//        periodCount.Div(periodCount, expDiffPeriod)
//        if periodCount.Cmp(big1) > 0 {
//            // diff = diff + 2^(periodCount - 2)
//            expDiff := periodCount.Sub(periodCount, big2)
//            expDiff.Exp(big2, expDiff, nil)
//            diff.Add(diff, expDiff)
//            diff = math.BigMax(diff, params.MinimumDifficulty)
//        }
//        return diff
        return BigInteger.ZERO
    }

    override fun signatureValues(txType: Byte, sig: ByteArray): ECDSASignature {
        require(txType == LegacyTransaction.TYPE) { "ErrTxTypeNotSupported" }
        return ECDSASignature.fromBytes(sig)
    }

    override fun hash(tx: Transaction): Hash {
        return RLPEncoder.encodeArray {
            addULong(tx.inner.nonce)
            addBigInt(tx.inner.gasPrice)
            addULong(tx.inner.gas)
            addBytes(tx.inner.to.bytes)
            addBigInt(tx.inner.value)
            addBytes(tx.inner.data ?: byteArrayOf())
        }.let { Hash.keccak256FromBytes(it) }
    }
}