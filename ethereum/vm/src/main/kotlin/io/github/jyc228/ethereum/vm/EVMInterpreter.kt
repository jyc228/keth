package io.github.jyc228.ethereum.vm

interface EVMInterpreter {
    suspend fun execute(frame: FrameContext): EVMReturn

    companion object {
        fun of(set: InstructionSet, delegate: EVMInterpreterDelegate? = null): EVMInterpreter {
            if (delegate == null) return EVMDefaultInterpreter(set)
            return EVMDefaultInterpreter.Delegate(set, delegate)
        }
    }
}

interface EVMInterpreterDelegate {
    suspend fun execute(
        frame: FrameContext,
        execute: suspend (FrameContext) -> EVMReturn
    ): EVMReturn = execute(frame)

    suspend fun execute(
        frame: FrameContext,
        operation: Operation?,
        execute: suspend (FrameContext, Operation?) -> EVMReturn?
    ): EVMReturn? = execute(frame, operation)
}

open class EVMDefaultInterpreter(private val instructionSet: InstructionSet) : EVMInterpreter {
    override suspend fun execute(frame: FrameContext): EVMReturn {
        frame.interpreter = this
        while (true) return execute(frame, instructionSet[frame.contract.code[frame.pc]]) ?: continue
    }

    open suspend fun execute(frame: FrameContext, operation: Operation?): EVMReturn? {
        if (operation == null) {
            return EVMReturn.unknownOpCode(frame.contract.code[frame.pc]).also { frame.result = it }
        }
        val memSize = operation.memorySize?.invoke(frame) ?: 0
        frame.gas -= operation.gas
        frame.gas -= operation.dynamicGas?.invoke(frame, memSize) ?: 0
        if (frame.memory.size < memSize) {
            frame.memory = frame.memory.copyOf(memSize)
        }
        if (frame.gas < 0) {
            return EVMReturn.outOfGas().also { frame.result = it }
        }
        operation.execute(frame)
        frame.pc++
        return frame.result
    }

    class Delegate(set: InstructionSet, private val delegate: EVMInterpreterDelegate) : EVMDefaultInterpreter(set) {
        override suspend fun execute(frame: FrameContext) = delegate.execute(frame) { super.execute(it) }
        override suspend fun execute(
            frame: FrameContext,
            operation: Operation?
        ) = delegate.execute(frame, operation) { f, o -> super.execute(f, o) }
    }
}

class EVMConsoleLogger : EVMInterpreterDelegate {
    private var index = 0

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun execute(
        frame: FrameContext,
        operation: Operation?,
        execute: suspend (FrameContext, Operation?) -> EVMReturn?
    ): EVMReturn? {
        index++
        if (operation != null) {
            val beforeGas = frame.gas
            var prefix = "$index\t${frame.pc}\t${frame.gas}\t${operation.opCode}"
            if (operation.opCode.name.startsWith("SLOAD")) {
                prefix += "(0x${frame.contract.address.bytes.toHexString()}, ${frame.stack.last()})"
            }
            println(prefix)
        }
        return execute(frame, operation)
    }
}