package io.github.jyc228.ethereum.vm

import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.rpc.EthereumClient
import io.github.jyc228.ethereum.rpc.fromRpcUrl
import io.github.jyc228.ethereum.state.OffchainStateDatabase
import io.github.jyc228.ethereum.vm.interpreter.EVMStructLogger
import io.github.jyc228.ethereum.vm.interpreter.StructLog
import io.github.jyc228.jsonrpc.JsonRpcRequest
import io.github.jyc228.jsonrpc.KtorJsonRpcClient
import io.github.jyc228.keth.fork.HardForkManager
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.resource.resourceAsString
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

class EVMFixtureTest : DescribeSpec({
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
    val root = File(requireNotNull(EVMFixtureTest::class.java.getResource("/")).toURI())
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

private suspend fun simulateTransaction(txHash: Hash, client: EthereumClient, expected: List<StructLog>) {
    val logger = EVMStructLogger(enableStorage = true)
    val evm = EVM(
        { client.eth.getHeaderByNumber(it.blockNumber.number - 1u).awaitOrThrow() },
        { OffchainStateDatabase(it.hash, client) },
        HardForkManager.fromNetworkName("mainnet")
    )
    evm.execute(client.eth.getTransactionByHash(txHash).awaitOrThrow()!!, logger)
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
