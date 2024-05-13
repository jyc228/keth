package io.github.jyc228.ethereum.vm

import io.github.jyc228.ethereum.state.StateDatabase
import io.github.jyc228.ethereum.state.account.Address
import java.math.BigInteger
import java.nio.ByteBuffer

class EVMContext(
    val block: BlockContext,
    val db: StateDatabase,
)

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
    private val logs = mutableListOf<Log>()

    fun addLog(address: Address, topics: List<ByteArray>, data: ByteArray, blockNumber: ULong) {
        logs += Log(address, topics, data, blockNumber)
    }

    private class Log(
        val address: Address,
        val topics: List<ByteArray>,
        val data: ByteArray,
        val blockNumber: ULong
    )
}

class FrameContext(
    val caller: Address,
    val callValue: BigInteger,
    val callData: ByteArray,
    val contract: EVMContract,
    var gas: Int,
) {
    lateinit var transaction: TransactionContext
    lateinit var interpreter: EVMInterpreter
    private lateinit var vm: EVMContext

    val db get() = vm.db
    val block get() = vm.block

    var depth = 0
    var pc: Int = 0
    var memory: ByteArray = ByteArray(0)
    var memoryLastGasCost: Long = 0
    val stack: EVMStack = EVMStack()
    var nextFrameGas = 0
    var result: EVMReturn? = null
    var nextFrame: FrameContext? = null

    fun with(vm: EVMContext? = null, transaction: TransactionContext? = null): FrameContext {
        if (vm != null) this.vm = vm
        if (transaction != null) this.transaction = transaction
        return this
    }

    suspend fun nextFrame(newFrame: suspend () -> FrameContext): EVMReturn {
        val nextFrame = newFrame().with(vm, transaction).also { this.nextFrame = it }
        nextFrame.depth = this.depth + 1
        return interpreter.execute(nextFrame).apply { gas += nextFrame.gas }
    }

    fun addLog(topics: List<ByteArray>, data: ByteArray) {
        transaction.addLog(contract.address, topics, data, block.number)
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

    fun ByteArray.read(offset: Int, size: Int): ByteArray = copyOfRange(offset, offset + size)
    fun ByteArray.write(offset: Int, bytes: ByteArray): ByteArray =
        bytes.copyInto(destination = this, destinationOffset = offset)

    fun ByteArray.write(offset: Int, length: Int, bytes: ByteArray?): ByteArray =
        (bytes ?: ByteArray(length)).copyInto(destination = this, destinationOffset = offset)

    fun ByteArray.sliceArrayLast(n: Int): ByteArray = when (size.compareTo(n)) {
        -1 -> ByteBuffer.allocate(n).position(n - size).put(this).array()
        0 -> this
        1 -> copyOfRange(size - n, size)
        else -> error("")
    }

    fun ByteArray.toElement() = EVMStackElement(_bytes = this)
    fun BigInteger.toElement() = EVMStackElement(_big = this)
    fun Int.toElement() = EVMStackElement(_int = this)
    fun ULong.toElement() = EVMStackElement(_big = toLong().toBigInteger())
    fun Address.toElement() = EVMStackElement(bytes)

    fun EVMStackElement.toAddress() = Address(bytes.sliceArrayLast(20))

    companion object {
        val MAX_UINT64 = 18446744073709551615uL
    }
}