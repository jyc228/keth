package ethereum.evm

class Operation(
    val opCode: OpCode,
    val minStack: Int,
    val gas: Int,
    val dynamicGas: ((FrameContext) -> Int)?,
    val memorySize: ((FrameContext) -> Int)?,
    val execute: (FrameContext) -> Unit
) {
    val maxStack: Int = 0

    override fun toString(): String = opCode.toString()
}

class OperationBuilder(val opCode: OpCode) {
    var minStack: Int = 0
    var gas: Int = 0
    var dynamicGas: ((FrameContext) -> Int)? = null
    var memorySize: ((FrameContext) -> Int)? = null
    lateinit var execute: (FrameContext) -> Unit

    fun withGas2(): OperationBuilder = withGas(2)
    fun withGas3(): OperationBuilder = withGas(3)
    fun withGas5(): OperationBuilder = withGas(5)
    fun withGas8(): OperationBuilder = withGas(8)
    fun withGas(gas: Int): OperationBuilder = apply { this.gas = gas }

    fun pop0(execute: FrameContext.() -> Unit) = withExecute(execute)
    inline fun pop1(
        crossinline execute: FrameContext.(top0: EVMStackElement) -> Unit
    ) = withExecute {
        val top0 = stack.pop()
        execute(top0)
    }

    inline fun pop2(
        crossinline execute: FrameContext.(top1: EVMStackElement, top0: EVMStackElement) -> Unit
    ) = withExecute {
        val top0 = stack.pop()
        val top1 = stack.pop()
        execute(top1, top0)
    }

    inline fun pop3(
        crossinline execute: FrameContext.(top2: EVMStackElement, top1: EVMStackElement, top0: EVMStackElement) -> Unit
    ) = withExecute {
        val top0 = stack.pop()
        val top1 = stack.pop()
        val top2 = stack.pop()
        execute(top2, top1, top0)
    }

    inline fun push(crossinline execute: FrameContext.() -> EVMStackElement) = withExecute { stack.push(execute()) }

    inline fun pop1push(
        crossinline execute: FrameContext.(top0: EVMStackElement) -> EVMStackElement
    ) = withExecute {
        val top0 = stack.pop()
        val result = execute(top0)
        stack.push(result)
    }

    inline fun pop2push(
        crossinline execute: FrameContext.(top1: EVMStackElement, top0: EVMStackElement) -> EVMStackElement
    ) = withExecute {
        val top0 = stack.pop()
        val top1 = stack.pop()
        val result = execute(top1, top0)
        stack.push(result)
    }

    inline fun pop3push(
        crossinline execute: FrameContext.(top2: EVMStackElement, top1: EVMStackElement, top0: EVMStackElement) -> EVMStackElement
    ) = withExecute {
        val top0 = stack.pop()
        val top1 = stack.pop()
        val top2 = stack.pop()
        val result = execute(top2, top1, top0)
        stack.push(result)
    }

    fun withExecute(execute: FrameContext.() -> Unit) = apply { this.execute = execute }
    fun withMemorySize(execute: FrameContext.() -> Int) = apply { this.memorySize = execute }
    fun withDynamicGas(execute: FrameContext.() -> Int) = apply { this.dynamicGas = execute }

    companion object {
        fun build(opCode: OpCode, init: OperationBuilder.() -> Unit): Operation {
            val builder = OperationBuilder(opCode).apply(init)
            return Operation(
                opCode = builder.opCode,
                minStack = builder.minStack,
                gas = builder.gas,
                dynamicGas = builder.dynamicGas,
                memorySize = builder.memorySize,
                execute = builder.execute
            )
        }
    }
}

