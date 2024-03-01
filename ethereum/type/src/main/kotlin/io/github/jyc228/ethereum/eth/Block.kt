package io.github.jyc228.ethereum.eth

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.BlockSerializer
import io.github.jyc228.ethereum.BlockTransactionsSerializer
import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.HexBigInt
import io.github.jyc228.ethereum.HexData
import io.github.jyc228.ethereum.HexULong
import io.github.jyc228.ethereum.InstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

interface BlockHeader {
    val hash: Hash
    val parentHash: Hash
    val sha3Uncles: Hash
    val coinbase: Address?
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

    // BaseFee was added by EIP-1559 and is ignored in legacy headers.
    val baseFeePerGas: HexBigInt?

    // WithdrawalsHash was added by EIP-4895 and is ignored in legacy headers.
    val withdrawalsRoot: Hash?
}

@Serializable(BlockSerializer::class)
interface Block : BlockHeader {
    val miner: String
    val size: HexData
    val totalDifficulty: HexBigInt?
    val transactions: Transactions
    val uncles: List<String>
    val withdrawals: List<Withdrawal>

    @Serializable(BlockTransactionsSerializer::class)
    interface Transactions {
        val hashes: List<Hash>
    }
}

@Serializable
data class SimpleBlock(
    override val coinbase: Address? = null,
    override val baseFeePerGas: HexBigInt? = null,
    override val difficulty: HexBigInt,
    override val extraData: String = "",
    override val gasLimit: HexBigInt,
    override val gasUsed: HexBigInt,
    override val hash: Hash,
    override val logsBloom: String = "",
    override val miner: String = "",
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
    override val totalDifficulty: HexBigInt?,
    override val transactions: TransactionHashes,
    override val transactionsRoot: Hash,
    override val uncles: List<String> = emptyList(),
    override val withdrawals: List<Withdrawal> = emptyList(),
    override val withdrawalsRoot: Hash? = null
) : Block {

    @Serializable(BlockTransactionsSerializer.TransactionHashesSerializer::class)
    class TransactionHashes(override val hashes: List<Hash>) : Block.Transactions, List<Hash> by hashes
}


@Serializable
data class FullBlock(
    override val coinbase: Address? = null,
    override val baseFeePerGas: HexBigInt? = null,
    override val difficulty: HexBigInt,
    override val extraData: String = "",
    override val gasLimit: HexBigInt,
    override val gasUsed: HexBigInt,
    override val hash: Hash,
    override val logsBloom: String = "",
    override val miner: String = "",
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
    override val totalDifficulty: HexBigInt?,
    override val transactions: Transactions,
    override val transactionsRoot: Hash,
    override val uncles: List<String> = emptyList(),
    override val withdrawals: List<Withdrawal> = emptyList(),
    override val withdrawalsRoot: Hash? = null
) : Block {

    @Serializable(BlockTransactionsSerializer.TransactionsSerializer::class)
    class Transactions(self: List<Transaction>) : Block.Transactions, List<Transaction> by self {
        override val hashes: List<Hash> = object : AbstractList<Hash>() {
            override val size: Int get() = this@Transactions.size
            override fun get(index: Int): Hash = this@Transactions[index].hash
        }
    }
}
