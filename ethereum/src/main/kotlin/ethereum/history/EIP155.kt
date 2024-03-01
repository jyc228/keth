package ethereum.history

import ethereum.collections.Hash
import ethereum.crypto.ECDSASignature
import ethereum.history.fork.FrontierHardFork
import ethereum.rlp.RLPEncoder
import ethereum.type.LegacyTransaction
import ethereum.type.Transaction
import java.math.BigInteger

/**
 * https://eips.ethereum.org/EIPS/eip-155
 */
object EIP155 {

    open class Signer(val chainId: ULong) : ethereum.type.Signer {
        val vMultiplier = chainId.toString().toBigInteger() * BigInteger.TWO
        override fun signatureValues(txType: Byte, sig: ByteArray): ECDSASignature {
            require(txType == LegacyTransaction.TYPE) { "ErrTxTypeNotSupported" }
            val ecdsa = FrontierHardFork.signatureValues(txType, sig)
            if (chainId > 0u) {
                val v = BigInteger(1, byteArrayOf((sig[64] + 35).toByte())) * vMultiplier
                return ECDSASignature.Mutable(ecdsa.r, ecdsa.s, v)
            }
            return ecdsa
        }

        override fun hash(tx: Transaction): Hash {
            return RLPEncoder.encodeArray {
                addULong(tx.inner.nonce)
                addBigInt(tx.inner.gasPrice)
                addULong(tx.inner.gas)
                addBytes(tx.inner.to.bytes)
                addBigInt(tx.inner.value)
                addBytes(tx.inner.data ?: byteArrayOf())
                addULong(chainId)
                addULong(0u)
                addULong(0u)
            }.let { Hash.keccak256FromBytes(it) }
        }
    }
}