package io.github.jyc228.ethereum.vm

import io.github.jyc228.ethereum.BlockHeader
import io.github.jyc228.ethereum.Transaction
import io.github.jyc228.ethereum.state.StateDatabase
import io.github.jyc228.ethereum.state.account.Address
import io.github.jyc228.ethereum.vm.interpreter.EVMInterpreter
import io.github.jyc228.ethereum.vm.interpreter.EVMInterpreterDelegate
import java.math.BigInteger

class EVM(
    private val findHeader: suspend (Transaction) -> BlockHeader,
    private val createDatabase: suspend (BlockHeader) -> StateDatabase,
    private val config: EVMConfig = EVMConfig(),
    delegate: EVMInterpreterDelegate? = null
) {
    private val interpreter = EVMInterpreter.of(InstructionSet.fromConfig(config), delegate)

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun execute(transaction: Transaction) {
        val header = findHeader(transaction)
        val database = createDatabase(header)

        when (val to = transaction.to) {
            null -> EVMFrame(
                contract = database.withAccountOrThrow(Address.fromHexString(transaction.from.hex)) {
                    val newContractAddress = Address.new(it.address, it.nonce)
                    EVMContract(newContractAddress, transaction.input.removePrefix("0x").hexToByteArray())
                },
                callData = byteArrayOf(),
                caller = Address.fromHexString(transaction.from.hex),
                callValue = transaction.value.number,
                remainGas = intrinsicGas(transaction)
            ).with(database, context(header), context(transaction)).let { interpreter.execute(it, OpCode.CREATE) }

            else -> EVMFrame(
                contract = database.withAccountOrThrow(Address.fromHexString(to.hex), EVMContract::of),
                callData = transaction.input.removePrefix("0x").hexToByteArray(),
                caller = Address.fromHexString(transaction.from.hex),
                callValue = transaction.value.number,
                remainGas = intrinsicGas(transaction)
            ).with(database, context(header), context(transaction)).let { interpreter.execute(it) }
        }
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
    private fun context(transaction: Transaction) = EVMFrame.TransactionContext(
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
    private fun intrinsicGas(tx: Transaction): Int {
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
}