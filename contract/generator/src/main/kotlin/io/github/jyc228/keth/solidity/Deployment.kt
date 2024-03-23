package io.github.jyc228.keth.solidity

import io.github.jyc228.solidity.AbiItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
class Deployment(
    val address: String,
    val abi: List<AbiItem>,
    val args: List<String>,
    val bytecode: String,
    val deployedBytecode: String,
    val devdoc: Doc,
    val metadata: String,
    val numDeployments: Int,
    val receipt: Map<String, JsonElement>,
    val solcInputHash: String,
    val storageLayout: StorageLayout? = null,
    val transactionHash: String,
    val userdoc: Doc
) {
    val metadataObject: Metadata = Json { ignoreUnknownKeys = true }.decodeFromString(metadata)

    @Serializable
    data class Metadata(
        val compiler: Compiler,
        val language: String,
        val output: Output,
        val settings: Settings,
        val sources: Map<String, JsonObject>,
        val version: Int
    )

    @Serializable
    data class Compiler(
        val version: String
    )

    @Serializable
    data class Output(
        val abi: List<AbiItem>,
        val devdoc: Doc,
        val userdoc: Doc
    )

    @Serializable
    data class Settings(
        val compilationTarget: Map<String, String>,
        val evmVersion: String,
        val libraries: JsonObject,
        val metadata: JsonObject,
        val optimizer: JsonObject,
        val remappings: List<String>
    )

    @Serializable
    data class StorageLayout(
        val storage: List<Storage>,
        val types: Map<String, JsonObject>
    )

    @Serializable
    data class Storage(
        val astId: Int,
        val contract: String,
        val label: String,
        val offset: Int,
        val slot: String,
        val type: String
    )

    @Serializable
    data class Doc(
        val kind: String = "",
        val methods: Map<String, JsonObject> = emptyMap(),
        val version: Int = 0,
        val notice: String = "",
    )
}
