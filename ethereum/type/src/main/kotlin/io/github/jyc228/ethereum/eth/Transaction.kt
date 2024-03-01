package io.github.jyc228.ethereum.eth

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.HexBigInt
import io.github.jyc228.ethereum.HexInt
import io.github.jyc228.ethereum.HexString
import io.github.jyc228.ethereum.HexULong
import io.github.jyc228.ethereum.TransactionTypeSerializer
import io.github.jyc228.ethereum.TransactionsSerializer
import kotlinx.serialization.Serializable

@Serializable(TransactionsSerializer::class)
interface Transaction {
    val blockHash: Hash
    val blockNumber: HexULong
    val hash: Hash
    val from: Address
    val to: Address?
    val input: String
    val value: HexBigInt
    val nonce: HexULong
    val gas: HexBigInt
    val gasPrice: HexBigInt?
    val transactionIndex: HexInt
    val type: TransactionType
    val v: String?
    val r: String?
    val s: String?
    val accessList: List<Access>?
    val maxFeePerGas: HexBigInt?
    val maxPriorityFeePerGas: HexBigInt?
    val maxFeePerBlobGas: HexBigInt?
    val blobHashes: List<Hash>?
    val chainId: HexULong?
    val yParity: HexULong?

    companion object {
        val pendingBlockHash = Hash("0xpending")
        val pendingBlockNumber = HexULong(0uL)
    }
}

@Serializable
data class Access(val address: Address, val storageKey: List<Hash>)

interface LegacyTransaction : Transaction

interface AccessListTransaction : Transaction {
    override val accessList: List<Access>
}

interface DynamicFeeTransaction : AccessListTransaction {
    override val maxFeePerGas: HexBigInt
    override val maxPriorityFeePerGas: HexBigInt
}

interface BlobTransaction : DynamicFeeTransaction {
    override val maxFeePerBlobGas: HexBigInt
    override val blobHashes: List<Hash>
}

@Serializable
abstract class AbstractMutableTransaction : Transaction {
    override var blockHash: Hash = Transaction.pendingBlockHash
    override var blockNumber: HexULong = Transaction.pendingBlockNumber
    override var hash: Hash = Hash.empty
    override var from: Address = Address.empty
    override var to: Address? = null
    override var input: String = ""
    override var value: HexBigInt = HexBigInt.ZERO
    override var nonce: HexULong = HexULong.ZERO
    override var gas: HexBigInt = HexBigInt.ZERO
    override var gasPrice: HexBigInt? = null
    override var transactionIndex: HexInt = HexInt.ZERO
    override var type: TransactionType = TransactionType.Legacy
    override var v: String? = null
    override var r: String? = null
    override var s: String? = null
    override var chainId: HexULong? = null
    override var yParity: HexULong? = null

    override val accessList: List<Access>? get() = null
    override val maxFeePerGas: HexBigInt? get() = null
    override val maxPriorityFeePerGas: HexBigInt? get() = null
    override val maxFeePerBlobGas: HexBigInt? get() = null
    override val blobHashes: List<Hash>? get() = null
}

@Serializable
class MutableLegacyTransaction : AbstractMutableTransaction(), LegacyTransaction

@Serializable
class MutableAccessListTransaction : AccessListTransaction, AbstractMutableTransaction() {
    override var accessList: List<Access> = emptyList()
}

@Serializable
class MutableDynamicFeeTransaction : AbstractMutableTransaction(), DynamicFeeTransaction {
    override var accessList: List<Access> = emptyList()
    override var maxFeePerGas: HexBigInt = HexBigInt.ZERO
    override var maxPriorityFeePerGas: HexBigInt = HexBigInt.ZERO
}

@Serializable
class MutableBlobTransaction : AbstractMutableTransaction(), BlobTransaction {
    override var accessList: List<Access> = emptyList()
    override var maxFeePerGas: HexBigInt = HexBigInt.ZERO
    override var maxPriorityFeePerGas: HexBigInt = HexBigInt.ZERO
    override var maxFeePerBlobGas: HexBigInt = HexBigInt.ZERO
    override var blobHashes: List<Hash> = emptyList()
}

@Serializable(TransactionTypeSerializer::class)
sealed class TransactionType(val value: Int, override val hex: String) : HexString {
    data object Legacy : TransactionType(0, "0x0")
    data object AccessList : TransactionType(1, "0x1")
    data object DynamicFee : TransactionType(2, "0x2")
    data object Blob : TransactionType(3, "0x3")
    class Custom(hex: String) : TransactionType(hex.removePrefix("0x").toInt(16), hex)

    companion object {
        fun from(hex: String): TransactionType = Legacy.takeIf { it.hex == hex }
            ?: AccessList.takeIf { it.hex == hex }
            ?: DynamicFee.takeIf { it.hex == hex }
            ?: Blob.takeIf { it.hex == hex }
            ?: Custom(hex)
    }
}
