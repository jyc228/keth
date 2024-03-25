package io.github.jyc228.ethereum.contract

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.abi.Abi
import io.github.jyc228.ethereum.rpc.ApiResult
import io.github.jyc228.ethereum.rpc.eth.EthApi
import io.github.jyc228.ethereum.rpc.eth.GetLogsRequest
import io.github.jyc228.ethereum.rpc.eth.Log
import io.github.jyc228.solidity.AbiItem
import kotlinx.serialization.json.Json
import org.intellij.lang.annotations.Language

interface Contract<EVENT : ContractEvent> {
    suspend fun getLogs(options: (GetLogsRequest.() -> Unit)? = null): ApiResult<List<Pair<EVENT, Log>>>

    suspend fun <INDEXED : Any, FACTORY : ContractEventFactory<out EVENT, INDEXED>> getLogs(
        factory: FACTORY,
        filterParameter: (GetLogsRequest.(indexedParam: INDEXED) -> Unit)? = null
    ): ApiResult<List<Pair<EVENT, Log>>>

    abstract class Factory<T : Contract<*>>(val create: (Address, EthApi) -> T) {
        protected fun encodeParameters(@Language("json") jsonAbi: String, vararg args: Any?): String {
            val abi: AbiItem = Json.decodeFromString(jsonAbi)
            return Abi.encodeParameters(abi.inputs.map { it.type }, args.toList()).removePrefix("0x")
        }
    }
}

interface ContractEvent
