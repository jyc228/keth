package io.github.jyc228.ethereum.contract

import io.github.jyc228.ethereum.HexData
import io.github.jyc228.ethereum.HexString
import io.github.jyc228.ethereum.abi.Abi
import io.github.jyc228.ethereum.contract.TypeExtensions.decodeEthereumValue
import io.github.jyc228.ethereum.contract.TypeExtensions.ethereumType
import io.github.jyc228.solidity.AbiInput
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

abstract class ContractEventFactory<EVENT : ContractEvent, INDEXED : Any>(
    private val event: KClass<EVENT>,
    private val indexed: KClass<INDEXED>,
    val hash: String
) : (INDEXED.() -> Unit) -> List<String?> {
    private val const = requireNotNull(event.primaryConstructor) { "${event.simpleName} primaryConstructor not exist" }
    private val inputs by lazy(LazyThreadSafetyMode.NONE) {
        val indexedProperties = indexed.memberProperties.map { it.name }.toSet()
        const.parameters.map { p ->
            AbiInput(
                name = p.name!!,
                type = p.type.ethereumType,
                indexed = p.name in indexedProperties
            )
        }
    }

    override fun invoke(indexedParameter: INDEXED.() -> Unit): List<String> {
        return emptyList()
    }

    fun buildTopics(init: (INDEXED.() -> Unit)? = null): List<String?> {
        if (init == null) {
            return listOf(hash)
        }
        return buildList {
            add(hash)
            val instance = indexed.createInstance().apply(init)
            indexed.memberProperties.forEach { p ->
                when (val v = p.get(instance)) {
                    null -> add(null)
                    else -> when (HexString::class.isSuperclassOf(p.returnType.classifier as KClass<*>)) {
                        true -> add((v as HexString).hex.replaceFirst("0x", "0x000000000000000000000000"))
                        false -> TODO()
                    }
                }
            }
        }
    }

    fun createIndexedInstance() = indexed.constructors.first().call()

    fun decodeIf(
        data: HexData,
        topics: List<HexData> = emptyList()
    ): EVENT? {
        return if (topics[0].hex == hash) decode(data, topics) else null
    }

    fun decode(
        data: HexData,
        topics: List<HexData> = emptyList()
    ): EVENT {
        val resultByName = when (topics[0].hex) {
            hash -> Abi.decodeLog(inputs, data.hex, topics.drop(1).map { it.hex })
            else -> Abi.decodeLog(inputs, data.hex, topics.map { it.hex })
        }
        val params = const.parameters.associateWith { p -> p.type.decodeEthereumValue(resultByName[p.name]) }
        return const.callBy(params)
    }
}