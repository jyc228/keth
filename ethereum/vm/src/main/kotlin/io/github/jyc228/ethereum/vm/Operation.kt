package io.github.jyc228.ethereum.vm

class Operation(
    val opCode: OpCode,
    val minStack: Int,
    val gas: Int,
    val dynamicGas: (suspend FrameContext.() -> Int)?,
    val memorySize: (FrameContext.() -> Int)?,
    val execute: suspend FrameContext.() -> Unit
) {
    val maxStack: Int = 0

    override fun toString(): String = opCode.toString()
}

class OperationBuilder {
    private var minStack: Int = 0
    private var defaultGas: Int = 0
    private var additionalGas: (suspend FrameContext.() -> Int)? = null
    private var memorySize: (FrameContext.() -> Int)? = null
    private var execute: (suspend FrameContext.() -> Unit)? = null

    fun gas2(): OperationBuilder = gas(2)
    fun gas3(): OperationBuilder = gas(3)
    fun gas5(): OperationBuilder = gas(5)
    fun gas8(): OperationBuilder = gas(8)
    fun gas(gas: Int): OperationBuilder = apply { this.defaultGas = gas }

    fun execute(execute: suspend FrameContext.() -> Unit) = apply { this.execute = execute }
    fun memorySize(execute: FrameContext.() -> Int) = apply { this.memorySize = execute }
    fun additionalGas(execute: suspend FrameContext.() -> Int) = apply { this.additionalGas = execute }

    fun pop0(execute: suspend FrameContext.() -> Unit) = execute(execute)

    inline fun pop1(
        crossinline execute: suspend FrameContext.(top0: EVMStackElement) -> Unit
    ) = execute {
        val top0 = stack.pop()
        execute(top0)
    }

    inline fun pop2(
        crossinline execute: suspend FrameContext.(top1: EVMStackElement, top0: EVMStackElement) -> Unit
    ) = execute {
        val top0 = stack.pop()
        val top1 = stack.pop()
        execute(top1, top0)
    }

    inline fun pop3(
        crossinline execute: suspend FrameContext.(top2: EVMStackElement, top1: EVMStackElement, top0: EVMStackElement) -> Unit
    ) = execute {
        val top0 = stack.pop()
        val top1 = stack.pop()
        val top2 = stack.pop()
        execute(top2, top1, top0)
    }

    inline fun push(crossinline execute: suspend FrameContext.() -> EVMStackElement) =
        execute { stack.push(execute()) }

    inline fun pop1push(
        crossinline execute: suspend FrameContext.(top0: EVMStackElement) -> EVMStackElement
    ) = execute {
        val top0 = stack.pop()
        val result = execute(top0)
        stack.push(result)
    }

    inline fun pop2push(
        crossinline execute: suspend FrameContext.(top1: EVMStackElement, top0: EVMStackElement) -> EVMStackElement
    ) = execute {
        val top0 = stack.pop()
        val top1 = stack.pop()
        val result = execute(top1, top0)
        stack.push(result)
    }

    inline fun pop3push(
        crossinline execute: suspend FrameContext.(top2: EVMStackElement, top1: EVMStackElement, top0: EVMStackElement) -> EVMStackElement
    ) = execute {
        val top0 = stack.pop()
        val top1 = stack.pop()
        val top2 = stack.pop()
        val result = execute(top2, top1, top0)
        stack.push(result)
    }

    inline fun pop6push(
        crossinline execute: suspend FrameContext.(top5: EVMStackElement, top4: EVMStackElement, top3: EVMStackElement, top2: EVMStackElement, top1: EVMStackElement, top0: EVMStackElement) -> EVMStackElement
    ) = execute {
        val top0 = stack.pop()
        val top1 = stack.pop()
        val top2 = stack.pop()
        val top3 = stack.pop()
        val top4 = stack.pop()
        val top5 = stack.pop()
        val result = execute(top5, top4, top3, top2, top1, top0)
        stack.push(result)
    }

    inline fun pop7push(
        crossinline execute: suspend FrameContext.(top6: EVMStackElement, top5: EVMStackElement, top4: EVMStackElement, top3: EVMStackElement, top2: EVMStackElement, top1: EVMStackElement, top0: EVMStackElement) -> EVMStackElement
    ) = execute {
        val top0 = stack.pop()
        val top1 = stack.pop()
        val top2 = stack.pop()
        val top3 = stack.pop()
        val top4 = stack.pop()
        val top5 = stack.pop()
        val top6 = stack.pop()
        val result = execute(top6, top5, top4, top3, top2, top1, top0)
        stack.push(result)
    }

    fun build(opCode: OpCode): Operation? {
        return Operation(
            opCode = opCode,
            minStack = minStack,
            gas = defaultGas,
            dynamicGas = additionalGas,
            memorySize = memorySize,
            execute = execute ?: return null
        )
    }
}
