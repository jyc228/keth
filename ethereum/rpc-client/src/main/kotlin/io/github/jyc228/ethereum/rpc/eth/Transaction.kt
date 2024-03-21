package io.github.jyc228.ethereum.rpc.eth

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.HexBigInt
import io.github.jyc228.ethereum.HexInt
import io.github.jyc228.ethereum.HexString
import io.github.jyc228.ethereum.HexULong
import io.github.jyc228.ethereum.NullSerializer
import io.github.jyc228.ethereum.TransactionSerializer
import io.github.jyc228.ethereum.TransactionTypeSerializer
import io.github.jyc228.ethereum.wei
import java.math.BigInteger
import kotlinx.serialization.Serializable

@Serializable(TransactionSerializer::class)
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
    val blobVersionedHashes: List<Hash>?
    val chainId: HexULong?
    val yParity: HexULong?

    companion object {
        val pendingBlockHash = Hash("0xpending")
        val pendingBlockNumber = HexULong(0uL)
    }
}

@Serializable
data class Access(val address: Address, val storageKey: List<Hash> = emptyList())

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
    override val blobVersionedHashes: List<Hash>
}

interface DepositTransaction : Transaction

@Serializable
abstract class AbstractMutableTransaction : Transaction {
    @Serializable(NullBlockHash::class)
    override var blockHash: Hash = Transaction.pendingBlockHash

    @Serializable(NullBlockNumber::class)
    override var blockNumber: HexULong = Transaction.pendingBlockNumber

    override var hash: Hash = Hash.empty
    override var from: Address = Address.empty
    override var to: Address? = null
    override var input: String = ""
    override var value: HexBigInt = HexBigInt.ZERO
    override var nonce: HexULong = HexULong.ZERO
    override var gas: HexBigInt = HexBigInt.ZERO
    override var gasPrice: HexBigInt? = null

    @Serializable(NullTxIndex::class)
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
    override val blobVersionedHashes: List<Hash>? get() = null

    private class NullBlockHash : NullSerializer<Hash>(Hash.serializer(), Transaction.pendingBlockHash)
    private class NullBlockNumber : NullSerializer<HexULong>(HexULong.serializer(), Transaction.pendingBlockNumber)
    private class NullTxIndex : NullSerializer<HexInt>(HexInt.serializer(), HexInt(-1))

    override fun toString(): String {
        if (to == null) {
            return "new contract: from=${from} value=${value.number.wei} gas=${gas.number.wei} gasPrice=${gasPrice?.number?.wei}"
        }
        return when {
            input.length > 2 -> "call contract"
            value.number > BigInteger.ZERO -> "send eth"
            else -> type.toString()
        }.let { "$it: from=${from} to=${to} value=${value.number.wei} gas=${gas.number.wei} gasPrice=${gasPrice?.number?.wei}" }
    }
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
    override var blobVersionedHashes: List<Hash> = emptyList()
}

@Serializable
class MutableDepositTransaction : AbstractMutableTransaction(), DepositTransaction {
    var sourceHash: Hash = Hash.empty
    var mint: HexBigInt = HexBigInt.ZERO
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
