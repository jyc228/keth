package io.github.jyc228.keth.solidity

import io.github.jyc228.solidity.AbiComponent
import io.github.jyc228.solidity.AbiItem
import io.github.jyc228.solidity.AbiOutput
import io.github.jyc228.solidity.AbiType
import java.io.File
import kotlinx.serialization.json.Json

class Abi(
    val contractName: String,
    private val items: List<AbiItem>
) {
    val overloadingFunctions = mutableMapOf<String, MutableSet<Int>>()
    private val functionIndex = mutableSetOf<Int>()
    private val eventIndex = mutableSetOf<Int>()
    private val topLevelTuples = mutableMapOf<String, AbiComponent>()
    private val internalTuples = mutableSetOf<AbiComponent>()
    private val externalTuples = mutableSetOf<AbiComponent>()

    init {
        val functionNames = mutableSetOf<String>()
        items.forEachIndexed { index, item ->
            item.ioAsSequence().filter { it.type == "tuple" }.forEach {
                val struct = it.resolveStruct()
                when (struct.ownerName.isBlank()) {
                    true -> topLevelTuples[struct.name] = it
                    false -> when (contractName == struct.ownerName) {
                        true -> internalTuples += it
                        false -> externalTuples += it
                    }
                }
            }
            if (item.outputs.size >= 4) {
                internalTuples += AbiOutput(
                    name = "",
                    type = "tuple",
                    internalType = "struct $contractName.${item.name?.replaceFirstChar { it.titlecase() }}Output",
                    components = item.outputs
                )
            }
            when (item.type) {
                AbiType.function -> {
                    functionIndex += index
                    val functionName = item.name!!
                    when (functionName in functionNames) {
                        true -> overloadingFunctions.getOrPut(functionName) {
                            mutableSetOf(functions().indexOfFirst { it.name == functionName })
                        } += index

                        false -> functionNames += functionName
                    }
                }

                AbiType.event -> eventIndex += index

                AbiType.constructor,
                AbiType.fallback,
                AbiType.receive,
                AbiType.error,
                null -> Unit
            }
        }
    }

    fun topLevelStructures(): Set<AbiComponent> = topLevelTuples.values.toSet()
    fun internalStructures(): Set<AbiComponent> = internalTuples
    fun externalStructures(): Set<AbiComponent> = externalTuples
    fun functions() = functionIndex.asSequence().map { items[it] }
    fun events() = eventIndex.asSequence().map { items[it] }

    companion object {
        fun fromFile(abiFile: File): Abi {
            val contractName = abiFile.name.replace(".json", "")
            val jsonString = abiFile.readText()
            return runCatching { Abi(contractName, Json.decodeFromString<List<AbiItem>>(jsonString)) }
                .getOrElse {
                    val deployment = Json.decodeFromString<Deployment>(jsonString)
                    Abi(contractName, deployment.abi)
                }

        }
    }
}
