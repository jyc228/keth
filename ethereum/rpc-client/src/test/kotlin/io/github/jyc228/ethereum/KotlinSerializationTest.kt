package io.github.jyc228.ethereum

import io.github.jyc228.ethereum.rpc.eth.Block
import io.github.jyc228.ethereum.rpc.eth.FullBlock
import io.github.jyc228.ethereum.rpc.eth.Log
import io.github.jyc228.ethereum.rpc.eth.SimpleBlock
import io.github.jyc228.ethereum.rpc.eth.Transaction
import io.kotest.core.spec.style.StringSpec
import kotlinx.serialization.json.Json

class KotlinSerializationTest : StringSpec({
    fun loadJsonFromFile(name: String): String {
        return ClassLoader.getSystemClassLoader().getResourceAsStream(name)?.bufferedReader()?.readText()
            ?: error("$name not exist")
    }


    "block with full tx" {
        val a = HexULong(0uL)
        val block = Json.decodeFromString<Block>(loadJsonFromFile("eth_block.json"))
        println(block)
    }

    "block with tx hashes" {
        val block = Json.decodeFromString<FullBlock>(loadJsonFromFile("eth_simple_block.json"))
        println(block)
    }

    "block with empty tx" {
        val block = Json.decodeFromString<SimpleBlock>(loadJsonFromFile("eth_block_empty_tx.json"))
        println(block)
    }

    "block" {
        val block = Json.decodeFromString<Block>(loadJsonFromFile("eth_simple_block.json"))
        println(block)
    }

    "transaction" {
        val tx = Json.decodeFromString<Transaction>(loadJsonFromFile("eth_transaction.json"))
        println(tx)
    }

    "log" {
        val r = Json.decodeFromString<Log>(loadJsonFromFile("eth_log.json"))
        println(r)
    }
})

