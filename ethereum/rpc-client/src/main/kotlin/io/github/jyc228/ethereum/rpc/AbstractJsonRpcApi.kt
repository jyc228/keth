package io.github.jyc228.ethereum.rpc

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

abstract class AbstractJsonRpcApi(val client: JsonRpcClient) {
    protected suspend inline operator fun <reified T> String.invoke(): RpcCall<T> {
        return client.send(this, JsonNull) { json.decodeFromJsonElement(it) }
    }

    protected suspend inline operator fun <reified T, reified P1> String.invoke(p1: P1): RpcCall<T> {
        val inputs = listOf(Json.encodeToJsonElement(p1))
        return client.send(this, JsonArray(inputs)) { json.decodeFromJsonElement(it) }
    }

    protected suspend inline operator fun <reified T, reified P1, reified P2> String.invoke(
        p1: P1,
        p2: P2
    ): RpcCall<T> {
        val inputs = listOf(Json.encodeToJsonElement(p1), Json.encodeToJsonElement(p2))
        return client.send(this, JsonArray(inputs)) { json.decodeFromJsonElement(it) }
    }

    companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
