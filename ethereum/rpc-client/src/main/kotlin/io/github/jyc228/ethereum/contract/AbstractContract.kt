package io.github.jyc228.ethereum.contract

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.HexInt
import io.github.jyc228.ethereum.rpc.ApiResult
import io.github.jyc228.ethereum.rpc.eth.CallRequest
import io.github.jyc228.ethereum.rpc.eth.EthApi
import io.github.jyc228.ethereum.rpc.eth.GetLogsRequest
import io.github.jyc228.ethereum.rpc.eth.Log
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses

abstract class AbstractContract<EVENT : ContractEvent>(
    private val address: Address,
    private val api: EthApi
) : Contract<EVENT> {

    override suspend fun getLogs(options: (GetLogsRequest.() -> Unit)?): ApiResult<List<Pair<EVENT, Log>>> {
        val contractInterface = this::class.superclasses.first { it.isSubclassOf(Contract::class) }
        val eventFactoryByHash = contractInterface
            .nestedClasses
            .mapNotNull { it.companionObjectInstance as? ContractEventFactory<EVENT, *> }
            .associateBy { it.hash }

        val request = GetLogsRequest(address = address.hex).apply { options?.invoke(this) }
        return api.getLogs(request).map { logs ->
            logs.mapNotNull { log ->
                eventFactoryByHash[log.topics[0].hex]?.decodeIf(log.data, log.topics)?.let { e -> e to log }
            }
        }
    }

    override suspend fun <INDEXED : Any, FACTORY : ContractEventFactory<out EVENT, INDEXED>> getLogs(
        factory: FACTORY,
        filterParameter: (GetLogsRequest.(INDEXED) -> Unit)?
    ): ApiResult<List<Pair<EVENT, Log>>> {
        val request = GetLogsRequest(address = address.hex)
        if (filterParameter != null) {
            request.topics = factory.buildTopics { filterParameter(request, this) }
        }
        return api.getLogs(request).map { logs -> logs.map { log -> factory.decode(log.data, log.topics) to log } }
    }

    suspend operator fun <R> ContractFunctionP0<R>.invoke(
        callOption: (Contract.CallOption.() -> Unit)?
    ): ApiResult<R> = call(callOption, encodeFunctionCall())

    suspend operator fun <P1, R> ContractFunctionP1<P1, R>.invoke(
        p1: P1,
        callOption: (Contract.CallOption.() -> Unit)?
    ): ApiResult<R> = call(callOption, encodeFunctionCall(p1))

    suspend operator fun <P1, P2, R> ContractFunctionP2<P1, P2, R>.invoke(
        p1: P1,
        p2: P2,
        callOption: (Contract.CallOption.() -> Unit)?
    ): ApiResult<R> = call(callOption, encodeFunctionCall(p1, p2))

    suspend operator fun <P1, P2, P3, R> ContractFunctionP3<P1, P2, P3, R>.invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        callOption: (Contract.CallOption.() -> Unit)?
    ): ApiResult<R> = call(callOption, encodeFunctionCall(p1, p2, p3))

    suspend operator fun <P1, P2, P3, P4, R> ContractFunctionP4<P1, P2, P3, P4, R>.invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        callOption: (Contract.CallOption.() -> Unit)?
    ): ApiResult<R> = call(callOption, encodeFunctionCall(p1, p2, p3, p4))

    suspend operator fun <P1, P2, P3, P4, P5, R> ContractFunctionP5<P1, P2, P3, P4, P5, R>.invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        callOption: (Contract.CallOption.() -> Unit)?
    ): ApiResult<R> = call(callOption, encodeFunctionCall(p1, p2, p3, p4, p5))

    suspend operator fun <P1, P2, P3, P4, P5, P6, R> ContractFunctionP6<P1, P2, P3, P4, P5, P6, R>.invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        callOption: (Contract.CallOption.() -> Unit)?
    ): ApiResult<R> = call(callOption, encodeFunctionCall(p1, p2, p3, p4, p5, p6))

    suspend operator fun <P1, P2, P3, P4, P5, P6, P7, R> ContractFunctionP7<P1, P2, P3, P4, P5, P6, P7, R>.invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        callOption: (Contract.CallOption.() -> Unit)?
    ): ApiResult<R> = call(callOption, encodeFunctionCall(p1, p2, p3, p4, p5, p6, p7))

    suspend operator fun <P1, P2, P3, P4, P5, P6, P7, P8, R> ContractFunctionP8<P1, P2, P3, P4, P5, P6, P7, P8, R>.invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        callOption: (Contract.CallOption.() -> Unit)?
    ): ApiResult<R> = call(callOption, encodeFunctionCall(p1, p2, p3, p4, p5, p6, p7, p8))

    suspend operator fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R> ContractFunctionP9<P1, P2, P3, P4, P5, P6, P7, P8, P9, R>.invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        callOption: (Contract.CallOption.() -> Unit)?
    ): ApiResult<R> = call(callOption, encodeFunctionCall(p1, p2, p3, p4, p5, p6, p7, p8, p9))

    private suspend fun <R> AbstractContractFunction<R>.call(
        callOption: (Contract.CallOption.() -> Unit)?,
        data: String
    ): ApiResult<R> {
        val option = Contract.CallOption().also { callOption?.invoke(it) }
        val result = api.call(
            CallRequest(
                from = option.from,
                to = address.hex,
                gas = option.gas?.let(::HexInt),
                gasPrice = option.gasPrice?.let(::HexInt),
                value = option.value?.let(::HexInt),
                data = data,
            ),
            option.targetBlock
        )
        return result.map { decodeResult(it) }
    }
}
