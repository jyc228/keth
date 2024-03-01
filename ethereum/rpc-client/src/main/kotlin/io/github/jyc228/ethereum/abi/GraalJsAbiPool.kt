package io.github.jyc228.ethereum.abi

import io.github.jyc228.solidity.AbiInput
import io.github.jyc228.solidity.AbiItem
import java.util.Collections

class GraalJsAbiPool : Abi {
    private val pool =
        Collections.synchronizedList(mutableListOf(GraalJsAbi.init(), GraalJsAbi.init(), GraalJsAbi.init()))

    override fun decodeLog(inputs: List<AbiInput>, hex: String, topics: List<String>): Map<String, String?> =
        withAbi { decodeLog(inputs, hex, topics) }

    override fun decodeParameters(types: List<String>, hex: String): List<Any> =
        withAbi { decodeParameters(types, hex) }

    override fun encodeParameters(types: List<String>, parameters: List<*>): String =
        withAbi { encodeParameters(types, parameters) }

    override fun encodeFunctionCall(abiItem: AbiItem, parameters: List<*>): String =
        withAbi { encodeFunctionCall(abiItem, parameters) }

    private fun <R> withAbi(runner: Abi.() -> R): R {
        var abi = pool.removeFirstOrNull()
        if (pool.size > 10) repeat(5) { pool.removeLast() }
        if (abi == null) abi = GraalJsAbi.init()
        val result = runner(abi)
        pool.add(abi)
        return result
    }
}