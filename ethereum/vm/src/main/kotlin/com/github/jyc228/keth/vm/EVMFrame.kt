package com.github.jyc228.keth.vm

import com.github.jyc228.keth.state.StateDatabase
import com.github.jyc228.keth.state.account.Address
import com.github.jyc228.keth.vm.interpreter.EVMInterpreter
import java.math.BigInteger

class EVMFrame(
    val caller: Address,
    val callValue: BigInteger,
    val callData: ByteArray,
    val contract: EVMContract,
    var remainGas: Int,
) {
    var refundGas: Int = 0
    var vmConfig: EVMConfig = EVMConfig()
    var chainId: Int = 0
    lateinit var db: StateDatabase
    lateinit var block: BlockContext
    lateinit var transaction: TransactionContext
    lateinit var interpreter: EVMInterpreter

    lateinit var operation: Operation
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
        nextFrame.refundGas = this.refundGas
        return interpreter.execute(nextFrame).apply {
            remainGas += nextFrame.remainGas
            refundGas = nextFrame.refundGas
        }
    }

    fun memorySize(offset: Int, size: Int): Int = if (size == 0) 0 else offset + size

    fun memoryGasCost(): Int {
        val newMemSizeWords = memorySize.wordSize
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

    suspend fun transferValueGas(address: Address, value: BigInteger): Int {
        return when (vmConfig.eip158) {
            true -> if (db.findAccount(address) == null && value > BigInteger.ZERO) 25000 else 0
            false -> if (db.findAccount(address) == null) 25000 else 0
        } + if (value > BigInteger.ZERO) 9000 else 0
    }

    fun callGas(callGas: Int, baseGas: Int): Int {
        nextFrameGas = callGas
        if (vmConfig.eip150) {
            val availableGas = remainGas - baseGas
            val gas = availableGas - availableGas / 64
            if (gas < callGas) nextFrameGas = gas
        }
        return baseGas + nextFrameGas
    }

    fun computeAccessAccountGas(address: Address): Int {
        if (address in (transaction.accessList!!)) return 0
        transaction.accessList!! += address
        return 2500
    }

    suspend fun computeAccessContractCallGas(address: Address, contractCallGas: suspend () -> Int): Int {
        if (address in (transaction.accessList!!)) return contractCallGas()
        transaction.accessList!! += address
        this.remainGas -= 2500
        val nextGas = contractCallGas()
        this.remainGas += 2500
        return nextGas + 2500
    }

    fun computeAccessSlotGas(contract: EVMContract, key: ByteArray, alreadyExistGas: Int = 0): Int {
        if (AccessList.Slot(key) in (transaction.accessList!![contract.address])) return alreadyExistGas
        transaction.accessList!![contract.address] += AccessList.Slot(stack.back(0).bytes)
        return 2100
    }

    fun createLog(topics: List<ByteArray>, data: ByteArray?) = Log(contract.address, topics, data, block.number)

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
        val accessList: AccessList? = null,
    ) {
        val logs = mutableListOf<Log>()
    }

    class Log(
        val address: Address,
        val topics: List<ByteArray>,
        val data: ByteArray?,
        val blockNumber: ULong
    )
}