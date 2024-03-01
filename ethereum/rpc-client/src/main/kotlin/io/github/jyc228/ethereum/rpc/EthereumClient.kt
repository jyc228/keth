package io.github.jyc228.ethereum.rpc

import io.github.jyc228.ethereum.rpc.contract.ContractApi
import io.github.jyc228.ethereum.rpc.engin.EngineApi
import io.github.jyc228.ethereum.rpc.eth.EthApi
import io.github.jyc228.ethereum.rpc.txpool.TxpoolApi

interface EthereumClient {
    val eth: EthApi
    val engin: EngineApi
    val txpool: TxpoolApi
    val contract: ContractApi
    suspend fun <R> batch(init: suspend EthereumClient.() -> List<RpcCall<R>>): List<RpcCall<R>>

    companion object
}

suspend fun <R1, R2> EthereumClient.batch2(
    e1: BatchElement<R1>,
    e2: BatchElement<R2>,
): Pair<RpcCall<R1>, RpcCall<R2>> = batch2(e1, e2) { r1, r2 -> r1 to r2 }

@Suppress("UNCHECKED_CAST")
suspend fun <R1, R2, RESULT> EthereumClient.batch2(
    e1: BatchElement<R1>,
    e2: BatchElement<R2>,
    transform: suspend (RpcCall<R1>, RpcCall<R2>) -> RESULT
): RESULT = batch(e1, e2).let { transform(it[0] as RpcCall<R1>, it[1] as RpcCall<R2>) }

suspend fun <R1, R2, R3> EthereumClient.batch3(
    e1: BatchElement<R1>,
    e2: BatchElement<R2>,
    e3: BatchElement<R3>
): Triple<RpcCall<R1>, RpcCall<R2>, RpcCall<R3>> = batch3(e1, e2, e3) { v1, v2, v3 -> Triple(v1, v2, v3) }

@Suppress("UNCHECKED_CAST")
suspend fun <R1, R2, R3, RESULT> EthereumClient.batch3(
    e1: BatchElement<R1>,
    e2: BatchElement<R2>,
    e3: BatchElement<R3>,
    transform: suspend (RpcCall<R1>, RpcCall<R2>, RpcCall<R3>) -> RESULT
): RESULT = batch(e1, e2, e3).let { transform(it[0] as RpcCall<R1>, it[1] as RpcCall<R2>, it[2] as RpcCall<R3>) }

private suspend fun EthereumClient.batch(vararg e: BatchElement<Any?>): List<RpcCall<out Any?>> {
    return batch { e.map { it(this) } }
}

private typealias BatchElement<T> = suspend EthereumClient.() -> RpcCall<out T>
