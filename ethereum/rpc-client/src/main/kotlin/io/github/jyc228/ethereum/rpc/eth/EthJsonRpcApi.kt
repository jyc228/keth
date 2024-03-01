package io.github.jyc228.ethereum.rpc.eth

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.HexBigInt
import io.github.jyc228.ethereum.HexData
import io.github.jyc228.ethereum.HexULong
import io.github.jyc228.ethereum.rpc.AbstractJsonRpcApi
import io.github.jyc228.ethereum.rpc.JsonRpcClient
import io.github.jyc228.ethereum.rpc.RpcCall
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.utils.Numeric

class EthJsonRpcApi(client: JsonRpcClient) : EthApi, AbstractJsonRpcApi(client) {

    override suspend fun chainId(): RpcCall<HexULong> = "eth_chainId"()
    override suspend fun gasPrice(): RpcCall<HexBigInt> = "eth_gasPrice"()
    override suspend fun blockNumber(): RpcCall<HexULong> = "eth_blockNumber"()

    override suspend fun getBlockByHash(
        hash: Hash,
        fullTransaction: Boolean
    ): RpcCall<out Block?> = getBlock(BlockReference(hash), fullTransaction)

    override suspend fun getBlockByNumber(
        number: ULong,
        fullTransaction: Boolean
    ): RpcCall<out Block?> = getBlock(BlockReference(number), fullTransaction)

    override suspend fun getBlockByNumber(
        tag: BlockReference,
        fullTransaction: Boolean
    ): RpcCall<out Block?> = getBlock(tag, fullTransaction)

    private suspend inline fun getBlock(
        target: BlockReference,
        fullTransaction: Boolean
    ): RpcCall<out Block?> = when (fullTransaction) {
        true -> "eth_getBlockByNumber"<FullBlock, BlockReference, Boolean>(target, true)
        false -> "eth_getBlockByNumber"<SimpleBlock, BlockReference, Boolean>(target, false)
    }

    override suspend fun getTransactionCount(
        address: Address,
        target: BlockReference
    ): RpcCall<HexULong> = "eth_getTransactionCount"(address, target)

    override suspend fun getTransactionByHash(hash: Hash): RpcCall<Transaction?> = "eth_getTransactionByHash"(hash)

    override suspend fun getTransactionByBlockHashAndIndex(
        blockHash: Hash,
        index: Int
    ): RpcCall<Transaction?> = "eth_getTransactionByBlockHashAndIndex"(blockHash, index)

    override suspend fun getTransactionByBlockNumberAndIndex(
        blockNumber: ULong,
        index: Int
    ): RpcCall<Transaction?> = "eth_getTransactionByBlockNumberAndIndex"(blockNumber, index)

    override suspend fun getTransactionReceipt(hash: Hash): RpcCall<TransactionReceipt?> =
        "eth_getTransactionReceipt"(hash)

    override suspend fun getUncleByBlockHashAndIndex(
        blockHash: Hash,
        index: Int
    ): RpcCall<Block?> = "eth_getUncleByBlockHashAndIndex"(blockHash, index)

    override suspend fun getUncleByBlockNumberAndIndex(
        blockNumber: ULong,
        index: Int
    ): RpcCall<Block?> = "eth_getUncleByBlockNumberAndIndex"(blockNumber, index)

    override suspend fun getLogs(request: GetLogsRequest): RpcCall<List<Log>> = "eth_getLogs"(request)

    override suspend fun getBalance(
        address: Address,
        target: BlockReference
    ): RpcCall<HexBigInt?> = "eth_getBalance"(address, target)

    override suspend fun getCode(
        address: Address,
        target: BlockReference
    ): RpcCall<HexData?> = "eth_getCode"(address, target)

    override suspend fun call(
        request: CallRequest,
        target: BlockReference
    ): RpcCall<HexData?> = "eth_call"(request, target)

    override suspend fun estimateGas(request: CallRequest): RpcCall<HexBigInt> = "eth_estimateGas"(request)

    override suspend fun sendRawTransaction(signedTransactionData: String): RpcCall<String> =
        "eth_sendRawTransaction"(signedTransactionData)

    override suspend fun sendTransaction(
        privateKey: String,
        build: suspend TransactionBuilder.() -> Unit
    ): RpcCall<String> {
        val client by lazy(LazyThreadSafetyMode.NONE) { EthJsonRpcApi(client.toImmediateClient()) }
        val tx = TransactionBuilder().apply { build() }
        val rawTx = RawTransaction.createTransaction(
            tx.nonce.hex.removePrefix("0x").toBigInteger(16), // nonce
            tx.gasPrice?.number ?: client.gasPrice().awaitOrThrow().number,
            tx.gas.number,
            tx.to?.hex,
            tx.value.number,
            tx.input
        )

        val signedMessage = TransactionEncoder.signMessage(
            rawTx,
            tx.chainId?.number?.toLong() ?: client.chainId().awaitOrThrow().number.toLong(),
            Credentials.create(privateKey)
        )
        return sendRawTransaction(Numeric.toHexString(signedMessage))
    }
}
