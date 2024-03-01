package io.github.jyc228.ethereum.rpc.txpool

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.HexInt
import io.github.jyc228.ethereum.rpc.eth.Transaction
import kotlinx.serialization.Serializable

@Serializable
data class TxpoolContent(
    val pending: Map<Address, Map<Int, Transaction>>,
    val queued: Map<Address, Map<Int, Transaction>>
) {
    fun keys() = pending.keys + queued.keys
}

@Serializable
data class TxpoolContentFrom(
    val pending: Map<Int, Transaction>,
    val queued: Map<Int, Transaction>
) {
    fun keys() = pending.keys + queued.keys
}

@Serializable
data class TxpoolInspect(
    val pending: Map<Address, Map<Int, String>>,
    val queued: Map<Address, Map<Int, String>>
)

@Serializable
data class TxpoolStatus(
    val pending: HexInt,
    val queued: HexInt
)
