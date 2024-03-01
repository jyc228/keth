package ethereum.evm

abstract class Instruction {
    companion object {
        fun fromOpCode(opCode: Byte): Instruction {
            error("...")
        }
    }

    object Create : Instruction()

    object Stop : Instruction()
}
