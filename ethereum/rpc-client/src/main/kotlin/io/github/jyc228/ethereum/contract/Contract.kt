package io.github.jyc228.ethereum.contract

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.rpc.RpcCall
import io.github.jyc228.ethereum.rpc.eth.BlockReference
import io.github.jyc228.ethereum.rpc.eth.EthApi
import io.github.jyc228.ethereum.rpc.eth.GetLogsRequest
import io.github.jyc228.ethereum.rpc.eth.Log

interface Contract<EVENT : ContractEvent> {
    suspend fun getLogs(options: (GetLogsRequest.() -> Unit)? = null): RpcCall<List<Pair<EVENT, Log>>>

    suspend fun <INDEXED : Any, FACTORY : ContractEventFactory<out EVENT, INDEXED>> getLogs(
        factory: FACTORY,
        filterParameter: (GetLogsRequest.(indexedParam: INDEXED) -> Unit)? = null
    ): RpcCall<List<Pair<EVENT, Log>>>

    abstract class Factory<T : Contract<*>>(val create: (Address, EthApi) -> T)

    class CallOption(
        var from: String? = null,
        var gas: Int? = null,
        var gasPrice: Int? = null,
        var value: Int? = null,
        var targetBlock: BlockReference = BlockReference.latest
    )
}

interface ContractEvent