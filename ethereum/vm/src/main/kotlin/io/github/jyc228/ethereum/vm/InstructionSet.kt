package io.github.jyc228.ethereum.vm

class InstructionSet(private val self: Map<Byte, Operation>) : Map<Byte, Operation> by self {
    companion object {
        fun all() = InstructionSet(OpCode.entries.associate { it.v to newOperation(it) })
    }
}