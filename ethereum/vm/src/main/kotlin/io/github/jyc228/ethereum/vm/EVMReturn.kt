package io.github.jyc228.ethereum.vm

class EVMReturn private constructor(val data: ByteArray?, val err: EVMException?) {
    override fun toString(): String {
        if (data != null) return "success ${data.contentToString()}"
        if (err != null) return "fail $err"
        error("invalid state")
    }

    companion object {
        fun success(bytes: ByteArray) = EVMReturn(bytes, null)
        fun failure(err: EVMException) = EVMReturn(null, err)
        fun failure(message: String) = EVMReturn(null, DefaultEVMException(message))

        fun unknownOpCode(opCodeByte: Byte): EVMReturn {
            val opCode = OpCode.entries.find { it.v == opCodeByte }
            if (opCode == null) return failure(InvalidOpCodeException(opCodeByte))
            return failure(DisableOpCodeException(opCode))
        }

        fun outOfGas() = failure(DefaultEVMException("out of gas"))
        fun insufficientBalance() = failure(DefaultEVMException("insufficient balance for transfer"))
        fun executionReverted(data: ByteArray) = failure(ExecutionRevertedException(data))
        fun nonceOverflow() = failure("nonce overflow")
        fun contractAddressCollision() = failure("contract address collision")
        fun initMaxCodeSizeExceeded() = failure("max init code size exceeded")
        fun maxCodeSizeExceeded() = failure("max code size exceeded")
        fun invalidCode() = failure("contract code must not begin with 0xef")
        fun codeStoreOutOfGas() = failure("contract creation code storage out of gas")
    }
}

abstract class EVMException(message: String? = null) : RuntimeException(message)

class ExecutionRevertedException(val data: ByteArray) : EVMException()
class InvalidOpCodeException(val opCodeByte: Byte) : EVMException()
class DisableOpCodeException(val opCode: OpCode) : EVMException()
class DefaultEVMException(message: String) : EVMException(message)
