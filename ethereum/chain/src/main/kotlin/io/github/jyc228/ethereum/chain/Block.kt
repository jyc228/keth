package io.github.jyc228.ethereum.chain

import ethereum.rlp.RLPEncoder
import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.Transaction
import io.github.jyc228.ethereum.Withdrawal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime

class Block(
    val header: BlockHeader,
    val body: BlockBody,

    val receivedAt: LocalDateTime? = null,
    val receivedFrom: Any? = null
) {
    val number = header.number.toString().toULong()
    val hash by lazy(LazyThreadSafetyMode.NONE) { header.hash }
    val size by lazy(LazyThreadSafetyMode.NONE) { 0 }

    override fun toString(): String {
        return "$number : $hash"
    }

    companion object
}

data class BlockHeader(
    val parentHash: Hash,
    val uncleHash: Hash,
    val coinbase: Address,
    val root: Hash,
    val txHash: Hash,
    val receiptHash: Hash,
    // BloomByteLength : 256
    val bloom: ByteArray,
    val difficulty: BigInteger?,
    val number: ULong,
    val gasLimit: BigInteger,
    val gasUsed: BigInteger,
    val time: ULong,
    val extra: ByteArray?,
    val mixDigest: Hash,
    // size : 8
    val nonce: ULong,

    /** [baseFee] was added by EIP-1559 and is ignored in legacy headers. */
    val baseFee: BigInteger? = null,

    /** [withdrawalsHash] was added by EIP-4895 and is ignored in legacy headers. */
    val withdrawalsHash: Hash? = null,

    /** [excessDataGas] was added by EIP-4844 and is ignored in legacy headers. */
    val excessDataGas: BigInteger? = null,

    /** [dataGasUsed] was added by EIP-4844 and is ignored in legacy headers. */
    val dataGasUsed: BigInteger? = null,
) {
    val hash by lazy(LazyThreadSafetyMode.NONE) { Hash.keccak256(BlockHeaderRlp.encodeToRlp(this)) }

    override fun toString(): String {
        return "$number : $hash : $parentHash"
    }

    companion object
}

class BlockBody(
    val uncles: List<BlockHeader>,
    val transactions: List<Transaction>,
    val withdrawals: List<Withdrawal>,
)

object BlockHeaderRlp {
    fun encodeToRlp(header: BlockHeader) = RLPEncoder.encodeArray {
        addBytes(header.parentHash.bytes)
        addBytes(header.uncleHash.bytes)
        addBytes(header.coinbase.bytes)
        addBytes(header.root.bytes)
        addBytes(header.txHash.bytes)
        addBytes(header.receiptHash.bytes)
        addBytes(header.bloom)
        if (header.difficulty == null) addEmptyString() else addBigInt(header.difficulty)
        addULong(header.number)
        addBigInt(header.gasLimit)
        addBigInt(header.gasUsed)
        addULong(header.time)
        addBytes(header.extra)
        addBytes(header.mixDigest.bytes)
        addBytes(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(header.nonce.toLong()).array())
    }
}