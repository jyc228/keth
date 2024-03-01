package ethereum.evm

import java.math.BigInteger
import java.util.Queue

class ScopeContext(val stack: Stack)

class Stack(val data: Queue<BigInteger> = java.util.ArrayDeque()) : Queue<BigInteger> by data

class EVMInterpreter() {
    fun run(contract: ContractReference, input: ByteArray?, readOnly: Boolean): Result<Unit> {
        val stack = Stack()
        val context = ScopeContext(stack)
        var pc = 0
        while (true) {
            val op = contract.getOpCode(pc)
//            if (stack.data.size < op.minStack) {
//                error("underflow")
//            }
//            if (op.maxStack < stack.data.size) {
//                error("overflow")
//            }
//            if (!contract.useGas(op.constantGas)) {
//                error("outofgas")
//            }
//            if (op.dynamicGas != null) {
//                if (op.memorySize != null) {
//                    val (size, overflow) = op.memorySize!!(stack)
//                    if (overflow) {
//                        error("ErrGasUintOverflow")
//                    }
//                }
//                val gas = op.dynamicGas!!()
//            }
//            op.execute(pc, this, context)
//            pc++
        }
    }
}