package io.github.jyc228.ethereum.rpc.eth

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.HexBigInt
import io.github.jyc228.ethereum.HexData
import io.github.jyc228.ethereum.HexULong
import io.github.jyc228.ethereum.InstantSerializer
import io.github.jyc228.ethereum.TransactionHashesSerializer
import io.github.jyc228.ethereum.TransactionsSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

interface BlockHeader {
    val hash: Hash
    val parentHash: Hash
    val sha3Uncles: Hash
    val miner: Address?
    val stateRoot: Hash
    val transactionsRoot: Hash
    val receiptsRoot: Hash
    val logsBloom: String
    val difficulty: HexBigInt
    val number: HexULong
    val gasLimit: HexBigInt
    val gasUsed: HexBigInt
    val timestamp: Instant
    val extraData: String
    val mixHash: Hash
    val nonce: HexULong
    val totalDifficulty: HexBigInt?

    // BaseFee was added by EIP-1559 and is ignored in legacy headers.
    val baseFeePerGas: HexBigInt?

    // WithdrawalsHash was added by EIP-4895 and is ignored in legacy headers.
    val withdrawalsRoot: Hash?
    val parentBeaconBlockRoot: Hash?
    val blobGasUsed: HexBigInt?
    val excessBlobGas: HexBigInt?
}

@Serializable
data class SimpleBlockHeader(
    override val hash: Hash,
    override val parentHash: Hash,
    override val sha3Uncles: Hash,
    override val miner: Address? = null,
    override val stateRoot: Hash,
    override val transactionsRoot: Hash,
    override val receiptsRoot: Hash,
    override val logsBloom: String,
    override val difficulty: HexBigInt,
    override val number: HexULong,
    override val gasLimit: HexBigInt,
    override val gasUsed: HexBigInt,
    @Serializable(InstantSerializer::class)
    override val timestamp: Instant,
    override val extraData: String,
    override val mixHash: Hash,
    override val nonce: HexULong,
    override val totalDifficulty: HexBigInt? = null,
    override val baseFeePerGas: HexBigInt? = null,
    override val withdrawalsRoot: Hash? = null,
    override val parentBeaconBlockRoot: Hash? = null,
    override val blobGasUsed: HexBigInt? = null,
    override val excessBlobGas: HexBigInt? = null,
) : BlockHeader

interface Block : BlockHeader {
    val size: HexData
    val transactions: Transactions
    val uncles: List<String>
    val withdrawals: List<Withdrawal>

    interface Transactions {
        val hashes: List<Hash>
    }
}

@Serializable
data class SimpleBlock(
    override val baseFeePerGas: HexBigInt? = null,
    override val difficulty: HexBigInt,
    override val extraData: String = "",
    override val gasLimit: HexBigInt,
    override val gasUsed: HexBigInt,
    override val hash: Hash,
    override val logsBloom: String = "",
    override val miner: Address? = null,
    override val mixHash: Hash,
    override val nonce: HexULong,
    override val number: HexULong,
    override val parentHash: Hash,
    override val receiptsRoot: Hash,
    override val sha3Uncles: Hash,
    override val size: HexData,
    override val stateRoot: Hash,
    @Serializable(InstantSerializer::class)
    override val timestamp: Instant,
    override val totalDifficulty: HexBigInt? = null,
    override val transactions: TransactionHashes,
    override val transactionsRoot: Hash,
    override val uncles: List<String> = emptyList(),
    override val withdrawals: List<Withdrawal> = emptyList(),
    override val withdrawalsRoot: Hash? = null,
    override val parentBeaconBlockRoot: Hash? = null,
    override val blobGasUsed: HexBigInt? = null,
    override val excessBlobGas: HexBigInt? = null,
) : Block {

    @Serializable(TransactionHashesSerializer::class)
    class TransactionHashes(override val hashes: List<Hash>) : Block.Transactions, List<Hash> by hashes
}


@Serializable
data class FullBlock(
    override val baseFeePerGas: HexBigInt? = null,
    override val difficulty: HexBigInt,
    override val extraData: String = "",
    override val gasLimit: HexBigInt,
    override val gasUsed: HexBigInt,
    override val hash: Hash,
    override val logsBloom: String = "",
    override val miner: Address? = null,
    override val mixHash: Hash,
    override val nonce: HexULong,
    override val number: HexULong,
    override val parentHash: Hash,
    override val receiptsRoot: Hash,
    override val sha3Uncles: Hash,
    override val size: HexData,
    override val stateRoot: Hash,
    @Serializable(InstantSerializer::class)
    override val timestamp: Instant,
    override val totalDifficulty: HexBigInt? = null,
    override val transactions: Transactions,
    override val transactionsRoot: Hash,
    override val uncles: List<String> = emptyList(),
    override val withdrawals: List<Withdrawal> = emptyList(),
    override val withdrawalsRoot: Hash? = null,
    override val parentBeaconBlockRoot: Hash? = null,
    override val blobGasUsed: HexBigInt? = null,
    override val excessBlobGas: HexBigInt? = null,
) : Block {

    override fun toString(): String {
        return "number=${number} hash=${hash} parentHash=${parentHash} timestamp=${timestamp} txCount=${transactions.size}"
    }

    @Serializable(TransactionsSerializer::class)
    class Transactions(self: List<Transaction>) : Block.Transactions, List<Transaction> by self {
        override val hashes: List<Hash> = object : AbstractList<Hash>() {
            override val size: Int get() = this@Transactions.size
            override fun get(index: Int): Hash = this@Transactions[index].hash
        }
    }
}
