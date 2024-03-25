package io.github.jyc228.ethereum.abi

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import io.github.jyc228.ethereum.HexString
import io.github.jyc228.solidity.AbiInput
import io.github.jyc228.solidity.AbiItem
import java.math.BigInteger
import kotlin.reflect.full.memberProperties
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyObject

class GraalJsAbi(private val context: Context) : Abi, AutoCloseable by context {
    constructor(jsCode: String) : this(
        Context
            .newBuilder("js")
            .engine(Engine.newBuilder().option("engine.WarnInterpreterOnly", "false").build())
            .build()
            .apply { eval("js", jsCode) }
    )

    private val mapper = jacksonMapperBuilder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .serializationInclusion(JsonInclude.Include.NON_NULL)
        .withConfigOverride(BigInteger::class.java) { it.format = JsonFormat.Value.forShape(JsonFormat.Shape.STRING) }
        .build()

    override fun decodeLog(inputs: List<AbiInput>, hex: String, topics: List<String>): Map<String, String?> {
        val result = context.getBindings("js").getMember("decodeLog").execute(
            ProxyArray.fromList(inputs.map { AbiProxyObject(it) }),
            hex,
            ProxyArray.fromList(topics)
        )
        return inputs.associate { input ->
            val value = result.getMember(input.name)
            input.name to when {
                value == null || value.isNull -> null
                value.isBoolean -> value.asBoolean().toString()
                else -> value.asString()
            }
        }
    }

    override fun decodeParameters(types: List<String>, hex: String): List<Any> {
        val result = context.getBindings("js").getMember("decodeParameters").execute(
            ProxyArray.fromList(types),
            hex.removePrefix("0x")
        )

        return types.indices.map {
            val data = result.getMember("$it")
            when (data.isString) {
                true -> data.asString()
                false -> data.`as`(Array::class.java)
            }
        }
    }

    override fun encodeParameters(types: List<String>, parameters: List<*>): String {
        return context.getBindings("js").getMember("encodeParameters").execute(
            ProxyArray.fromList(types),
            ProxyArray.fromList(parameters.map { (it as? HexString?)?.hex ?: it })
        ).asString()
    }

    override fun encodeFunctionCall(abiItem: AbiItem, parameters: List<*>): String {
        return context.getBindings("js").getMember("encodeFunctionCall").execute(
            Json.encodeToString(abiItem),
            mapper.writeValueAsString(parameters.map { (it as? HexString?)?.hex ?: it })
        ).asString()
    }

    companion object {
        fun init(): GraalJsAbi {
            val resource = GraalJsAbi::class.java.getResource("/js/bundle.js") ?: error("bundle.js not exist")
            return GraalJsAbi(resource.readText())
        }
    }

    private class AbiProxyObject(val abi: AbiInput) : ProxyObject {
        override fun getMember(key: String?): Any? {
            val r = propertyByName[key]?.get(abi)
            if (r is Collection<*> && r.isEmpty()) {
                return null
            }
            return propertyByName[key]?.get(abi)
        }

        override fun getMemberKeys(): Any = propertyByName.keys
        override fun hasMember(key: String?): Boolean = key in propertyByName
        override fun putMember(key: String?, value: Value?) = error("putMember does not support")

        companion object {
            private val propertyByName = AbiInput::class.memberProperties.associateBy { it.name }
        }
    }
}
