package io.github.jyc228.ethereum.abi

import io.github.jyc228.solidity.AbiInput
import io.github.jyc228.solidity.AbiItem
import java.nio.ByteBuffer

object AbiImpl : Abi {
    override fun decodeLog(inputs: List<AbiInput>, hex: String, topics: List<String>): Map<String, Any> {
        val types = inputs.fold(LogTypes()) { types, abi -> types.add(abi) }
        val result = TupleCodec.decode(TupleType(types.nonIndexed), hexToByteBuffer(hex))
        types.indexed.forEachIndexed { i, t -> result[t.key] = Codec.decode(t.type, topics[i + 1]) }
        return result
    }

    override fun decodeParameters(types: List<String>, hex: String): List<Any> {
        return TupleCodec.decode(TupleType(types.map(Type::of)), hexToByteBuffer(hex)).values.toList()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun hexToByteBuffer(hex: String) = ByteBuffer.wrap(hex.removePrefix("0x").hexToByteArray())

    @OptIn(ExperimentalStdlibApi::class)
    override fun encodeParameters(types: List<String>, parameters: List<*>): String {
        return Codec.encode(TupleType(types.map(Type::of)), parameters).toHexString()
    }

    override fun encodeFunctionCall(abiItem: AbiItem, parameters: List<*>): String {
        TODO("Not yet implemented")
    }

    private data class LogTypes(
        val indexed: MutableList<TypeWithKey> = mutableListOf(),
        val nonIndexed: MutableList<TypeWithKey> = mutableListOf()
    ) {
        fun add(abi: AbiInput) = apply {
            when (abi.indexed == true) {
                true -> indexed
                false -> nonIndexed
            } += Type.of(abi.type).withKey(abi.name)
        }
    }
}
