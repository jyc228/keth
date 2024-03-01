package io.github.jyc228.solidity

import io.github.jyc228.kotlin.codegen.GenerationContext
import io.github.jyc228.kotlin.codegen.KtFileBuilder

class LibraryGenerator(
    val packagePath: String,
    val abiIOByName: MutableMap<String, AbiComponent> = mutableMapOf()
) : SolidityCodeGen() {
    val generated = mutableSetOf<String>()
    fun generate(objectName: String): KtFileBuilder {
        if (objectName == "_Struct") {
            return generateStruct()
        }
        return KtFileBuilder(
            GenerationContext { it.importPackagePath },
            objectName,
            packagePath
        ).apply {
            type().`object`(objectName).body {
                while (abiIOByName.isNotEmpty()) {
                    abiIOByName.toList().forEach { (typeName, io) ->
                        generated += typeName
                        abiIOByName -= typeName
                        type().dataClass(typeName).constructor {
                            io.components.forEach { item ->
                                if (item.type == "tuple") {
                                    val struct = item.resolveStruct()
                                    if (struct.ownerName == objectName) {
                                        parameter(item.name).immutable().type(struct.name)
                                        if (struct.name !in generated) abiIOByName[struct.name] = item
                                    } else {
                                        TODO()
                                    }
                                } else {
                                    parameter(item.name).immutable().type(item.typeToKotlin)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun generateStruct(): KtFileBuilder {
        return KtFileBuilder(
            GenerationContext { it.importPackagePath },
            "_Struct",
            packagePath
        ).apply {
            while (abiIOByName.isNotEmpty()) {
                abiIOByName.toList().forEach { (typeName, io) ->
                    generated += typeName
                    abiIOByName -= typeName
                    val struct = io.resolveStruct()
                    type().dataClass(struct.name).constructor {
                        io.components.forEach { output ->
                            parameter(output.name)
                                .immutable()
                                .type(output.typeToKotlin)
                        }
                    }
                    context.reportType("$packagePath.${struct.name}")
                }
            }
        }
    }
}