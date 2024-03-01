package ethereum.type.builder

import ethereum.collections.Hash
import ethereum.evm.Address
import ethereum.type.BlockHeader
import java.math.BigInteger

class BlockHeaderBuilder(val parent: BlockHeader) {
    val number = parent.number + 1u
    var root: Hash = Hash.EMPTY
    var difficulty: BigInteger = BigInteger.ZERO
    var baseFee: BigInteger? = null
    var gasLimit: BigInteger = parent.gasLimit
    var coinbase: Address = parent.coinbase
    var txHash: Hash = Hash.EMPTY_TX_HASH
    var receiptHash: Hash = Hash.EMPTY_RECEIPT_HASH
    var uncleHash: Hash = Hash.EMPTY_UNCLE_HASH

    fun mutate(callback: BlockHeaderBuilder.() -> Unit): BlockHeaderBuilder = apply { callback(this) }

    fun build(mutate: (BlockHeaderBuilder.() -> Unit)? = null): BlockHeader {
        mutate?.invoke(this)
        return BlockHeader(
            parentHash = parent.hash,
            uncleHash = uncleHash,
            coinbase = coinbase,
            root = root,
            txHash = txHash,
            receiptHash = receiptHash,
            bloom = ByteArray(256),
            difficulty = difficulty,
            number = number,
            gasLimit = gasLimit,
            gasUsed = BigInteger.ZERO,
            time = parent.time + 10u,
            extra = null,
            mixDigest = Hash.EMPTY,
            nonce = ByteArray(8),
            baseFee = baseFee,
            withdrawalsHash = null,
            excessDataGas = null
        )
    }
}
