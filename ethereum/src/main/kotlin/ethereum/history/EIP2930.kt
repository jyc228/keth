package ethereum.history

import ethereum.type.keccak256
import io.github.jyc228.ethereum.ECDSASignature
import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.HexBigInt
import io.github.jyc228.ethereum.Transaction
import io.github.jyc228.ethereum.TransactionRlp
import io.github.jyc228.ethereum.TransactionType
import java.math.BigInteger

/**
 * [Optional access lists](https://eips.ethereum.org/EIPS/eip-2930)
 *
 * require [EIP2718], [EIP2929]
 */
object EIP2930 {

    class Signer(chainId: ULong) : EIP155.Signer(chainId) {
        override fun signatureValues(txType: TransactionType, sig: ByteArray): ECDSASignature {
            if (txType == TransactionType.Legacy) {
                return super.signatureValues(txType, sig)
            }
            if (txType == TransactionType.AccessList) {
                val ecdsa = ECDSASignature.fromBytes(sig)
                return ECDSASignature.Mutable(ecdsa.r, ecdsa.s, HexBigInt(BigInteger(1, byteArrayOf(sig[64]))))
            }
            error("ErrTxTypeNotSupported")
        }

        override fun hash(tx: Transaction): Hash {
            if (tx.type == TransactionType.Legacy) {
                return super.hash(tx)
            }
            if (tx.type == TransactionType.AccessList) {
                return Hash.keccak256(TransactionRlp.encode(tx, withSignature = false))
            }
            error("ErrTxTypeNotSupported")
        }
    }
}