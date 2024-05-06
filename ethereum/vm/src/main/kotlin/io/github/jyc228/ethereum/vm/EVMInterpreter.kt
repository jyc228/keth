package io.github.jyc228.ethereum.vm

open class EVMInterpreter(private val instructionSet: InstructionSet) {
    open suspend fun execute(context: FrameContext): EVMReturn {
        context.interpreter = this
        while (context.result == null) {
            val operation = instructionSet[context.contract.code[context.pc]]
            if (operation == null) {
                context.result = EVMReturn.unknownOpCode(context.contract.code[context.pc])
            } else {
                execute(operation, context)
                context.pc++
            }
        }
        return context.result!!
    }

    open suspend fun execute(operation: Operation, context: FrameContext) {
        val memSize = operation.memorySize?.invoke(context) ?: 0
        context.gas -= operation.gas
        context.gas -= operation.dynamicGas?.invoke(context, memSize) ?: 0
        if (context.memory.size < memSize) {
            context.memory = context.memory.copyOf(memSize)
        }
        if (context.gas < 0) context.result = EVMReturn.outOfGas()
        else operation.execute(context)
    }
}

class EVMDebugInterpreter(instructionSet: InstructionSet) : EVMInterpreter(instructionSet) {
    private var index = 0
    override suspend fun execute(context: FrameContext): EVMReturn {
        return super.execute(context)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun execute(operation: Operation, context: FrameContext) {
        index++
        val beforeGas = context.gas
        var prefix = "$index\t${context.pc}\t${context.gas}\t${operation.opCode}"
        if (operation.opCode.name.startsWith("SLOAD")) {
            prefix += "(0x${context.contract.address.bytes.toHexString()}, ${context.stack.last()})"
        }
        if (operation.opCode == OpCode.KECCAK256) {
//            val offset = BigInteger(1, context.stack.data[context.stack.data.lastIndex]).toInt()
//            val size = BigInteger(1, context.stack.data[context.stack.data.lastIndex - 1]).toInt()
//            val input = context.memory.copyOfRange(offset, offset + size)
//            prefix += "(0x${input.toHexString()})"
        }
        println(prefix)
        super.execute(operation, context)
//        if (operation.opCode.name.startsWith("PUSH")) {
//            println("$prefix, ${beforeGas - context.gas} ${context.stack.last()}")
//        } else if (operation.opCode.name.startsWith("SLOAD")) {
//            println("$prefix, ${beforeGas - context.gas}, value ${context.stack.last()}")
//        } else if (operation.opCode == OpCode.KECCAK256) {
//            println("$prefix, ${beforeGas - context.gas}, result ${context.stack.last()}")
//        } else if (operation.opCode == OpCode.MSTORE) {
//            println("$prefix, ${context.memory.toHexString()}")
//        } else {
//            println("$prefix, ${beforeGas - context.gas}")
//        }
    }
}