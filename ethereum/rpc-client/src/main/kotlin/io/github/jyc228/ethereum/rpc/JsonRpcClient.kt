package io.github.jyc228.ethereum.rpc

import io.github.jyc228.jsonrpc.JsonRpcRequest
import io.github.jyc228.jsonrpc.KtorJsonRpcClient
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement

sealed class JsonRpcClient {
    abstract suspend fun <T> send(
        method: String,
        params: JsonElement,
        decode: (JsonElement) -> T
    ): ApiResult<T>

    abstract fun toImmediateClient(): JsonRpcClient
}

class ImmediateJsonRpcClient(private val client: KtorJsonRpcClient) : JsonRpcClient() {
    override suspend fun <T> send(
        method: String,
        params: JsonElement,
        decode: (JsonElement) -> T
    ): ApiResult<T> {
        val request = JsonRpcRequest(params, method, method)
        return ApiResult(client.send(request), decode)
    }

    override fun toImmediateClient(): JsonRpcClient = this
}

sealed class DeferredJsonRpcClient(protected val client: KtorJsonRpcClient) : JsonRpcClient() {
    private val idGenerator = AtomicLong()

    override suspend fun <T> send(
        method: String,
        params: JsonElement,
        decode: (JsonElement) -> T
    ): DeferredApiResult<T> {
        val request = JsonRpcRequest(params, method, "$method::${idGenerator.getAndIncrement()}")
        return DeferredApiResult(request, Channel(1)) { ApiResult(it, decode) }
    }

    protected suspend fun executeAndSendResult(calls: List<DeferredApiResult<*>>) {
        val response = client.sendBatch(calls.map { it.request })
        calls.onEachIndexed { index, call ->
            call.onResponse.send(when (call.request.id == response[index].id) {
                true -> response[index]
                false -> response.first { call.request.id == it.id }
            })
        }
    }

    override fun toImmediateClient(): JsonRpcClient = ImmediateJsonRpcClient(client)
}

class BatchJsonRpcClient(client: KtorJsonRpcClient) : DeferredJsonRpcClient(client) {
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> execute(calls: List<ApiResult<T>>): List<ApiResult<T>> {
        executeAndSendResult(calls as List<DeferredApiResult<T>>)
        return calls
    }
}

class ScheduledJsonRpcClient(
    client: KtorJsonRpcClient,
    private val interval: Duration,
    private val maxBatchSize: Int = 999
) : DeferredJsonRpcClient(client) {
    private val calls = mutableListOf<DeferredApiResult<*>>()
    private val mutex = Mutex()
    private val job = CoroutineScope(Dispatchers.IO).launch {
        while (isActive) {
            delay(interval)
            val calls = collectCalls().takeIf { it.isNotEmpty() } ?: continue
            launch { executeAndSendResult(calls) }
        }
    }

    override suspend fun <T> send(
        method: String,
        params: JsonElement,
        decode: (JsonElement) -> T
    ): DeferredApiResult<T> {
        return super.send(method, params, decode).also { mutex.withLock { calls += it } }
    }

    private suspend fun collectCalls() = mutex.withLock {
        val size = min(calls.size, maxBatchSize)
        buildList(size) { repeat(size) { add(calls.removeFirst()) } }
    }
}
