package ethereum.evm

import ethereum.collections.Hash
import io.github.jyc228.ethereum.state.StateDatabase
import io.github.jyc228.ethereum.state.account.Address
import java.math.BigInteger

class EVMContext(
    val block: BlockContext,
    val db: StateDatabase,
)

class BlockContext(
    val number: ULong = 0uL,
    val difficulty: ULong = 0uL,
    val time: ULong = 0uL,
    val gasLimit: BigInteger = BigInteger.ZERO,
    val random: Hash = Hash.EMPTY_TX_HASH,
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
    private lateinit var vm: EVMContext
    private lateinit var interpreter: EVMInterpreter

    val db get() = vm.db
    val block get() = vm.block

    var pc: Int = 0
    var stop: Boolean = false
    var memory: ByteArray = ByteArray(0)
    var memoryLastGasCost: Long = 0
    val stack: EVMStack = EVMStack()

    fun with(vm: EVMContext? = null, transaction: TransactionContext? = null): FrameContext {
        if (vm != null) this.vm = vm
        if (transaction != null) this.transaction = transaction
        return this
    }

    suspend fun nextFrame(newFrame: suspend () -> FrameContext): Result<Unit> {
        return newFrame().with(vm, transaction).execute(interpreter)
    }

    suspend fun execute(interpreter: EVMInterpreter): Result<Unit> {
        this.interpreter = interpreter
        return this.interpreter.execute(this)
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

    val Number.wordSize: ULong
        get() {
            val self = this.toLong().toULong()
            if (self > MAX_UINT64 - 31uL) return MAX_UINT64 / 32uL + 1uL
            return self + 31uL / 32uL
        }

    fun ByteArray.read(offset: Int, size: Int): ByteArray = copyOfRange(offset, offset + size)
    fun ByteArray.write(offset: Int, bytes: ByteArray): ByteArray =
        bytes.copyInto(destination = this, destinationOffset = offset)

    fun ByteArray.toElement() = EVMStackElement(_bytes = this)
    fun BigInteger.toElement() = EVMStackElement(_big = this)
    fun Int.toElement() = EVMStackElement(_int = this)
    fun ULong.toElement() = EVMStackElement(_big = toLong().toBigInteger())
    fun Address.toElement() = EVMStackElement(bytes)
    fun Hash.toElement() = EVMStackElement(bytes)

    companion object {
        val MAX_UINT64 = 18446744073709551615uL
    }
}