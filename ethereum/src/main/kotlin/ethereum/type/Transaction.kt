package ethereum.type

import ethereum.collections.Hash
import ethereum.crypto.ECDSASignature
import ethereum.evm.Address
import ethereum.rlp.RLPEncoder
import ethereum.rlp.toRlp
import java.math.BigInteger
import java.time.LocalDateTime

class Transaction(
    val inner: TransactionDetail
) {
    val time: LocalDateTime? = null
    val hash: Hash? = null
    val size: UInt? = null
    val from: Address? = null

    fun toRlp(): ByteArray {
        if (inner.txType == LegacyTransaction.TYPE) {
            return inner.toRlp()
        }
        return RLPEncoder.encodeArray {
            addByte(inner.txType)
            addBytes(inner.toRlp())
        }
    }
}

sealed interface TransactionDetail : ECDSASignature {
    val txType: Byte
    val chainID: ULong
    val accessList: List<Access>
    val data: ByteArray?
    val gas: ULong
    val gasPrice: BigInteger
    val gasTipCap: BigInteger
    val gasFeeCap: BigInteger
    val value: BigInteger
    val nonce: ULong
    val to: Address
    val blobGas: ULong
    val blobGasFeeCap: BigInteger?
    val blobHashes: List<Hash>?

    fun effectiveGasPrice(dst: BigInteger, baseFee: BigInteger): BigInteger
    fun copy(): TransactionDetail // creates a deep copy and initializes all fields
}

data class Access(val address: Address, val storageKey: List<Hash>)

/** [BlobTransaction] represents an EIP-4844 transaction. */
class BlobTransaction(
    override val chainID: ULong,
    override val nonce: ULong,
    override val gasTipCap: BigInteger, // a.k.a. maxPriorityFeePerGas
    override val gasFeeCap: BigInteger, // a.k.a. maxFeePerGas
    override val gas: ULong,
    override val to: Address,
    override val value: BigInteger,
    override val data: ByteArray?,
    override val accessList: List<Access>,
    override val blobGasFeeCap: BigInteger? = null,
    override val blobHashes: List<Hash>? = null,
    override val r: BigInteger,
    override val s: BigInteger,
    override val v: BigInteger,
) : TransactionDetail {
    override val txType: Byte = TYPE
    override val blobGas: ULong = 131072u + (blobHashes?.size?.toULong() ?: 0u)
    override val gasPrice: BigInteger = gasFeeCap
    override fun effectiveGasPrice(dst: BigInteger, baseFee: BigInteger): BigInteger {
        TODO("Not yet implemented")
    }

    override fun copy(): TransactionDetail {
        TODO("Not yet implemented")
    }

    companion object {
        const val TYPE: Byte = 0x03
    }
}

/** [DynamicFeeTransaction] represents an EIP-1559 transaction. */
class DynamicFeeTransaction(
    override val chainID: ULong,
    override val nonce: ULong,
    override val gasTipCap: BigInteger, // a.k.a. maxPriorityFeePerGas
    override val gasFeeCap: BigInteger, // a.k.a. maxFeePerGas
    override val gas: ULong,
    override val to: Address,
    override val value: BigInteger,
    override val data: ByteArray?,
    override val accessList: List<Access>,
    override val r: BigInteger,
    override val s: BigInteger,
    override val v: BigInteger,
) : TransactionDetail {
    override val txType: Byte = TYPE
    override val blobGas: ULong = 0u
    override val blobGasFeeCap: BigInteger? = null
    override val blobHashes: List<Hash>? = null
    override val gasPrice: BigInteger = gasFeeCap
    override fun effectiveGasPrice(dst: BigInteger, baseFee: BigInteger): BigInteger {
        TODO("Not yet implemented")
    }

    override fun copy(): TransactionDetail {
        TODO("Not yet implemented")
    }

    companion object {
        const val TYPE: Byte = 0x02
    }
}

/** [AccessListTransaction] is an EIP-2930 access list. */
class AccessListTransaction(
    override val chainID: ULong,
    override val nonce: ULong,
    override val gasPrice: BigInteger,
    override val gas: ULong,
    override val to: Address,
    override val value: BigInteger,
    override val data: ByteArray?,
    override val accessList: List<Access>,
    override val v: BigInteger,
    override val r: BigInteger,
    override val s: BigInteger,
) : TransactionDetail {
    override val txType: Byte = TYPE
    override val gasTipCap: BigInteger = gasPrice
    override val gasFeeCap: BigInteger = gasPrice
    override val blobGas: ULong = 0u
    override val blobGasFeeCap: BigInteger? = null
    override val blobHashes: List<Hash>? = null

    override fun effectiveGasPrice(dst: BigInteger, baseFee: BigInteger): BigInteger {
        TODO("Not yet implemented")
    }

    override fun copy(): TransactionDetail {
        TODO("Not yet implemented")
    }

    companion object {
        const val TYPE: Byte = 0x01
    }
}

/** [LegacyTransaction] is the transaction data of the original Ethereum transactions. */
class LegacyTransaction(
    /** nonce of sender account */
    override val nonce: ULong,
    override val gasPrice: BigInteger,
    override val gas: ULong,
    override val to: Address,
    override val value: BigInteger,
    override val data: ByteArray?,
    override val v: BigInteger,
    override val r: BigInteger,
    override val s: BigInteger,
) : TransactionDetail {
    override val txType: Byte = TYPE
    override val chainID: ULong
        get() = TODO("Not yet implemented")
    override val accessList: List<Access> = emptyList()
    override val gasTipCap: BigInteger = gasPrice
    override val gasFeeCap: BigInteger = gasPrice
    override val blobGas: ULong = 0u
    override val blobGasFeeCap: BigInteger? = null
    override val blobHashes: List<Hash>? = null

    override fun effectiveGasPrice(dst: BigInteger, baseFee: BigInteger): BigInteger {
        value.bitCount()
        TODO("Not yet implemented")
    }

    override fun copy(): TransactionDetail {
        TODO("Not yet implemented")
    }

    companion object {
        const val TYPE: Byte = 0x00
    }
}

//class TransactionV2(
//    val txType: TransactionType,
//    val chainID: BigInteger,
//    val accessList: List<Access>,
//    val data: ByteArray?,
//    val gas: ULong,
//    val gasPrice: BigInteger,
//    val gasTipCap: BigInteger,
//    val gasFeeCap: BigInteger,
//    val value: BigInteger,
//    val nonce: ULong,
//    val to: Address,
//    val blobGas: ULong,
//    val blobGasFeeCap: BigInteger?,
//    val blobHashes: List<Hash>?
//)
//

//
//sealed class TransactionType(val value: Byte) {
//    object Legacy : TransactionType(0x0)
//    object AccessList : TransactionType(0x1)
//    object DynamicFee : TransactionType(0x2)
//    object Blob : TransactionType(0x3)
//    class Custom(value: Byte) : TransactionType(value)
//}