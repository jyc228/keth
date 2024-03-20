package io.github.jyc228.solidity

import io.github.jyc228.kotlin.codegen.BodyBuilder
import io.github.jyc228.kotlin.codegen.GenerationContext
import io.github.jyc228.kotlin.codegen.KtFileBuilder
import io.github.jyc228.kotlin.codegen.TypeBuilder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bouncycastle.jcajce.provider.digest.Keccak
import org.bouncycastle.util.encoders.Hex

class ContractGenerator(
    val packagePath: String,
    val abi: Abi,
) : SolidityCodeGen() {
    fun generateInterface() = KtFileBuilder(
        context = GenerationContext { it.importPackagePath },
        name = abi.contractName,
        packagePath = packagePath
    ).apply { type().buildContractInterface(abi.contractName) }

    private fun TypeBuilder.buildContractInterface(interfaceName: String) = `interface`(interfaceName)
        .inherit { `interface`("Contract").typeParameter("$interfaceName.Event") }
        .body {
            abi.functions().forEach { item -> addFunction(item) }
            abi.internalStructures().forEach { addStruct(it, interfaceName) }
            type().sealedInterface("Event").inherit { `interface`("ContractEvent") }
            abi.events().forEach { item -> addEvent(item) }

            companionObject()
                .inherit {
                    `class`("Contract.Factory")
                        .typeParameter(interfaceName)
                        .invokeConstructor("::${interfaceName}Impl")
                }
                .body {
                    abi.functions().forEach { item -> addFunctionMetadata(item, interfaceName) }
                }
        }

    private fun BodyBuilder.addFunction(item: AbiItem) {
        function(item.name!!)
            .parameters(item.inputs.mapIndexed { i, input -> input.name.ifBlank { "key$i" } to input.typeToKotlin })
            .returnType("ContractFunctionRequest", listOf(item.outputToKotlinType() ?: "Unit"))
        context.reportType("Contract")
    }

    private fun BodyBuilder.addStruct(item: AbiComponent, interfaceName: String) {
        val struct = item.resolveStruct()
        type().dataClass(struct.name).constructor {
            item.components.forEach { output ->
                parameter(output.name)
                    .immutable()
                    .type(output.typeToKotlin)
            }
        }
        context.reportType("$packagePath.$interfaceName.${struct.name}")
    }

    private fun BodyBuilder.addFunctionMetadata(item: AbiItem, interfaceName: String) {
        property(resolveMetadataPropertyName(item))
            .immutable()
            .defaultValue("ContractFunctionP${item.inputs.size}") {
                parameter("$interfaceName::${item.name}")
                stringTemplateParameter(Json.encodeToString(item))
                val hash = "${item.name}(${item.inputs.joinToString(",") { it.type }})"
                stringParameter("0x${hash.keccak256Hash()}")
            }
    }

    private fun BodyBuilder.addEvent(event: AbiItem) {
        val eventClass = when (event.inputs.isEmpty()) {
            true -> type().`class`(event.name!!)
            false -> type().dataClass(event.name!!)
        }
        eventClass
            .constructor {
                event.inputs.forEach { input ->
                    parameter(input.name)
                        .immutable()
                        .type(input.typeToKotlin)
                }
            }
            .inherit { `interface`("Event") }
            .body {
                val hasIndexed = event.inputs.any { it.indexed == true }
                if (hasIndexed) {
                    type().`class`("Indexed").body {
                        event.inputs.forEach {
                            if (it.indexed == true) {
                                property(it.name).mutable().type(it.typeToKotlin, true).defaultNull()
                            }
                        }
                    }
                }
                companionObject().inherit {
                    val hash = "${event.name}(${event.inputs.joinToString(",") { it.type }})"
                    val indexedClass = if (hasIndexed) "Indexed" else "Unit"
                    `class`("ContractEventFactory")
                        .typeParameter(event.name!!)
                        .typeParameter(if (hasIndexed) "Indexed" else "Unit")
                        .invokeConstructor(
                            "${event.name}::class",
                            "${indexedClass}::class",
                            "\"0x${hash.keccak256Hash()}\""
                        )
                }
            }
    }

    fun generateDefaultImplementation() = KtFileBuilder(
        context = GenerationContext { it.importPackagePath },
        name = "${abi.contractName}Impl",
        packagePath = packagePath
    ).apply { type().buildContractInterfaceImplementation(abi.contractName) }

    private fun TypeBuilder.buildContractInterfaceImplementation(interfaceName: String) =
        `class`("${interfaceName}Impl")
            .constructor {
                parameter("address").type("Address")
                parameter("api").type("EthApi")
            }
            .inherit {
                `interface`(interfaceName)
                `class`("AbstractContract")
                    .typeParameter("$interfaceName.Event")
                    .invokeConstructor("address", "api")
            }
            .body {
                abi.functions().forEach { item ->
                    val parameters = buildString {
                        item.inputs.forEachIndexed { index, input ->
                            append(input.name.ifBlank { "key$index" })
                            append(", ")
                        }
                    }
                    function(item.name!!)
                        .override()
                        .parameters(item.inputs.mapIndexed { i, input -> input.name.ifBlank { "key$i" } to input.typeToKotlin })
                        .returnType(
                            "ContractFunctionRequest",
                            when (item.outputs.size > 3) {
                                true -> listOf("$interfaceName.${item.outputToKotlinType()}")
                                false -> listOf(item.outputToKotlinType() ?: "Unit")
                            }
                        )
                        .body("""return $interfaceName.${resolveMetadataPropertyName(item)}(${parameters})""")
                    context.reportType("Contract")
                }
            }

    private fun resolveMetadataPropertyName(item: AbiItem): String {
        if (item.name!! in abi.overloadingFunctions) {
            return when (item.inputs.isEmpty()) {
                true -> item.name!!
                false -> "${item.name}_${item.inputs.joinToString("_") { it.type.replace("[]", "Array") }}"
            }
        }
        return item.name!!
    }

    private fun String.keccak256Hash(): String {
        val bytes = with(Keccak.Digest256()) {
            forEach { update(it.toByte()) }
            digest()
        }
        return Hex.encode(bytes).decodeToString()
    }
}
