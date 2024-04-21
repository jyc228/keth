package io.github.jyc228.ethereum

import io.kotest.matchers.resource.resourceAsString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.plus

val defaultJson = Json {
    ignoreUnknownKeys = true
    classDiscriminator = ""
    serializersModule += createEthSerializersModule(null)
}

inline fun <reified T> decodeJsonResource(path: String, json: Json = defaultJson): T {
    return json.decodeFromString<T>(resourceAsString(path))
}