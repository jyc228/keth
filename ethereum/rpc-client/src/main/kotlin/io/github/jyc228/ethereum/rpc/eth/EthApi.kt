package io.github.jyc228.ethereum.rpc.eth

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.HexBigInt
import io.github.jyc228.ethereum.HexData
import io.github.jyc228.ethereum.HexULong
import io.github.jyc228.ethereum.contract.ContractEvent
import io.github.jyc228.ethereum.contract.ContractEventFactory
import io.github.jyc228.ethereum.rpc.ApiResult

@Suppress("UNCHECKED_CAST")
interface EthApi {
    suspend fun chainId(): ApiResult<HexULong>
    suspend fun gasPrice(): ApiResult<HexBigInt>
    suspend fun blockNumber(): ApiResult<HexULong>

    suspend fun getBlockByHash(hash: Hash, fullTransaction: Boolean): ApiResult<out Block?>
    suspend fun getBlockByNumber(number: ULong, fullTransaction: Boolean): ApiResult<out Block?>
    suspend fun getBlockByNumber(
        tag: BlockReference = BlockReference.latest,
        fullTransaction: Boolean
    ): ApiResult<out Block?>

    suspend fun getTransactionCount(
        address: Address,
        target: BlockReference = BlockReference.latest
    ): ApiResult<HexULong>

    suspend fun getTransactionByHash(hash: Hash): ApiResult<Transaction?>
    suspend fun getTransactionByBlockHashAndIndex(blockHash: Hash, index: Int): ApiResult<Transaction?>
    suspend fun getTransactionByBlockNumberAndIndex(blockNumber: ULong, index: Int): ApiResult<Transaction?>
    suspend fun getTransactionReceipt(hash: Hash): ApiResult<TransactionReceipt?>

    suspend fun getUncleByBlockHashAndIndex(blockHash: Hash, index: Int): ApiResult<Block?>
    suspend fun getUncleByBlockNumberAndIndex(blockNumber: ULong, index: Int): ApiResult<Block?>

    suspend fun getLogs(request: GetLogsRequest): ApiResult<List<Log>>
    suspend fun getBalance(address: Address, target: BlockReference = BlockReference.latest): ApiResult<HexBigInt?>
    suspend fun getCode(address: Address, target: BlockReference = BlockReference.latest): ApiResult<HexData?>

    suspend fun call(request: CallRequest, target: BlockReference = BlockReference.latest): ApiResult<HexData?>
    suspend fun estimateGas(request: CallRequest): ApiResult<HexBigInt>
    suspend fun sendRawTransaction(signedTransactionData: String): ApiResult<String>
    suspend fun sendTransaction(privateKey: String, build: suspend TransactionBuilder.() -> Unit): ApiResult<String>

    suspend fun getFullBlock(hash: Hash) = getBlockByHash(hash, true) as ApiResult<FullBlock?>
    suspend fun getFullBlock(number: ULong) = getBlockByNumber(number, true) as ApiResult<FullBlock?>
    suspend fun getFullBlock(tag: BlockReference = BlockReference.latest) =
        getBlockByNumber(tag, true) as ApiResult<FullBlock?>

    suspend fun getFullBlocks(numbers: ULongProgression) = numbers.map { getFullBlock(it) }

    suspend fun getSimpleBlock(hash: Hash) = getBlockByHash(hash, false) as ApiResult<SimpleBlock?>
    suspend fun getSimpleBlock(number: ULong) = getBlockByNumber(number, false) as ApiResult<SimpleBlock?>
    suspend fun getSimpleBlock(tag: BlockReference = BlockReference.latest) =
        getBlockByNumber(tag, false) as ApiResult<SimpleBlock?>

    suspend fun getSimpleBlocks(numbers: ULongProgression) = numbers.map { getSimpleBlock(it) }

    suspend fun getLogs(init: GetLogsRequest.() -> Unit): ApiResult<List<Log>> = getLogs(GetLogsRequest().apply(init))

    suspend fun <T : ContractEvent> getLogs(
        event: ContractEventFactory<T, *>,
        init: GetLogsRequest.() -> Unit
    ): List<Pair<T, Log>> = getLogs(GetLogsRequest(topics = listOf(event.hash)).apply(init))
        .awaitOrThrow()
        .mapNotNull { log -> event.decodeIf(log.data, log.topics)?.let { e -> e to log } }

    suspend fun call(
        target: BlockReference = BlockReference.latest,
        init: CallRequest.() -> Unit
    ): ApiResult<HexData?> = call(CallRequest().apply(init), target)
}
