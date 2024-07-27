package com.github.jyc228.keth.vm

import com.github.jyc228.keth.fork.HardForkManager
import com.github.jyc228.keth.state.StateDatabase
import com.github.jyc228.keth.type.Address
import com.github.jyc228.keth.type.BlockHeader
import com.github.jyc228.keth.type.HexBigInt
import com.github.jyc228.keth.type.HexData
import com.github.jyc228.keth.type.HexInt
import com.github.jyc228.keth.type.Log
import com.github.jyc228.keth.type.Transaction
import com.github.jyc228.keth.type.TransactionReceipt
import com.github.jyc228.keth.type.TransactionStatus
import com.github.jyc228.keth.vm.interpreter.EVMInterpreter
import com.github.jyc228.keth.vm.interpreter.EVMInterpreterDelegate
import java.math.BigInteger
import kotlin.math.min

class EVM(
    private val findHeader: suspend (ULong) -> BlockHeader,
    private val createDatabase: suspend (BlockHeader) -> StateDatabase,
    private val hardForkManager: HardForkManager,
) {
    @OptIn(ExperimentalStdlibApi::class)
    suspend fun execute(transaction: Transaction, delegate: EVMInterpreterDelegate? = null): TransactionReceipt {
        val header = findHeader(transaction.blockNumber.number - 1u)
        val db = createDatabase(header)

        val config = EVMConfig.fromHardFork(hardForkManager.findFork(header.number.number))
        val interpreter = EVMInterpreter.of(InstructionSet.fromConfig(config), delegate)

        val frame = when (val to = transaction.to) {
            null -> EVMFrame(
                contract = db.withAccountOrThrow(Address.fromHexString(transaction.from.hex)) {
                    val newContractAddress = Address.generate(it.address, it.nonce)
                    EVMContract(newContractAddress, transaction.input.removePrefix("0x").hexToByteArray())
                },
                callData = byteArrayOf(),
                caller = Address.fromHexString(transaction.from.hex),
                callValue = transaction.value.number,
                remainGas = intrinsicGas(transaction, config)
            ).with(db, context(header), context(transaction, config)).also { interpreter.execute(it, OpCode.CREATE) }

            else -> EVMFrame(
                contract = db.withAccountOrThrow(Address.fromHexString(to.hex), EVMContract::of),
                callData = transaction.input.removePrefix("0x").hexToByteArray(),
                caller = Address.fromHexString(transaction.from.hex),
                callValue = transaction.value.number,
                remainGas = intrinsicGas(transaction, config)
            ).with(db, context(header), context(transaction, config)).also { interpreter.execute(it) }
        }
        return receipt(transaction, frame)
    }

    private fun context(header: BlockHeader) = EVMFrame.BlockContext(
        number = header.number.number,
        difficulty = header.totalDifficulty?.number?.toLong()?.toULong() ?: 0uL,
        time = header.timestamp.epochSeconds.toULong() + 2u,
        gasLimit = header.gasLimit.number,
        random = header.mixHash.hex.removePrefix("0x").toByteArray(),
        coinbase = Address.fromHexString(header.miner?.hex ?: "0x"),
        baseFee = header.baseFeePerGas?.number ?: BigInteger.ZERO
    )

    @OptIn(ExperimentalStdlibApi::class)
    private fun context(transaction: Transaction, config: EVMConfig) = EVMFrame.TransactionContext(
        Address.fromHexString(transaction.from.hex),
        Address.fromHexString(requireNotNull(transaction.to).hex),
        requireNotNull(transaction.gasPrice?.number),
        transaction.value.number,
        when (config.eip2929) {
            true -> AccessList().also { accessList ->
                accessList += Address.fromHexString(transaction.from.hex)
                if (transaction.to != null) accessList += Address.fromHexString(transaction.to!!.hex)
                transaction.accessList.forEach {
                    accessList[Address.fromHexString(it.address.hex)] += it.storageKeys.map { k ->
                        AccessList.Slot(k.hex.hexToByteArray())
                    }
                }
            }

            false -> null
        }
    )

    @OptIn(ExperimentalStdlibApi::class)
    private fun intrinsicGas(tx: Transaction, config: EVMConfig): Int {
        var gas = if (tx.to == null && config.eip2) 53000 else 21000
        val calldata = tx.input.removePrefix("0x").hexToByteArray()
        if (calldata.isNotEmpty()) {
            val nonZeroCount = calldata.count { it != 0.toByte() }
            val nonZeroGas = if (config.eip2028) 16 else 68
            gas += nonZeroCount * nonZeroGas
            val zeroCount = calldata.size - nonZeroCount
            gas += zeroCount * 4
            if (tx.to == null && config.eip3860) {
                gas += (calldata.size + 31) / 32 * 2
            }
        }
        if (tx.accessList.isNotEmpty()) {
            gas += tx.accessList.size * 2400
            gas += tx.accessList.flatMap { it.storageKeys }.size * 1900
        }
        return tx.gas.number.toInt() - gas
    }

    private suspend fun receipt(transaction: Transaction, frame: EVMFrame): TransactionReceipt {
        frame.remainGas += min(
            transaction.gas.number.toInt() - frame.remainGas / if (frame.vmConfig.eip3529) 5 else 2,
            frame.refundGas
        )
        frame.db.applyAccount(Address.fromHexString(transaction.from.hex)) {
            it.balance += frame.remainGas.toBigInteger() * requireNotNull(transaction.gasPrice).number
        }
        return TransactionReceipt(
            transactionHash = transaction.hash,
            transactionIndex = transaction.transactionIndex,
            blockHash = transaction.blockHash,
            blockNumber = transaction.blockNumber,
            from = transaction.from,
            to = transaction.to,
            effectiveGasPrice = transaction.gasPrice,
            cumulativeGasUsed = transaction.gas - HexBigInt(frame.remainGas.toBigInteger()), // todo
            gasUsed = transaction.gas - HexBigInt(frame.remainGas.toBigInteger()),
            contractAddress = when (transaction.to) {
                null -> frame.contract.address
                else -> null
            },
            status = if (frame.result?.err == null) TransactionStatus.Success else TransactionStatus.Fail,
            type = transaction.type,
            logs = frame.transaction.logs.mapIndexed { i, log ->
                Log(
                    removed = false,
                    logIndex = HexInt(number = i),
                    transactionIndex = transaction.transactionIndex,
                    transactionHash = transaction.hash,
                    blockHash = transaction.blockHash,
                    blockNumber = transaction.blockNumber,
                    address = log.address,
                    data = log.data?.let(HexData::fromByteArray) ?: HexData(""),
                    topics = log.topics.map { HexData.fromByteArray(it.copyInto(ByteArray(32), 32 - it.size)) }
                )
            }
        )
    }
}