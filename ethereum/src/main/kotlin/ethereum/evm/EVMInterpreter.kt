package ethereum.evm

open class EVMInterpreter(private val instructionSet: InstructionSet) {
    open suspend fun execute(context: FrameContext): Result<Unit> {
        while (!context.stop) {
            val operation = instructionSet[context.contract.code[context.pc]] ?: error("")
            execute(operation, context)
            context.pc++
        }
        return Result.success(Unit)
    }

    open suspend fun execute(operation: Operation, context: FrameContext) {
        if (operation.dynamicGas != null) {
            var memSize = 0
            if (operation.memorySize != null) {
                memSize = operation.memorySize.invoke(context)
            }
            context.gas -= operation.dynamicGas.invoke(context)
            if (context.memory.size < memSize) {
                context.memory = context.memory.copyOf(memSize)
            }
        }
        context.gas -= operation.gas
        if (context.gas < 0) TODO()
        operation.execute(context)
    }
}

class EVMDebugInterpreter(instructionSet: InstructionSet) : EVMInterpreter(instructionSet) {
    private var index = 1
    override suspend fun execute(context: FrameContext): Result<Unit> {
        return super.execute(context)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun execute(operation: Operation, context: FrameContext) {
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
        super.execute(operation, context)
        if (operation.opCode.name.startsWith("PUSH")) {
            println("$prefix, ${beforeGas - context.gas} ${context.stack.last()}")
        } else if (operation.opCode.name.startsWith("SLOAD")) {
            println("$prefix, ${beforeGas - context.gas}, value ${context.stack.last()}")
        } else if (operation.opCode == OpCode.KECCAK256) {
            println("$prefix, ${beforeGas - context.gas}, result ${context.stack.last()}")
        } else if (operation.opCode == OpCode.MSTORE) {
            println("$prefix, ${context.memory.toHexString()}")
        } else {
            println("$prefix, ${beforeGas - context.gas}")
        }
        index++
    }
}