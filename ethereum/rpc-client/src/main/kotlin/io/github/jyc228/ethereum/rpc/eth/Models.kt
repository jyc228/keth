package io.github.jyc228.ethereum.rpc.eth

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.HexBigInt
import io.github.jyc228.ethereum.HexInt
import kotlinx.serialization.Serializable

@Serializable
data class GetLogsRequest(
    var fromBlock: BlockReference? = null,
    var toBlock: BlockReference? = null,
    var address: String? = null,
    var topics: List<String?>? = null
)

@Serializable
data class CallRequest(
    var from: String? = null,
    var to: String? = null,
    var gas: HexInt? = null,
    var gasPrice: HexInt? = null,
    var value: HexInt? = null,
    var data: String? = null
)

@Serializable
data class Withdrawal(
    val address: Address,
    val amount: HexBigInt,
    val index: HexInt,
    val validatorIndex: HexInt
)

@JvmInline
@Serializable
value class BlockReference private constructor(val value: String) {
    constructor(number: ULong) : this("0x${number.toString(16)}")
    constructor(number: Int) : this("0x${number.toString(16)}")
    constructor(hash: Hash) : this(hash.hex)

    companion object {
        val latest = BlockReference("latest")
        val safe = BlockReference("safe")
        val finalized = BlockReference("finalized")

        fun fromTag(tag: String) = when (tag.lowercase()) {
            latest.value -> latest
            safe.value -> safe
            finalized.value -> finalized
            else -> error("unknown tag $tag")
        }
    }
}