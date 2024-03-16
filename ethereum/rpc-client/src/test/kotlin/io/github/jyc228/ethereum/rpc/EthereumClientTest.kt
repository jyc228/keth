package io.github.jyc228.ethereum.rpc

suspend fun main() {
    val client = EthereumClient.sepolia()
    val bn = client.eth.blockNumber().awaitOrThrow()
    val fullBlocks = client.batch { eth.getFullBlocks((bn.number - 4uL)..bn.number) }.awaitAllOrThrow()
    val simpleBlocks = client.batch { eth.getSimpleBlocks((bn.number - 4uL)..bn.number) }.awaitAllOrThrow()
    println(fullBlocks)
    println(simpleBlocks)
}
