package io.github.jyc228.ethereum.rpc.eth

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.HexBigInt
import io.github.jyc228.ethereum.HexData
import io.github.jyc228.ethereum.HexInt
import io.github.jyc228.ethereum.HexString
import io.github.jyc228.ethereum.HexULong
import io.github.jyc228.ethereum.TransactionStatusSerializer
import kotlinx.serialization.Serializable

@Serializable
data class TransactionReceipt(
    val transactionHash: Hash,
    val transactionIndex: HexInt,
    val blockHash: Hash,
    val blockNumber: HexULong,
    val from: Address,
    /** address of the receiver. null when its a contract creation transaction. */
    val to: Address?,
    val effectiveGasPrice: HexBigInt?,
    val cumulativeGasUsed: HexBigInt,
    val gasUsed: HexBigInt,
    /** The contract address created, if the transaction was a contract creation, otherwise null. */
    val contractAddress: Address?,
    val logs: List<Log> = emptyList(),
    val logsBloom: String = "",
    val status: TransactionStatus,
    val type: TransactionType?
)

@Serializable(TransactionStatusSerializer::class)
sealed class TransactionStatus(val value: Int, override val hex: String) : HexString {
    data object Fail : TransactionStatus(0, "0x0")
    data object Success : TransactionStatus(1, "0x1")
    class Custom(hex: String) : TransactionStatus(hex.removePrefix("0x").toInt(16), hex)

    companion object {
        fun from(hex: String): TransactionStatus = Fail.takeIf { it.hex == hex }
            ?: Success.takeIf { it.hex == hex }
            ?: Custom(hex)
    }
}

@Serializable
data class Log(
    val removed: Boolean,
    val logIndex: HexInt,
    val transactionIndex: HexInt,
    val transactionHash: Hash,
    val blockHash: Hash,
    val blockNumber: HexULong,
    val address: Address,
    val data: HexData,
    val topics: List<HexData>
)
