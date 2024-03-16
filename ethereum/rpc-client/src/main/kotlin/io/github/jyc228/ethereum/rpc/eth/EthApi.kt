package io.github.jyc228.ethereum.rpc.eth

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.HexBigInt
import io.github.jyc228.ethereum.HexData
import io.github.jyc228.ethereum.HexULong
import io.github.jyc228.ethereum.contract.ContractEvent
import io.github.jyc228.ethereum.contract.ContractEventFactory
import io.github.jyc228.ethereum.rpc.RpcCall

@Suppress("UNCHECKED_CAST")
interface EthApi {
    suspend fun chainId(): RpcCall<HexULong>
    suspend fun gasPrice(): RpcCall<HexBigInt>
    suspend fun blockNumber(): RpcCall<HexULong>

    suspend fun getBlockByHash(hash: Hash, fullTransaction: Boolean): RpcCall<out Block?>
    suspend fun getBlockByNumber(number: ULong, fullTransaction: Boolean): RpcCall<out Block?>
    suspend fun getBlockByNumber(
        tag: BlockReference = BlockReference.latest,
        fullTransaction: Boolean
    ): RpcCall<out Block?>

    suspend fun getTransactionCount(address: Address, target: BlockReference = BlockReference.latest): RpcCall<HexULong>
    suspend fun getTransactionByHash(hash: Hash): RpcCall<Transaction?>
    suspend fun getTransactionByBlockHashAndIndex(blockHash: Hash, index: Int): RpcCall<Transaction?>
    suspend fun getTransactionByBlockNumberAndIndex(blockNumber: ULong, index: Int): RpcCall<Transaction?>
    suspend fun getTransactionReceipt(hash: Hash): RpcCall<TransactionReceipt?>

    suspend fun getUncleByBlockHashAndIndex(blockHash: Hash, index: Int): RpcCall<Block?>
    suspend fun getUncleByBlockNumberAndIndex(blockNumber: ULong, index: Int): RpcCall<Block?>

    suspend fun getLogs(request: GetLogsRequest): RpcCall<List<Log>>
    suspend fun getBalance(address: Address, target: BlockReference = BlockReference.latest): RpcCall<HexBigInt?>
    suspend fun getCode(address: Address, target: BlockReference = BlockReference.latest): RpcCall<HexData?>

    suspend fun call(request: CallRequest, target: BlockReference = BlockReference.latest): RpcCall<HexData?>
    suspend fun estimateGas(request: CallRequest): RpcCall<HexBigInt>
    suspend fun sendRawTransaction(signedTransactionData: String): RpcCall<String>
    suspend fun sendTransaction(privateKey: String, build: suspend TransactionBuilder.() -> Unit): RpcCall<String>

    suspend fun getFullBlock(hash: Hash) = getBlockByHash(hash, true) as RpcCall<FullBlock?>
    suspend fun getFullBlock(number: ULong) = getBlockByNumber(number, true) as RpcCall<FullBlock?>
    suspend fun getFullBlock(tag: BlockReference = BlockReference.latest) =
        getBlockByNumber(tag, true) as RpcCall<FullBlock?>

    suspend fun getFullBlocks(numbers: ULongProgression) = numbers.map { getFullBlock(it) }

    suspend fun getSimpleBlock(hash: Hash) = getBlockByHash(hash, false) as RpcCall<SimpleBlock?>
    suspend fun getSimpleBlock(number: ULong) = getBlockByNumber(number, false) as RpcCall<SimpleBlock?>
    suspend fun getSimpleBlock(tag: BlockReference = BlockReference.latest) =
        getBlockByNumber(tag, false) as RpcCall<SimpleBlock?>

    suspend fun getSimpleBlocks(numbers: ULongProgression) = numbers.map { getSimpleBlock(it) }

    suspend fun getLogs(init: GetLogsRequest.() -> Unit): RpcCall<List<Log>> = getLogs(GetLogsRequest().apply(init))

    suspend fun <T : ContractEvent> getLogs(
        event: ContractEventFactory<T, *>,
        init: GetLogsRequest.() -> Unit
    ): List<Pair<T, Log>> = getLogs(GetLogsRequest(topics = listOf(event.hash)).apply(init))
        .awaitOrThrow()
        .mapNotNull { log -> event.decodeIf(log.data, log.topics)?.let { e -> e to log } }

    suspend fun call(
        target: BlockReference = BlockReference.latest,
        init: CallRequest.() -> Unit
    ): RpcCall<HexData?> = call(CallRequest().apply(init), target)
}
