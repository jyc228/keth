package com.github.jyc228.keth.solidity

abstract class SolidityCodeGen {
    protected val String.importPackagePath
        get() = when (this) {
            "@Indexed" -> "io.github.jyc228.keth.contract.Indexed"
            "Address" -> "io.github.jyc228.keth.Address"
            "Hash" -> "io.github.jyc228.keth.Hash"
            "HexInt" -> "io.github.jyc228.keth.HexInt"
            "HexULong" -> "io.github.jyc228.keth.HexULong"
            "HexBigInt" -> "io.github.jyc228.keth.HexBigInt"
            "AbstractContract" -> "io.github.jyc228.keth.contract.AbstractContract"
            "Contract" -> "io.github.jyc228.keth.contract.Contract"
            "ContractEvent" -> "io.github.jyc228.keth.contract.ContractEvent"
            "ContractEventFactory" -> "io.github.jyc228.keth.contract.ContractEventFactory"
            "ContractFunctionP0" -> "io.github.jyc228.keth.contract.ContractFunctionP0"
            "ContractFunctionP1" -> "io.github.jyc228.keth.contract.ContractFunctionP1"
            "ContractFunctionP2" -> "io.github.jyc228.keth.contract.ContractFunctionP2"
            "ContractFunctionP3" -> "io.github.jyc228.keth.contract.ContractFunctionP3"
            "ContractFunctionP4" -> "io.github.jyc228.keth.contract.ContractFunctionP4"
            "ContractFunctionP5" -> "io.github.jyc228.keth.contract.ContractFunctionP5"
            "ContractFunctionP6" -> "io.github.jyc228.keth.contract.ContractFunctionP6"
            "ContractFunctionP7" -> "io.github.jyc228.keth.contract.ContractFunctionP7"
            "ContractFunctionP8" -> "io.github.jyc228.keth.contract.ContractFunctionP8"
            "ContractFunctionP9" -> "io.github.jyc228.keth.contract.ContractFunctionP9"
            "ContractFunctionRequest" -> "io.github.jyc228.keth.contract.ContractFunctionRequest"
            "Indexed" -> "io.github.jyc228.keth.contract.Indexed"
            "ApiResult" -> "io.github.jyc228.keth.rpc.ApiResult"
            "EthApi" -> "io.github.jyc228.keth.rpc.eth.EthApi"
            "BigInteger" -> "java.math.BigInteger"
            else -> ""
        }

    protected fun AbiItem.outputToKotlinType(): String? {
        if (outputs.isEmpty()) return null
        return when (outputs.size) {
            1 -> outputs[0].typeToKotlin
            2 -> "Pair<${outputs.joinToString(", ") { it.typeToKotlin }}>"
            3 -> "Triple<${outputs.joinToString(", ") { it.typeToKotlin }}>"
            else -> "${name?.replaceFirstChar { it.titlecase() }}Output"
        }
    }

    protected val AbiComponent.typeToKotlin: String
        get() {
            val arrayStartIndex = type.indexOf('[')
            val type = if (arrayStartIndex == -1) type else type.take(arrayStartIndex)
            val kotlinType = when (type) {
                "tuple" -> requireNotNull(internalType) { "invalid abi" }.split(" ")[1]
                "bool" -> "Boolean"
                "address" -> "Address"
                "string" -> "String"
                else -> when {
                    type.startsWith("bytes") -> "ByteArray"
                    type.startsWith("int") -> "BigInteger"
                    type.startsWith("uint") -> "BigInteger"
                    else -> error("unsupported type $this")
                }
            }
            if (arrayStartIndex == -1) {
                return kotlinType
            }
            return "List<${kotlinType.removeSuffix("[]")}>"
        }
}
