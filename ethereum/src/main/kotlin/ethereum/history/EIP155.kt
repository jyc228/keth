package ethereum.history

import com.github.jyc228.keth.type.ECDSASignature
import com.github.jyc228.keth.type.Hash
import com.github.jyc228.keth.type.HexBigInt
import com.github.jyc228.keth.type.Transaction
import com.github.jyc228.keth.type.TransactionRlp
import com.github.jyc228.keth.type.TransactionType
import ethereum.history.fork.FrontierHardFork
import ethereum.type.keccak256
import java.math.BigInteger

/**
 * https://eips.ethereum.org/EIPS/eip-155
 */
object EIP155 {

    open class Signer(val chainId: ULong) : ethereum.type.Signer {
        val vMultiplier = chainId.toString().toBigInteger() * BigInteger.TWO
        override fun signatureValues(txType: TransactionType, sig: ByteArray): ECDSASignature {
            require(txType == TransactionType.Legacy) { "ErrTxTypeNotSupported" }
            val ecdsa = FrontierHardFork.signatureValues(txType, sig)
            if (chainId > 0u) {
                val v = BigInteger(1, byteArrayOf((sig[64] + 35).toByte())) * vMultiplier
                return ECDSASignature.Mutable(ecdsa.r, ecdsa.s, HexBigInt(v))
            }
            return ecdsa
        }

        override fun hash(tx: Transaction): Hash {
            return Hash.keccak256(TransactionRlp.encode(tx, withSignature = false))
        }
    }
}