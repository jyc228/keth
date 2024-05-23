package io.github.jyc228.ethereum.vm

import io.github.jyc228.ethereum.state.StateDatabase
import io.github.jyc228.ethereum.state.account.Address
import java.math.BigInteger

class EVMFrame(
    val caller: Address,
    val callValue: BigInteger,
    val callData: ByteArray,
    val contract: EVMContract,
    var gas: Int,
) {
    lateinit var db: StateDatabase
    lateinit var block: BlockContext
    lateinit var transaction: TransactionContext
    lateinit var interpreter: EVMInterpreter

    var depth = 0
    var pc: Int = 0
    var memory: ByteArray = ByteArray(0)
    var memorySize: Int = 0
    var memoryLastGasCost: Long = 0
    val stack: EVMStack = EVMStack()
    var nextFrameGas = 0
    var result: EVMReturn? = null
    var nextFrame: EVMFrame? = null

    fun with(
        db: StateDatabase? = null,
        block: BlockContext? = null,
        transaction: TransactionContext? = null
    ): EVMFrame {
        if (db != null) this.db = db
        if (block != null) this.block = block
        if (transaction != null) this.transaction = transaction
        return this
    }

    suspend fun nextFrame(newFrame: suspend () -> EVMFrame): EVMReturn {
        val nextFrame = newFrame().with(db, block, transaction).also { this.nextFrame = it }
        nextFrame.depth = this.depth + 1
        return interpreter.execute(nextFrame).apply { gas += nextFrame.gas }
    }

    fun memoryGasCost(newMemorySize: Int): Int {
        val newMemSizeWords = newMemorySize.wordSize.toInt()
        if (newMemSizeWords * 32 > memory.size) {
            val square = newMemSizeWords * newMemSizeWords
            val linCoef = newMemSizeWords * 3
            val quadCoef = square / 512
            val newTotalFee = linCoef + quadCoef
            val fee = newTotalFee - memoryLastGasCost
            memoryLastGasCost = newTotalFee.toLong()
            return fee.toInt()
        }
        return 0
    }

    fun memoryCopyGas(stackPos: Int, newMemorySize: Int): Int {
        val gas = memoryGasCost(newMemorySize)
        val length = stack.back(stackPos).int
        return gas + (length.wordSize.toInt() * 3)
    }

    fun callGas(memorySize: Int): Int {
        val address = stack.back(1)
        val coldAccess = false // todo
//            coldCost := params.ColdAccountAccessCostEIP2929 - params.WarmStorageReadCostEIP2929
        val coldCost = 2600 - 100
        if (coldAccess) {
            gas -= coldCost
        }
        //
        val base = memoryGasCost(memorySize)
        val eip150 = true
        nextFrameGas = if (eip150) {
            val availableGas = gas - base
            availableGas - availableGas / 64
        } else {
            stack.back(0).int
        }
        val nextGas = base + nextFrameGas
        //
        if (coldAccess) {
            gas += coldCost
            return nextGas + coldCost
        }
        return nextGas
    }

    val Number.wordSize: ULong
        get() {
            val self = this.toLong().toULong()
            if (self > MAX_UINT64 - 31uL) return MAX_UINT64 / 32uL + 1uL
            return (self + 31uL) / 32uL
        }

    fun createLog(topics: List<ByteArray>, data: ByteArray) = Log(contract.address, topics, data, block.number)

    class BlockContext(
        val number: ULong = 0uL,
        val difficulty: ULong = 0uL,
        val time: ULong = 0uL,
        val gasLimit: BigInteger = BigInteger.ZERO,
        val random: ByteArray = byteArrayOf(),
        val coinbase: Address = Address(byteArrayOf()),
        val baseFee: BigInteger = BigInteger.ZERO
    )

    class TransactionContext(
        val from: Address,
        val to: Address,
        val gasPrice: BigInteger = 0.toBigInteger(),
        val value: BigInteger = 0.toBigInteger(),
    ) {
        val logs = mutableListOf<Log>()
    }

    class Log(
        val address: Address,
        val topics: List<ByteArray>,
        val data: ByteArray,
        val blockNumber: ULong
    )

    companion object {
        val MAX_UINT64 = 18446744073709551615uL
    }
}