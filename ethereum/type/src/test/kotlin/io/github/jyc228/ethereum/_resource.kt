package io.github.jyc228.ethereum

import io.kotest.matchers.resource.resourceAsString
import kotlinx.serialization.json.Json

inline fun <reified T> decodeJsonResource(path: String, json: Json = Json.Default): T {
    return json.decodeFromString<T>(resourceAsString(path))
}