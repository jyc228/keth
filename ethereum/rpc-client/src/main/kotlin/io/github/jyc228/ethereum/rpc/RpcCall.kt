package io.github.jyc228.ethereum.rpc

import io.github.jyc228.jsonrpc.JsonRpcError
import io.github.jyc228.jsonrpc.JsonRpcException
import io.github.jyc228.jsonrpc.JsonRpcRequest
import io.github.jyc228.jsonrpc.JsonRpcResponse
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

suspend fun <T> List<RpcCall<out T>>.awaitAllOrThrow(): List<T> = map { it.awaitOrThrow() }

fun <T> RpcCall(
    response: JsonRpcResponse,
    decode: (JsonElement) -> T
): RpcCall<T> {
    if (response.error != null) {
        return RpcCallFail(response.id, response.error)
    }
    if (response.result is JsonNull) return RpcCallSuccess(null as T)
    return RpcCallSuccess(decode(response.result))
}

interface RpcCall<T> {
    suspend fun awaitOrNull(): T?
    suspend fun awaitOrThrow(): T

    fun <R> map(transform: (T) -> R): RpcCall<R>
    fun onFailure(handleError: (Throwable) -> Unit): RpcCall<T>

    companion object
}

internal data class RpcCallSuccess<T>(val data: T) : RpcCall<T> {
    override suspend fun awaitOrNull(): T? = data
    override suspend fun awaitOrThrow(): T = data
    override fun <R> map(transform: (T) -> R): RpcCall<R> = RpcCallSuccess(transform(data))
    override fun onFailure(handleError: (Throwable) -> Unit): RpcCall<T> = this
    override fun toString(): String = data.toString()
}

@Suppress("UNCHECKED_CAST")
internal class RpcCallFail<T>(
    private val id: String,
    val error: JsonRpcError
) : RpcCall<T> {
    private fun exception() = JsonRpcException(id, error)
    override suspend fun awaitOrNull(): T? = null
    override suspend fun awaitOrThrow(): T = throw exception()
    override fun <R> map(transform: (T) -> R): RpcCall<R> = this as RpcCall<R>
    override fun onFailure(handleError: (Throwable) -> Unit): RpcCall<T> = apply { handleError(exception()) }
    override fun toString(): String = error.toString()
}

class DeferredRpcCall<T>(
    val request: JsonRpcRequest,
    val onResponse: Channel<JsonRpcResponse>,
    val decode: suspend (JsonRpcResponse) -> RpcCall<T>
) : RpcCall<T> {
    override suspend fun awaitOrThrow() = decode(onResponse.receive()).awaitOrThrow()
    override suspend fun awaitOrNull() = decode(onResponse.receive()).awaitOrNull()
    override fun <R> map(transform: (T) -> R): RpcCall<R> =
        DeferredRpcCall(request, onResponse) { decode(it).map(transform) }

    override fun onFailure(handleError: (Throwable) -> Unit): RpcCall<T> =
        DeferredRpcCall(request, onResponse) { decode(it).onFailure(handleError) }
}
