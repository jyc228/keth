package io.github.jyc228.ethereum.abi

import io.github.jyc228.solidity.AbiInput
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import org.junit.jupiter.api.Test

internal class AbiTest {
    private val abi = GraalJsAbi.init()

    @Test
    fun decodeLogTest() {
        // mainnet l1 transaction 0x0aef838f174a0f9a8c19215c958f793824224c56905620d314b627af65832121
        val inputs = listOf(
            mapOf("type" to "uint256", "name" to "_batchIndex", "indexed" to true),
            mapOf("type" to "bytes32", "name" to "_batchRoot"),
            mapOf("type" to "uint256", "name" to "_batchSize"),
            mapOf("type" to "uint256", "name" to "_prevTotalElements"),
            mapOf("type" to "bytes", "name" to "_extraData")
        )

        val hex =
            "0x8c5b901f0037e84123ec2c8289ba4771b95052385ba97da5f39461c26ca0125e000000000000000000000000000000000000000000000000000000000000004d0000000000000000000000000000000000000000000000000000000001fbf85e00000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000000"

        val topics = listOf(
            "0x127186556e7be68c7e31263195225b4de02820707889540969f62c05cf73525e",
            "0x000000000000000000000000000000000000000000000000000000000003ad3b"
        )

        val result = abi.decodeLog(
            inputs.map {
                AbiInput(
                    name = it["name"].toString(),
                    type = it["type"].toString(),
                    indexed = it["indexed"]?.toString()?.toBoolean()
                )
            },
            hex,
            topics
        )

        result["_batchRoot"] shouldBeEqualIgnoringCase "0x8C5B901F0037E84123EC2C8289BA4771B95052385BA97DA5F39461C26CA0125E"
        result["_batchSize"] shouldBe "77"
        result["_prevTotalElements"] shouldBe "33290334"
        result["_extraData"] shouldBe null
    }


    @Test
    fun decodeParametersTest() {
        val hex =
            "0x015d8eb9000000000000000000000000000000000000000000000000000000000006cf4900000000000000000000000000000000000000000000000000000000639833b0000000000000000000000000000000000000000000000000000000000000000751c54ac1869a3f078ff0aa2994f27f6aceb5d2bda18ec30f2593fa20d9c052fa000000000000000000000000000000000000000000000000000000000000000000000000000000000000000016cb0a409497493c2ef7688f69534fd8f8f23b74000000000000000000000000000000000000000000000000000000000000083400000000000000000000000000000000000000000000000000000000000f4240"

        val result = abi.decodeParameters(
            // Solidity: function setL1BlockValues(uint64 _number, uint64 _timestamp, uint256 _basefee, bytes32 _hash, uint64 _sequenceNumber, bytes32 _batcherHash, uint256 _l1FeeOverhead, uint256 _l1FeeScalar) returns()
            listOf("uint64", "uint64", "uint256", "bytes32", "uint64", "bytes32", "uint256", "uint256"),
            hex,
        )

//        result[0].toString().toBigInteger().toString(16) shouldBe "6cf49"
//        result[1].toString().toBigInteger().toString(16) shouldBe "639833b0"
//        result[2].toString().toBigInteger().toString(16) shouldBe "7"
//        result[3] shouldBe "0x51c54ac1869a3f078ff0aa2994f27f6aceb5d2bda18ec30f2593fa20d9c052fa"
//        result[4].toString().toBigInteger().toString(16) shouldBe "0"
//        result[5] shouldBe "0x00000000000000000000000016cb0a409497493c2ef7688f69534fd8f8f23b74"
//        result[6].toString().toBigInteger().toString(16) shouldBe "834"
//        result[7].toString().toBigInteger().toString(16) shouldBe "f4240"

        // https://lab.miguelmota.com/ethereum-input-data-decoder/example/
        // language=json
        val abi =
            """[{"inputs":[],"stateMutability":"nonpayable","type":"constructor"},{"inputs":[],"name":"DEPOSITOR_ACCOUNT","outputs":[{"internalType":"address","name":"","type":"address"}],"stateMutability":"view","type":"function"},{"inputs":[],"name":"basefee","outputs":[{"internalType":"uint256","name":"","type":"uint256"}],"stateMutability":"view","type":"function"},{"inputs":[],"name":"batcherHash","outputs":[{"internalType":"bytes32","name":"","type":"bytes32"}],"stateMutability":"view","type":"function"},{"inputs":[],"name":"hash","outputs":[{"internalType":"bytes32","name":"","type":"bytes32"}],"stateMutability":"view","type":"function"},{"inputs":[],"name":"l1FeeOverhead","outputs":[{"internalType":"uint256","name":"","type":"uint256"}],"stateMutability":"view","type":"function"},{"inputs":[],"name":"l1FeeScalar","outputs":[{"internalType":"uint256","name":"","type":"uint256"}],"stateMutability":"view","type":"function"},{"inputs":[],"name":"number","outputs":[{"internalType":"uint64","name":"","type":"uint64"}],"stateMutability":"view","type":"function"},{"inputs":[],"name":"sequenceNumber","outputs":[{"internalType":"uint64","name":"","type":"uint64"}],"stateMutability":"view","type":"function"},{"inputs":[{"internalType":"uint64","name":"_number","type":"uint64"},{"internalType":"uint64","name":"_timestamp","type":"uint64"},{"internalType":"uint256","name":"_basefee","type":"uint256"},{"internalType":"bytes32","name":"_hash","type":"bytes32"},{"internalType":"uint64","name":"_sequenceNumber","type":"uint64"},{"internalType":"bytes32","name":"_batcherHash","type":"bytes32"},{"internalType":"uint256","name":"_l1FeeOverhead","type":"uint256"},{"internalType":"uint256","name":"_l1FeeScalar","type":"uint256"}],"name":"setL1BlockValues","outputs":[],"stateMutability":"nonpayable","type":"function"},{"inputs":[],"name":"timestamp","outputs":[{"internalType":"uint64","name":"","type":"uint64"}],"stateMutability":"view","type":"function"},{"inputs":[],"name":"version","outputs":[{"internalType":"string","name":"","type":"string"}],"stateMutability":"view","type":"function"}]"""
        // language=json
        val expectedOutput =
            """{"method":"setL1BlockValues","types":["uint64","uint64","uint256","bytes32","uint64","bytes32","uint256","uint256"],"inputs":[{"type":"BigNumber","hex":"0x06cf49"},{"type":"BigNumber","hex":"0x639833b0"},{"type":"BigNumber","hex":"0x07"},"0x51c54ac1869a3f078ff0aa2994f27f6aceb5d2bda18ec30f2593fa20d9c052fa",{"type":"BigNumber","hex":"0x00"},"0x00000000000000000000000016cb0a409497493c2ef7688f69534fd8f8f23b74",{"type":"BigNumber","hex":"0x0834"},{"type":"BigNumber","hex":"0x0f4240"}],"names":["_number","_timestamp","_basefee","_hash","_sequenceNumber","_batcherHash","_l1FeeOverhead","_l1FeeScalar"]}"""
    }
}
