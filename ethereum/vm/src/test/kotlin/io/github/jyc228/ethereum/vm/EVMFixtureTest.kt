package io.github.jyc228.ethereum.vm

import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.TransactionReceipt
import io.github.jyc228.ethereum.rpc.EthereumClient
import io.github.jyc228.ethereum.rpc.fromRpcUrl
import io.github.jyc228.ethereum.state.OffchainStateDatabase
import io.github.jyc228.ethereum.vm.interpreter.EVMStructLogger
import io.github.jyc228.ethereum.vm.interpreter.StructLog
import io.github.jyc228.jsonrpc.JsonRpcRequest
import io.github.jyc228.jsonrpc.KtorJsonRpcClient
import io.github.jyc228.keth.fork.HardForkManager
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.errorCollector
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.test.TestScope
import io.kotest.matchers.resource.resourceAsString
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

class EVMFixtureTest : DescribeSpec({

    context("ethereum") {
        this.testCase
        val client = EthereumClient.fromRpcUrl("") // todo
        val evm = EVM(
            { client.eth.getHeaderByNumber(it).awaitOrThrow() },
            { OffchainStateDatabase(it.hash, client) },
            HardForkManager.fromNetworkName("mainnet")
        )
    }

    context("kroma") {
        val client = EthereumClient.fromRpcUrl("https://api.kroma.network")
        val debugRpc = "http://apne2c-mainnet-debug01.kroma.network:8545"
        val evm = EVM(
            { client.eth.getHeaderByNumber(it).awaitOrThrow() },
            { OffchainStateDatabase(it.hash, client) },
            HardForkManager.fromNetworkName("kroma-mainnet")
        )

        it("0x875209b2b40a094196f4b032ba97c515efc94028778c47fddfe098d164fad747") { // https://kromascan.com/tx/0x875209b2b40a094196f4b032ba97c515efc94028778c47fddfe098d164fad747
            testTransaction(evm, client, debugRpc)
        }

        it("0x58eb9c19eb03a3c3db71eaa5887f3513a87bb29f9d39c89f459bf55356cd4434") { // https://kromascan.com/tx/0x58eb9c19eb03a3c3db71eaa5887f3513a87bb29f9d39c89f459bf55356cd4434
            testTransaction(evm, client, debugRpc)
        }
    }
})

private suspend fun TestScope.testTransaction(evm: EVM, client: EthereumClient, debugRpcUrl: String? = null) {
    val txHash = Hash(testCase.name.testName)
    val logger = EVMStructLogger(enableStorage = true)

    val actual = evm.execute(client.eth.getTransactionByHash(txHash).awaitOrThrow()!!, logger)
    val expected = client.eth.getTransactionReceipt(txHash).awaitOrThrow()!!

    assert(actual, expected) { if (debugRpcUrl != null) assert(logger, readExpected(txHash, debugRpcUrl)) }
}

private suspend fun assert(
    actual: TransactionReceipt,
    expected: TransactionReceipt,
    assert: (suspend () -> Unit)? = null
) = assertSoftly {
    actual.type shouldBe actual.type
    actual.transactionIndex.number shouldBe expected.transactionIndex.number
    actual.transactionHash shouldBe actual.transactionHash
    actual.gasUsed.number shouldBe expected.gasUsed.number
    actual.effectiveGasPrice?.number shouldBe expected.effectiveGasPrice?.number
//    actual.cumulativeGasUsed.number shouldBe expected.cumulativeGasUsed.number
    actual.logs.size shouldBe expected.logs.size
    actual.logs.zip(expected.logs).forEach { (actualLog, expectedLog) ->
        actualLog.data.hex shouldBe expectedLog.data.hex
        actualLog.topics.joinToString { it.hex } shouldBe expectedLog.topics.joinToString { it.hex }
    }
    if (errorCollector.errors().isNotEmpty()) assert?.invoke()
}

private fun assert(actual: EVMStructLogger, expected: List<StructLog>) {
    actual.logs.size shouldBe expected.size
    actual.logs.zip(expected).forEachIndexed { _, (actualLog, expectedLog) ->
        actualLog shouldBe expectedLog
    }
}

private suspend fun readExpected(txHash: Hash, debugRpcUrl: String): List<StructLog> {
    val root = File(requireNotNull(EVMFixtureTest::class.java.getResource("/")).toURI())
    val fixtureDir = File(root, "fixture")
    if (!fixtureDir.exists()) {
        fixtureDir.mkdirs()
    }

    val json = Json { ignoreUnknownKeys = true }
    val fixture = File(fixtureDir, txHash.hex)
    if (!fixture.exists()) {
        println("generate fixture data from debug rpc..")
        val client = KtorJsonRpcClient(debugRpcUrl)
        val request = JsonRpcRequest(json.encodeToJsonElement(listOf(txHash)), "debug_traceTransaction", "1")
        val response = client.send(request)
        val structLogs = requireNotNull(response.result.jsonObject["structLogs"])
        fixture.writeText(json.encodeToString(structLogs))
        return json.decodeFromJsonElement<List<StructLog>>(structLogs)
    }
    val jsonString = resourceAsString("/fixture/$txHash")
    return json.decodeFromString<List<StructLog>>(jsonString)
}
