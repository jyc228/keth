package io.github.jyc228.ethereum.abi

import io.github.jyc228.solidity.AbiItem
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import io.kotest.matchers.types.shouldBeInstanceOf
import java.math.BigInteger
import kotlinx.serialization.json.Json

@OptIn(ExperimentalStdlibApi::class)
internal class AbiTest : DescribeSpec({

    context("decodeLog") {
        it("TransactionBatchAppended") {
            val abi = // mainnet l1 transaction 0x0aef838f174a0f9a8c19215c958f793824224c56905620d314b627af65832121
                Json.decodeFromString<AbiItem>("""{"anonymous":false,"inputs":[{"indexed":true,"internalType":"uint256","name":"_batchIndex","type":"uint256"},{"indexed":false,"internalType":"bytes32","name":"_batchRoot","type":"bytes32"},{"indexed":false,"internalType":"uint256","name":"_batchSize","type":"uint256"},{"indexed":false,"internalType":"uint256","name":"_prevTotalElements","type":"uint256"},{"indexed":false,"internalType":"bytes","name":"_extraData","type":"bytes"}],"name":"TransactionBatchAppended","type":"event"}""")

            val hex =
                "0x8c5b901f0037e84123ec2c8289ba4771b95052385ba97da5f39461c26ca0125e000000000000000000000000000000000000000000000000000000000000004d0000000000000000000000000000000000000000000000000000000001fbf85e00000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000000"

            val topics = listOf(
                "0x127186556e7be68c7e31263195225b4de02820707889540969f62c05cf73525e",
                "0x000000000000000000000000000000000000000000000000000000000003ad3b"
            )

            val result = Abi.decodeLog(abi.inputs, hex, topics)

            result["_batchIndex"].shouldBeInstanceOf<BigInteger>() shouldBe 240955.toBigInteger()
            result["_batchRoot"].shouldBeInstanceOf<ByteArray>()
                .toHexString() shouldBeEqualIgnoringCase "8C5B901F0037E84123EC2C8289BA4771B95052385BA97DA5F39461C26CA0125E"
            result["_batchSize"].shouldBeInstanceOf<BigInteger>() shouldBe 77.toBigInteger()
            result["_prevTotalElements"].shouldBeInstanceOf<BigInteger>() shouldBe 33290334.toBigInteger()
            result["_extraData"].shouldBeInstanceOf<ByteArray>() shouldHaveSize 0
        }

        it("Transfer") {
            val abi =
                Json.decodeFromString<AbiItem>("""{"anonymous":false,"inputs":[{"indexed":true,"internalType":"address","name":"from","type":"address"},{"indexed":true,"internalType":"address","name":"to","type":"address"},{"indexed":false,"internalType":"uint256","name":"value","type":"uint256"}],"name":"Transfer","type":"event"}""")

            val hex =
                "0x00000000000000000000000000000000000000000000000000000000085a9fed"

            val topics = listOf(
                "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
                "0x0000000000000000000000009995287b63185310209478156318390715f2546e",
                "0x00000000000000000000000044638da736fed84089f616227ea9aefc033586eb",
            )

            val result = Abi.decodeLog(abi.inputs, hex, topics)

            result["from"].shouldBeInstanceOf<String>() shouldBeEqualIgnoringCase "9995287B63185310209478156318390715F2546e"
            result["to"].shouldBeInstanceOf<String>() shouldBeEqualIgnoringCase "44638Da736fEd84089F616227eA9aefc033586EB"
            result["value"].shouldBeInstanceOf<BigInteger>() shouldBe 140156909.toBigInteger()
        }
    }

    context("decodeParameters") {
        val hex =
            "0x015d8eb9000000000000000000000000000000000000000000000000000000000006cf4900000000000000000000000000000000000000000000000000000000639833b0000000000000000000000000000000000000000000000000000000000000000751c54ac1869a3f078ff0aa2994f27f6aceb5d2bda18ec30f2593fa20d9c052fa000000000000000000000000000000000000000000000000000000000000000000000000000000000000000016cb0a409497493c2ef7688f69534fd8f8f23b74000000000000000000000000000000000000000000000000000000000000083400000000000000000000000000000000000000000000000000000000000f4240"

        val result = Abi.decodeParameters(
            // Solidity: function setL1BlockValues(uint64 _number, uint64 _timestamp, uint256 _basefee, bytes32 _hash, uint64 _sequenceNumber, bytes32 _batcherHash, uint256 _l1FeeOverhead, uint256 _l1FeeScalar) returns()
            listOf("uint64", "uint64", "uint256", "bytes32", "uint64", "bytes32", "uint256", "uint256"),
            hex.drop(10),
        )

        println("..")

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
})
