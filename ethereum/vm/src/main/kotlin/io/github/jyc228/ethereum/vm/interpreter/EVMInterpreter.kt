package io.github.jyc228.ethereum.vm.interpreter

import io.github.jyc228.ethereum.vm.EVMFrame
import io.github.jyc228.ethereum.vm.EVMReturn
import io.github.jyc228.ethereum.vm.InstructionSet
import io.github.jyc228.ethereum.vm.OpCode
import io.github.jyc228.ethereum.vm.Operation
import io.github.jyc228.ethereum.vm.wordSize

open class EVMInterpreter(private val instructionSet: InstructionSet) {
    open suspend fun execute(frame: EVMFrame): EVMReturn {
        frame.interpreter = this
        while (true) return execute(frame, instructionSet[frame.contract.code[frame.pc]]) ?: continue
    }

    suspend fun execute(frame: EVMFrame, opCode: OpCode) = execute(frame, instructionSet[opCode.v])

    protected open suspend fun execute(frame: EVMFrame, operation: Operation?): EVMReturn? {
        if (operation == null) {
            return EVMReturn.unknownOpCode(frame.contract.code[frame.pc]).also { frame.result = it }
        }
        frame.operation = operation
        frame.memorySize = operation.memorySize?.invoke(frame, frame.stack)?.wordSize?.times(32) ?: 0
        frame.remainGas -= operation.gas
        frame.remainGas -= operation.extraGas?.invoke(frame, frame.stack) ?: 0
        if (frame.memory.size < frame.memorySize) {
            frame.memory = frame.memory.copyOf(frame.memorySize)
        }
        if (frame.remainGas < 0) {
            return EVMReturn.outOfGas().also { frame.result = it }
        }
        operation.execute(frame, frame.stack)
        frame.pc++
        return frame.result
    }

    class Delegate(set: InstructionSet, private val delegate: EVMInterpreterDelegate) : EVMInterpreter(set) {
        override suspend fun execute(frame: EVMFrame) = delegate.execute(frame) { super.execute(it) }
        override suspend fun execute(
            frame: EVMFrame,
            operation: Operation?
        ) = delegate.execute(frame, operation) { f, o -> super.execute(f, o) }
    }

    companion object {
        fun of(set: InstructionSet, delegate: EVMInterpreterDelegate? = null): EVMInterpreter {
            if (delegate == null) return EVMInterpreter(set)
            return Delegate(set, delegate)
        }
    }
}

