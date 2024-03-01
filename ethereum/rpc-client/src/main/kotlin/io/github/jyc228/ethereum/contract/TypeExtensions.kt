package io.github.jyc228.ethereum.contract

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.HexBigInt
import io.github.jyc228.ethereum.HexData
import io.github.jyc228.ethereum.HexInt
import io.github.jyc228.ethereum.HexString
import io.github.jyc228.ethereum.HexStringFactory
import io.github.jyc228.ethereum.HexULong
import io.github.jyc228.ethereum.abi.Abi
import io.github.jyc228.solidity.AbiComponent
import java.math.BigInteger
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSuperclassOf

object TypeExtensions {
    val KType.ethereumType
        get() = when (classifier) {
            BigInteger::class -> "uint256"
            Enum::class -> "uint8"
            UByte::class -> "uint8"
            Address::class -> "address"
            Hash::class -> "bytes32"
            HexData::class -> "bytes"
            Boolean::class -> "bool"
            else -> error("unsupported type $this")
        }

    fun KType.decodeEthereumValue(v: String?, outputs: List<AbiComponent>? = null): Any? {
        if (v.isNullOrEmpty()) return null
        return when (val resultType = classifier as KClass<*>) {
            HexInt::class -> HexInt(v)
            HexULong::class -> HexULong(v)
            HexBigInt::class -> HexBigInt(v)
            Boolean::class -> v.toBoolean()
            UByte::class -> v.toUByte()
            String::class -> HexData(v).toText()
            else -> when (HexString::class.isSuperclassOf(resultType)) {
                true -> (resultType.companionObjectInstance as HexStringFactory<*>).create(v)
                false -> when (outputs?.size) {
                    1 -> {
                        val types = outputs[0].components.joinToString(
                            prefix = "(",
                            postfix = ")",
                            separator = ","
                        ) { c -> c.type }
                        val decodedOutputs = Abi.decodeParameters(listOf(types), v)[0] as Array<*>
                        val const = resultType.constructors.first()
                        val param = const.parameters.associateBy { it.name }
                        const.callBy(
                            outputs[0].components.mapIndexed { index, output ->
                                val r = when (decodedOutputs[index]) {
                                    is String -> param[output.name]!!.type.decodeEthereumValue(
                                        decodedOutputs[index] as String,
                                        output.components,
                                    )

                                    is Map<*, *> -> emptyList<Any>()
                                    else -> error("")
                                }
                                param[output.name]!! to r
                            }.toMap()
                        )
                    }

                    2 -> error("unsupported type $resultType")
                    else -> error("unsupported type $resultType")
                }
            }
        }
    }
}
