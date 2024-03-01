package ethereum.history

import ethereum.collections.Hash
import ethereum.crypto.ECDSASignature
import ethereum.rlp.RLPEncoder
import ethereum.rlp.toRlp
import ethereum.type.AccessListTransaction
import ethereum.type.LegacyTransaction
import ethereum.type.Transaction
import java.math.BigInteger

/**
 * [Optional access lists](https://eips.ethereum.org/EIPS/eip-2930)
 *
 * require [EIP2718], [EIP2929]
 */
object EIP2930 {

    class Signer(chainId: ULong) : EIP155.Signer(chainId) {
        override fun signatureValues(txType: Byte, sig: ByteArray): ECDSASignature {
            if (txType == LegacyTransaction.TYPE) {
                return super.signatureValues(txType, sig)
            }
            if (txType == AccessListTransaction.TYPE) {
                val ecdsa = ECDSASignature.fromBytes(sig)
                return ECDSASignature.Mutable(ecdsa.r, ecdsa.s, BigInteger(1, byteArrayOf(sig[64])))
            }
            error("ErrTxTypeNotSupported")
        }

        override fun hash(tx: Transaction): Hash {
            if (tx.inner.txType == LegacyTransaction.TYPE) {
                return super.hash(tx)
            }
            if (tx.inner.txType == AccessListTransaction.TYPE) {
                return RLPEncoder.encode {
                    addByte(tx.inner.txType)
                    addArray {
                        addULong(chainId)
                        addULong(tx.inner.nonce)
                        addBigInt(tx.inner.gasPrice)
                        addULong(tx.inner.gas)
                        addBytes(tx.inner.to.bytes)
                        addBigInt(tx.inner.value)
                        addBytes(tx.inner.data ?: byteArrayOf())
                        addBytes(tx.inner.accessList.toRlp())
                    }
                }.let { Hash.keccak256FromBytes(it) }
            }
            error("ErrTxTypeNotSupported")
        }
    }
}