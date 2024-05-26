package io.github.jyc228.ethereum.vm

import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.Transaction
import io.github.jyc228.ethereum.rpc.EthereumClient
import io.github.jyc228.ethereum.rpc.fromRpcUrl
import io.github.jyc228.ethereum.state.OffchainStateDatabase
import io.github.jyc228.ethereum.state.account.Address
import io.github.jyc228.ethereum.vm.log.EVMStructLogger
import io.github.jyc228.ethereum.vm.log.StructLog
import io.github.jyc228.jsonrpc.JsonRpcRequest
import io.github.jyc228.jsonrpc.KtorJsonRpcClient
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.resource.resourceAsString
import java.io.File
import java.math.BigInteger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

class EVMInterpreterFixtureTest : DescribeSpec({
    val client = EthereumClient.fromRpcUrl("http://apne2c-mainnet-debug01.kroma.network:8545")

    xit("0x58eb9c19eb03a3c3db71eaa5887f3513a87bb29f9d39c89f459bf55356cd4434") {
        // https://kromascan.com/tx/0x58eb9c19eb03a3c3db71eaa5887f3513a87bb29f9d39c89f459bf55356cd4434
        val expected = readExpected(testCase.name.testName)
        simulateTransaction(Hash(testCase.name.testName), client, expected)
    }

    xit("0x875209b2b40a094196f4b032ba97c515efc94028778c47fddfe098d164fad747") {
        // https://kromascan.com/tx/0x875209b2b40a094196f4b032ba97c515efc94028778c47fddfe098d164fad747
        val expected = readExpected(testCase.name.testName)
        simulateTransaction(Hash(testCase.name.testName), client, expected)
    }
})

private suspend fun readExpected(txHash: String): List<StructLog> {
    val root = File(requireNotNull(EVMInterpreterFixtureTest::class.java.getResource("/")).toURI())
    val fixtureDir = File(root, "fixture")
    if (!fixtureDir.exists()) {
        fixtureDir.mkdirs()
    }

    val json = Json { ignoreUnknownKeys = true }
    val fixture = File(fixtureDir, txHash)
    if (!fixture.exists()) {
        println("generate fixture data from debug rpc..")
        val client = KtorJsonRpcClient("http://apne2c-mainnet-debug01.kroma.network:8545")
        val request = JsonRpcRequest(json.encodeToJsonElement(listOf(txHash)), "debug_traceTransaction", "1")
        val response = client.send(request)
        val structLogs = requireNotNull(response.result.jsonObject["structLogs"])
        fixture.writeText(json.encodeToString(structLogs))
        return json.decodeFromJsonElement<List<StructLog>>(structLogs)
    }
    val jsonString = resourceAsString("/fixture/$txHash")
    return json.decodeFromString<List<StructLog>>(jsonString)
}

@OptIn(ExperimentalStdlibApi::class)
private fun intrinsicGas(tx: Transaction, homestead: Boolean, eip2028: Boolean, eip3860: Boolean): Int {
    var gas = if (tx.to == null && homestead) {
        53000
    } else {
        21000
    }
    val calldata = tx.input.removePrefix("0x").hexToByteArray()
    if (calldata.isNotEmpty()) {
        val nonZeroCount = calldata.count { it != 0.toByte() }
        val nonZeroGas = when (eip2028) {
            true -> 16
            false -> 68
        }
        gas += nonZeroCount * nonZeroGas
        val zeroCount = calldata.size - nonZeroCount
        gas += zeroCount * 4
        if (tx.to == null && eip3860) {
            gas += (calldata.size + 31) / 32 * 2
        }
    }
    if (tx.accessList.isNotEmpty()) {
        gas += tx.accessList.size * 2400
        gas += tx.accessList.flatMap { it.storageKeys }.size * 1900
    }
    return tx.gas.number.toInt() - gas
}

@OptIn(ExperimentalStdlibApi::class)
private suspend fun simulateTransaction(txHash: Hash, client: EthereumClient, expected: List<StructLog>) {
    val tx = client.eth.getTransactionByHash(txHash).awaitOrThrow()!!
    val header = client.eth.getHeaderByNumber(tx.blockNumber.number - 1u).awaitOrThrow()
    val database = OffchainStateDatabase(header.hash, client)
    val context: suspend () -> EVMFrame = {
        EVMFrame(
            contract = database.withAccountOrThrow(
                Address.fromHexString(requireNotNull(tx.to).hex),
                EVMContract::of
            ),
            callData = tx.input.removePrefix("0x").hexToByteArray(),
            caller = Address.fromHexString(tx.from.hex),
            callValue = tx.value.number,
            remainGas = intrinsicGas(tx, true, true, true)
        ).with(
            database,
            EVMFrame.BlockContext(
                number = header.number.number,
                difficulty = header.totalDifficulty?.number?.toLong()?.toULong() ?: 0uL,
                time = header.timestamp.epochSeconds.toULong() + 2u,
                gasLimit = header.gasLimit.number,
                random = header.mixHash.hex.removePrefix("0x").toByteArray(),
                coinbase = Address.fromHexString(header.miner?.hex ?: "0x"),
                baseFee = header.baseFeePerGas?.number ?: BigInteger.ZERO
            ),
            EVMFrame.TransactionContext(
                Address.fromHexString(tx.from.hex),
                Address.fromHexString(requireNotNull(tx.to).hex),
                requireNotNull(tx.gasPrice?.number),
                tx.value.number
            )
        )
    }
    val logger = EVMStructLogger(enableStorage = true)
    runCatching {
        EVMDefaultInterpreter.Delegate(InstructionSet.all(), logger).execute(context())
    }.onFailure { it.printStackTrace() }
    logger.logs.asSequence().withIndex()
        .filter { (index, log) -> expected[index].copy(gas = 0) != log.copy(gas = 0) }
        .take(30)
        .forEach { (index, log) ->
            println(
                """
                    --------------- $index
                    from file   : ${expected.getOrNull(index - 1)}
                    from vm     : ${logger.logs.getOrNull(index - 1)}
                    
                    expected : ${expected[index]}
                    actual   : $log
                """.trimIndent()
            )
            val allKeys = expected[index].storage.keys + log.storage.keys
            val storageDiff = allKeys
                .filter { expected[index].storage[it] != log.storage[it] }
                .joinToString("\n") { "$it : ${expected[index].storage[it]} != ${log.storage[it]}" }
            if (storageDiff.isNotBlank()) {
                println("storage diff.. key : expected != actual")
                println(storageDiff)
            }
        }
}
