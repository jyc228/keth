package io.github.jyc228.ethereum.abi

import io.github.jyc228.solidity.AbiInput
import io.github.jyc228.solidity.AbiItem

interface Abi {
    fun decodeLog(inputs: List<AbiInput>, hex: String, topics: List<String>): Map<String, String?>
    fun decodeParameters(types: List<String>, hex: String): List<Any>
    fun encodeParameters(types: List<String>, parameters: List<*>): String
    fun encodeFunctionCall(abiItem: AbiItem, parameters: List<*>): String

    companion object : Abi by GraalJsAbiPool()
}