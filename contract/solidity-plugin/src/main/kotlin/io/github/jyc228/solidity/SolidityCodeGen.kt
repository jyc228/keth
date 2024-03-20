package io.github.jyc228.solidity

import org.gradle.configurationcache.extensions.capitalized

abstract class SolidityCodeGen {
    protected val String.importPackagePath
        get() = when (this) {
            "@Indexed" -> "io.github.jyc228.ethereum.contract.Indexed"
            "Address" -> "io.github.jyc228.ethereum.Address"
            "Hash" -> "io.github.jyc228.ethereum.Hash"
            "HexInt" -> "io.github.jyc228.ethereum.HexInt"
            "HexULong" -> "io.github.jyc228.ethereum.HexULong"
            "HexBigInt" -> "io.github.jyc228.ethereum.HexBigInt"
            "AbstractContract" -> "io.github.jyc228.ethereum.contract.AbstractContract"
            "Contract" -> "io.github.jyc228.ethereum.contract.Contract"
            "ContractEvent" -> "io.github.jyc228.ethereum.contract.ContractEvent"
            "ContractEventFactory" -> "io.github.jyc228.ethereum.contract.ContractEventFactory"
            "ContractFunctionP0" -> "io.github.jyc228.ethereum.contract.ContractFunctionP0"
            "ContractFunctionP1" -> "io.github.jyc228.ethereum.contract.ContractFunctionP1"
            "ContractFunctionP2" -> "io.github.jyc228.ethereum.contract.ContractFunctionP2"
            "ContractFunctionP3" -> "io.github.jyc228.ethereum.contract.ContractFunctionP3"
            "ContractFunctionP4" -> "io.github.jyc228.ethereum.contract.ContractFunctionP4"
            "ContractFunctionP5" -> "io.github.jyc228.ethereum.contract.ContractFunctionP5"
            "ContractFunctionP6" -> "io.github.jyc228.ethereum.contract.ContractFunctionP6"
            "ContractFunctionP7" -> "io.github.jyc228.ethereum.contract.ContractFunctionP7"
            "ContractFunctionP8" -> "io.github.jyc228.ethereum.contract.ContractFunctionP8"
            "ContractFunctionP9" -> "io.github.jyc228.ethereum.contract.ContractFunctionP9"
            "ContractFunctionRequest" -> "io.github.jyc228.ethereum.contract.ContractFunctionRequest"
            "Indexed" -> "io.github.jyc228.ethereum.contract.Indexed"
            "ApiResult" -> "io.github.jyc228.ethereum.rpc.ApiResult"
            "EthApi" -> "io.github.jyc228.ethereum.rpc.eth.EthApi"
            "BigInteger" -> "java.math.BigInteger"
            else -> ""
        }

    protected fun AbiItem.outputToKotlinType(): String? {
        if (outputs.isEmpty()) return null
        return when (outputs.size) {
            1 -> outputs[0].typeToKotlin
            2 -> "Pair<${outputs.joinToString(", ") { it.typeToKotlin }}>"
            3 -> "Triple<${outputs.joinToString(", ") { it.typeToKotlin }}>"
            else -> "${name?.capitalized()}Output"
        }
    }

    protected val AbiComponent.typeToKotlin
        get() = when (type) {
            "tuple" -> requireNotNull(internalType) { "invalid abi" }.split(" ")[1]
            "tuple[]" -> requireNotNull(internalType) { "invalid abi" }.split(" ")[1].let {
                "List<${it.removeSuffix("[]")}>"
            }

            "address" -> "Address"
            "address[]" -> "List<Address>"
            "int24" -> "HexInt"
            "int256" -> "HexBigInt"
            "uint8" -> "HexInt"
            "uint16" -> "HexULong"
            "uint24" -> "HexULong"
            "uint32" -> "HexULong"
            "uint48" -> "HexULong"
            "uint64" -> "HexULong"
            "uint96" -> "HexULong"
            "uint128" -> "HexBigInt"
            "uint160" -> "HexBigInt"
            "uint256" -> "HexBigInt"
            "uint128[]" -> "List<HexBigInt>"
            "uint256[]" -> "List<HexBigInt>"
            "bytes" -> "ByteArray"
            "bytes1" -> "ByteArray"
            "bytes4" -> "ByteArray"
            "bytes32" -> "Hash"
            "string" -> "String"
            "string[]" -> "List<String>"
            "bool" -> "Boolean"
            "bytes[]" -> "List<ByteArray>"
            "bytes32[]" -> "List<ByteArray>"
            else -> error("unsupported type $this")
        }
}
