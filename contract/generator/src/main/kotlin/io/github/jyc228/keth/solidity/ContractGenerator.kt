package io.github.jyc228.keth.solidity

import io.github.jyc228.keth.solidity.compile.CompileResult
import io.github.jyc228.kotlin.codegen.BodyBuilder
import io.github.jyc228.kotlin.codegen.GenerationContext
import io.github.jyc228.kotlin.codegen.KtFileBuilder
import io.github.jyc228.kotlin.codegen.TypeBuilder
import io.github.jyc228.solidity.AbiComponent
import io.github.jyc228.solidity.AbiInput
import io.github.jyc228.solidity.AbiItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bouncycastle.jcajce.provider.digest.Keccak
import org.bouncycastle.util.encoders.Hex

class ContractGenerator(
    val packagePath: String,
    val compileResult: CompileResult,
) : SolidityCodeGen() {
    fun generateInterface() = KtFileBuilder(
        context = GenerationContext { it.importPackagePath },
        name = compileResult.contractName,
        packagePath = packagePath
    ).apply { type().buildContractInterface(compileResult.contractName) }

    private fun TypeBuilder.buildContractInterface(interfaceName: String) = `interface`(interfaceName)
        .inherit { `interface`("Contract").typeParameter("$interfaceName.Event") }
        .body {
            compileResult.functions().forEach { item -> addFunction(item) }
            compileResult.internalStructures().forEach { addStruct(it, interfaceName) }
            type().sealedInterface("Event").inherit { `interface`("ContractEvent") }
            compileResult.events().forEach { item -> addEvent(item) }

            companionObject()
                .inherit {
                    `class`("Contract.Factory")
                        .typeParameter(interfaceName)
                        .invokeConstructor("::${interfaceName}Impl")
                }
                .body {
                    function("bin")
                        .returnType("String")
                        .body("return \"${compileResult.bin}\"")

                    var code = "return \"0x\" + bin()"
                    compileResult.constructor()?.let { item ->
                        code += " + encodeParameters(\n${indent.next.next}${item.toJsonStringTemplate()},\n${indent.next.next}${item.inputs.joinToStringNames()}\n${indent.next})"
                    }
                    function("encodeDeploymentCallData")
                        .parameters(compileResult.constructor()?.inputs?.toParameters())
                        .body(code)
                        .returnType("String")
                    compileResult.functions().forEach { item -> addFunctionMetadata(item, interfaceName) }
                }
        }

    private fun BodyBuilder.addFunction(item: AbiItem) {
        function(item.name!!)
            .parameters(item.inputs.toParameters())
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
                parameter(item.toJsonStringTemplate())
                stringParameter("0x${item.computeSig()}")
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
                    val indexedClass = if (hasIndexed) "Indexed" else "Unit"
                    `class`("ContractEventFactory")
                        .typeParameter(event.name!!)
                        .typeParameter(if (hasIndexed) "Indexed" else "Unit")
                        .invokeConstructor(
                            "${event.name}::class",
                            "${indexedClass}::class",
                            "\"0x${event.computeSig()}\""
                        )
                }
            }
    }

    fun generateDefaultImplementation() = KtFileBuilder(
        context = GenerationContext { it.importPackagePath },
        name = "${compileResult.contractName}Impl",
        packagePath = packagePath
    ).apply { type().buildContractInterfaceImplementation(compileResult.contractName) }

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
                compileResult.functions().forEach { item ->
                    function(item.name!!)
                        .override()
                        .parameters(item.inputs.toParameters())
                        .returnType(
                            "ContractFunctionRequest",
                            when (item.outputs.size > 3) {
                                true -> listOf("$interfaceName.${item.outputToKotlinType()}")
                                false -> listOf(item.outputToKotlinType() ?: "Unit")
                            }
                        )
                        .body("""return $interfaceName.${resolveMetadataPropertyName(item)}(${item.inputs.joinToStringNames()})""")
                    context.reportType("Contract")
                }
            }

    private fun resolveMetadataPropertyName(item: AbiItem): String {
        if (item.name!! in compileResult.overloadingFunctions) {
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

    private fun List<AbiInput>.toParameters() =
        mapIndexed { i, input -> input.name.ifBlank { "key$i" } to input.typeToKotlin }

    private fun List<AbiInput>.joinToStringNames() = joinToString(", ") { it.name }

    private fun AbiItem.toJsonStringTemplate() = "\"\"\"${Json.encodeToString(this)}\"\"\""
    private fun AbiItem.computeSig() = "${name}(${inputs.joinToString(",") { it.type }})".keccak256Hash()
}
