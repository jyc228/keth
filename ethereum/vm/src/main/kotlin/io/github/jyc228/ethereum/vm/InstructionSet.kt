package io.github.jyc228.ethereum.vm

class InstructionSet(private val self: Map<Byte, Operation>) : Map<Byte, Operation> by self {
    companion object {
        fun fromConfig(vmConfig: EVMConfig): InstructionSet {
            return OpCode.entries
                .mapNotNull { OperationBuilder(vmConfig).withOpCode(it).build(it) }
                .associateBy { it.opCode.v }
                .let(::InstructionSet)
        }
    }
}