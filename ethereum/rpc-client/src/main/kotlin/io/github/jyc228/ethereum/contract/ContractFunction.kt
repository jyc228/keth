package io.github.jyc228.ethereum.contract

import io.github.jyc228.ethereum.HexData
import io.github.jyc228.ethereum.abi.Abi
import io.github.jyc228.ethereum.contract.TypeExtensions.decodeEthereumValue
import io.github.jyc228.solidity.AbiItem
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction10
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3
import kotlin.reflect.KFunction4
import kotlin.reflect.KFunction5
import kotlin.reflect.KFunction6
import kotlin.reflect.KFunction7
import kotlin.reflect.KFunction8
import kotlin.reflect.KFunction9
import kotlin.reflect.KType
import kotlinx.serialization.json.Json
import org.bouncycastle.jcajce.provider.digest.Keccak
import org.bouncycastle.util.encoders.Hex
import org.intellij.lang.annotations.Language

abstract class AbstractContractFunction<R>(
    private val returnType: KType,
    jsonAbi: String,
    private val sig: String
) {
    protected val abi: AbiItem by lazy(LazyThreadSafetyMode.NONE) { Json.decodeFromString(jsonAbi) }

    @Suppress("UNCHECKED_CAST")
    fun decodeResult(result: HexData?): R {
        if (result == null) return null as R
        return returnType.arguments[0].type?.decodeEthereumValue(result.hex, abi.outputs) as R
    }
}

class ContractFunctionP0<R>(
    private val kFunction: KFunction1<*, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(): String {
        return "${kFunction.name}()".keccak256Hash()
    }

    private fun String.keccak256Hash(): String {
        val bytes = with(Keccak.Digest256()) {
            forEach { update(it.code.toByte()) }
            digest()
        }
        return "0x${Hex.encode(bytes).decodeToString()}"
    }
}

class ContractFunctionP1<P1, R>(
    private val kFunction: KFunction2<*, P1, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(p1: P1): String {
        return Abi.encodeFunctionCall(abi, listOf(p1))
    }
}

class ContractFunctionP2<P1, P2, R>(
    private val kFunction: KFunction3<*, P1, P2, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(p1: P1, p2: P2): String {
        return Abi.encodeFunctionCall(abi, listOf(p1, p2))
    }
}

class ContractFunctionP3<P1, P2, P3, R>(
    private val kFunction: KFunction4<*, P1, P2, P3, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(p1: P1, p2: P2, p3: P3): String {
        return Abi.encodeFunctionCall(abi, listOf(p1, p2, p3))
    }
}

class ContractFunctionP4<P1, P2, P3, P4, R>(
    private val kFunction: KFunction5<*, P1, P2, P3, P4, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(p1: P1, p2: P2, p3: P3, p4: P4): String {
        return Abi.encodeFunctionCall(abi, listOf(p1, p2, p3, p4))
    }
}

class ContractFunctionP5<P1, P2, P3, P4, P5, R>(
    private val kFunction: KFunction6<*, P1, P2, P3, P4, P5, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5): String {
        return Abi.encodeFunctionCall(abi, listOf(p1, p2, p3, p4, p5))
    }
}

class ContractFunctionP6<P1, P2, P3, P4, P5, P6, R>(
    private val kFunction: KFunction7<*, P1, P2, P3, P4, P5, P6, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6): String {
        return Abi.encodeFunctionCall(abi, listOf(p1, p2, p3, p4, p5, p6))
    }
}

class ContractFunctionP7<P1, P2, P3, P4, P5, P6, P7, R>(
    private val kFunction: KFunction8<*, P1, P2, P3, P4, P5, P6, P7, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7): String {
        return Abi.encodeFunctionCall(abi, listOf(p1, p2, p3, p4, p5, p6, p7))
    }
}

class ContractFunctionP8<P1, P2, P3, P4, P5, P6, P7, P8, R>(
    private val kFunction: KFunction9<*, P1, P2, P3, P4, P5, P6, P7, P8, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8): String {
        return Abi.encodeFunctionCall(abi, listOf(p1, p2, p3, p4, p5, p6, p7, p8))
    }
}

class ContractFunctionP9<P1, P2, P3, P4, P5, P6, P7, P8, P9, R>(
    private val kFunction: KFunction10<*, P1, P2, P3, P4, P5, P6, P7, P8, P9, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9): String {
        return Abi.encodeFunctionCall(abi, listOf(p1, p2, p3, p4, p5, p6, p7, p8, p9))
    }

    @Suppress("UNCHECKED_CAST")
    fun <R> decodeFunctionCall(input: String, callParameter: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R): R {
        val params = Abi.decodeParameters(
            abi.inputs.map { it.type },
            // remove 0x and function signature (4 bytes)
            input.drop(10),
        )
        return callParameter(
            kFunction.parameters[1].type.decodeEthereumValue(params[0].toString()) as P1,
            kFunction.parameters[2].type.decodeEthereumValue(params[1].toString()) as P2,
            kFunction.parameters[3].type.decodeEthereumValue(params[2].toString()) as P3,
            kFunction.parameters[4].type.decodeEthereumValue(params[3].toString()) as P4,
            kFunction.parameters[5].type.decodeEthereumValue(params[4].toString()) as P5,
            kFunction.parameters[6].type.decodeEthereumValue(params[5].toString()) as P6,
            kFunction.parameters[7].type.decodeEthereumValue(params[6].toString()) as P7,
            kFunction.parameters[8].type.decodeEthereumValue(params[7].toString()) as P8,
            kFunction.parameters[9].type.decodeEthereumValue(params[8].toString()) as P9,
        )
    }
}
