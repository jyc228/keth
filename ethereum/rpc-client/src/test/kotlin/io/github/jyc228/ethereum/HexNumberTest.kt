package io.github.jyc228.ethereum

import io.github.jyc228.ethereum.rpc.EthereumClient
import io.github.jyc228.ethereum.rpc.fromRpcUrl
import io.kotest.core.spec.style.FunSpec

class HexNumberTest : FunSpec({
    context("hi") {
        val a = HexBigInt(10.toBigInteger())
        val b = HexBigInt(12309229348.toBigInteger())
        val c = a + b
        println(c)
    }

    context("aa") {
        val client = EthereumClient.fromRpcUrl("https://rpc.ankr.com/eth")
    }
})
