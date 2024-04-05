package io.github.jyc228.ethereum.contract

import io.github.jyc228.ethereum.HexData
import io.github.jyc228.ethereum.HexString
import io.github.jyc228.ethereum.abi.Abi
import io.github.jyc228.solidity.AbiInput
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
import org.intellij.lang.annotations.Language

abstract class AbstractContractFunction<R>(
    private val returnType: KType,
    jsonAbi: String,
    private val sig: String
) {
    protected val abi: AbiItem by lazy(LazyThreadSafetyMode.NONE) { Json.decodeFromString(jsonAbi) }

    protected fun encodeFunctionCall(vararg parameters: Any?): String {
        if (parameters.isEmpty()) return sig
        val type = abi.inputs.map { createType(it) }
        return "${sig.take(10)}${Abi.encodeParameters(type, parameters.map { (it as? HexString)?.hex ?: it })}"
    }

    private fun createType(abi: AbiInput): String {
        if (abi.type == "tuple" && abi.components.isNotEmpty()) {
            return abi.components.joinToString(",", prefix = "(", postfix = ")") { createType(it) }
        }
        return abi.type
    }

    @Suppress("UNCHECKED_CAST")
    fun decodeResult(result: HexData?): R {
        if (result == null) return null as R
        return Abi.decodeParameters(abi.outputs.map { it.type }, result.hex.removePrefix("0x"))[0] as R
    }
}

class ContractFunctionP0<R>(
    private val kFunction: KFunction1<*, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall() = super.encodeFunctionCall()
}

class ContractFunctionP1<P1, R>(
    private val kFunction: KFunction2<*, P1, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(p1: P1) = super.encodeFunctionCall(p1)
}

class ContractFunctionP2<P1, P2, R>(
    private val kFunction: KFunction3<*, P1, P2, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(p1: P1, p2: P2) = super.encodeFunctionCall(p1, p2)
}

class ContractFunctionP3<P1, P2, P3, R>(
    private val kFunction: KFunction4<*, P1, P2, P3, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(p1: P1, p2: P2, p3: P3) = super.encodeFunctionCall(p1, p2, p3)
}

class ContractFunctionP4<P1, P2, P3, P4, R>(
    private val kFunction: KFunction5<*, P1, P2, P3, P4, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(p1: P1, p2: P2, p3: P3, p4: P4) = super.encodeFunctionCall(p1, p2, p3, p4)
}

class ContractFunctionP5<P1, P2, P3, P4, P5, R>(
    private val kFunction: KFunction6<*, P1, P2, P3, P4, P5, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5) = super.encodeFunctionCall(p1, p2, p3, p4, p5)
}

class ContractFunctionP6<P1, P2, P3, P4, P5, P6, R>(
    private val kFunction: KFunction7<*, P1, P2, P3, P4, P5, P6, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6) =
        super.encodeFunctionCall(p1, p2, p3, p4, p5, p6)
}

class ContractFunctionP7<P1, P2, P3, P4, P5, P6, P7, R>(
    private val kFunction: KFunction8<*, P1, P2, P3, P4, P5, P6, P7, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7) =
        super.encodeFunctionCall(p1, p2, p3, p4, p5, p6, p7)
}

class ContractFunctionP8<P1, P2, P3, P4, P5, P6, P7, P8, R>(
    private val kFunction: KFunction9<*, P1, P2, P3, P4, P5, P6, P7, P8, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8) =
        super.encodeFunctionCall(p1, p2, p3, p4, p5, p6, p7, p8)
}

class ContractFunctionP9<P1, P2, P3, P4, P5, P6, P7, P8, P9, R>(
    private val kFunction: KFunction10<*, P1, P2, P3, P4, P5, P6, P7, P8, P9, ContractFunctionRequest<R>>,
    @Language("json")
    jsonAbi: String,
    sig: String
) : AbstractContractFunction<R>(kFunction.returnType, jsonAbi, sig) {
    fun encodeFunctionCall(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9) =
        super.encodeFunctionCall(p1, p2, p3, p4, p5, p6, p7, p8, p9)

    @Suppress("UNCHECKED_CAST")
    fun <R> decodeFunctionCall(input: String, callParameter: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R): R {
        val params = Abi.decodeParameters(
            abi.inputs.map { it.type },
            // remove 0x and function signature (4 bytes)
            input.drop(10),
        )
        return callParameter(
            params[0] as P1,
            params[1] as P2,
            params[2] as P3,
            params[3] as P4,
            params[4] as P5,
            params[5] as P6,
            params[6] as P7,
            params[7] as P8,
            params[8] as P9,
        )
    }
}
