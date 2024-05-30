package io.github.jyc228.ethereum.vm

class Operation(
    val opCode: OpCode,
    val minStack: Int,
    val gas: Int,
    val extraGas: (suspend EVMFrame.(EVMStack) -> Int)?,
    val memorySize: (EVMFrame.(EVMStack) -> Int)?,
    val execute: suspend EVMFrame.() -> Unit
) {
    val maxStack: Int = 0

    override fun toString(): String = opCode.toString()
}

private typealias E = EVMStackElement

class OperationBuilder(val vmConfig: EVMConfig) {
    private var minStack: Int = 0
    private var gas: Int = 0
    private var extraGas: (suspend EVMFrame.(EVMStack) -> Int)? = null
    private var memorySize: (EVMFrame.(EVMStack) -> Int)? = null
    private var execute: (suspend EVMFrame.() -> Unit)? = null

    fun gas2(): OperationBuilder = gas(2)
    fun gas3(): OperationBuilder = gas(3)
    fun gas5(): OperationBuilder = gas(5)
    fun gas8(): OperationBuilder = gas(8)
    fun gas(gas: Int): OperationBuilder = apply { this.gas = gas }
    fun extraGas(execute: suspend EVMFrame.(EVMStack) -> Int) = apply { this.extraGas = execute }
    fun memorySize(execute: EVMFrame.(EVMStack) -> Int) = apply { this.memorySize = execute }

    fun pop0(execute: suspend EVMFrame.() -> Unit) = execute(execute)

    inline fun pop1(
        crossinline execute: suspend EVMFrame.(E) -> Unit
    ) = execute { execute(stack.pop()) }

    inline fun pop2(
        crossinline execute: suspend EVMFrame.(E, E) -> Unit
    ) = execute { execute(stack.pop(), stack.pop()) }

    inline fun pop3(
        crossinline execute: suspend EVMFrame.(E, E, E) -> Unit
    ) = execute { execute(stack.pop(), stack.pop(), stack.pop()) }

    inline fun push(crossinline execute: suspend EVMFrame.() -> E) = execute { stack.push(execute()) }

    inline fun pop1push(
        crossinline execute: suspend EVMFrame.(E) -> E
    ) = execute {
        val result = execute(stack.pop())
        stack.push(result)
    }

    inline fun pop2push(
        crossinline execute: suspend EVMFrame.(E, E) -> E
    ) = execute {
        val result = execute(stack.pop(), stack.pop())
        stack.push(result)
    }

    inline fun pop3push(
        crossinline execute: suspend EVMFrame.(E, E, E) -> E
    ) = execute {
        val result = execute(stack.pop(), stack.pop(), stack.pop())
        stack.push(result)
    }

    inline fun pop4push(
        crossinline execute: suspend EVMFrame.(E, E, E, E) -> EVMStackElement
    ) = execute {
        val result = execute(stack.pop(), stack.pop(), stack.pop(), stack.pop())
        stack.push(result)
    }

    inline fun pop6push(
        crossinline execute: suspend EVMFrame.(E, E, E, E, E, E) -> E
    ) = execute {
        val result = execute(stack.pop(), stack.pop(), stack.pop(), stack.pop(), stack.pop(), stack.pop())
        stack.push(result)
    }

    inline fun pop7push(
        crossinline execute: suspend EVMFrame.(E, E, E, E, E, E, E) -> E
    ) = execute {
        val result = execute(stack.pop(), stack.pop(), stack.pop(), stack.pop(), stack.pop(), stack.pop(), stack.pop())
        stack.push(result)
    }

    fun execute(execute: suspend EVMFrame.() -> Unit) = apply { this.execute = execute }

    fun build(opCode: OpCode): Operation? {
        return Operation(
            opCode = opCode,
            minStack = minStack,
            gas = gas,
            extraGas = extraGas,
            memorySize = memorySize,
            execute = execute ?: return null
        )
    }
}
