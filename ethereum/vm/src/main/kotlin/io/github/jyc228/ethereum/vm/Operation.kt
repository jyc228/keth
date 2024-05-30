package io.github.jyc228.ethereum.vm

class Operation(
    val opCode: OpCode,
    val minStack: Int,
    val gas: Int,
    val extraGas: (suspend EVMFrame.(EVMStack) -> Int)?,
    val memorySize: (EVMFrame.(EVMStack) -> Int)?,
    val execute: suspend EVMFrame.(EVMStack) -> Unit
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
    private var execute: (suspend EVMFrame.(EVMStack) -> Unit)? = null

    fun gas2(): OperationBuilder = gas(2)
    fun gas3(): OperationBuilder = gas(3)
    fun gas5(): OperationBuilder = gas(5)
    fun gas8(): OperationBuilder = gas(8)
    fun gas(gas: Int): OperationBuilder = apply { this.gas = gas }
    fun extraGas(execute: suspend EVMFrame.(EVMStack) -> Int) = apply { this.extraGas = execute }

    fun memorySize(execute: EVMFrame.(EVMStack) -> Int) = apply { this.memorySize = execute }

    inline fun pop(crossinline execute: suspend EVMFrame.(EVMStack) -> Unit) = execute {
        stack.destructuringPopIndex = 0
        execute(stack)
        stack.destructuringPopIndex = -1
    }

    inline fun poppush(crossinline execute: suspend EVMFrame.(EVMStack) -> E) = execute {
        stack.destructuringPopIndex = 0
        stack.push(execute(stack))
        stack.destructuringPopIndex = -1
    }

    inline fun push(crossinline execute: suspend EVMFrame.() -> E) = execute { stack.push(execute()) }

    fun execute(execute: suspend EVMFrame.(EVMStack) -> Unit) = apply { this.execute = execute }

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
