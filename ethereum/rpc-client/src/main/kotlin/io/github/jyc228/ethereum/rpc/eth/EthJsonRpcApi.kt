package io.github.jyc228.ethereum.rpc.eth

import io.github.jyc228.ethereum.AccountWithPrivateKey
import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.HexBigInt
import io.github.jyc228.ethereum.HexData
import io.github.jyc228.ethereum.HexULong
import io.github.jyc228.ethereum.rpc.AbstractJsonRpcApi
import io.github.jyc228.ethereum.rpc.ApiResult
import io.github.jyc228.ethereum.rpc.JsonRpcClient
import java.math.BigInteger
import org.web3j.crypto.Credentials
import org.web3j.crypto.TransactionEncoder
import org.web3j.utils.Numeric

class EthJsonRpcApi(client: JsonRpcClient) : EthApi, AbstractJsonRpcApi(client) {

    override suspend fun chainId(): ApiResult<HexULong> = "eth_chainId"()
    override suspend fun gasPrice(): ApiResult<HexBigInt> = "eth_gasPrice"()
    override suspend fun blockNumber(): ApiResult<HexULong> = "eth_blockNumber"()

    override suspend fun getBlockByHash(
        hash: Hash,
        fullTransaction: Boolean
    ): ApiResult<out Block?> = getBlock(BlockReference(hash), fullTransaction)

    override suspend fun getBlockByNumber(
        number: ULong,
        fullTransaction: Boolean
    ): ApiResult<out Block?> = getBlock(BlockReference(number), fullTransaction)

    override suspend fun getBlockByNumber(
        tag: BlockReference,
        fullTransaction: Boolean
    ): ApiResult<out Block?> = getBlock(tag, fullTransaction)

    private suspend inline fun getBlock(
        target: BlockReference,
        fullTransaction: Boolean
    ): ApiResult<out Block?> = when (fullTransaction) {
        true -> "eth_getBlockByNumber"<FullBlock, BlockReference, Boolean>(target, true)
        false -> "eth_getBlockByNumber"<SimpleBlock, BlockReference, Boolean>(target, false)
    }

    override suspend fun getTransactionCount(
        address: Address,
        target: BlockReference
    ): ApiResult<HexULong> = "eth_getTransactionCount"(address, target)

    override suspend fun getTransactionByHash(hash: Hash): ApiResult<Transaction?> = "eth_getTransactionByHash"(hash)

    override suspend fun getTransactionByBlockHashAndIndex(
        blockHash: Hash,
        index: Int
    ): ApiResult<Transaction?> = "eth_getTransactionByBlockHashAndIndex"(blockHash, index)

    override suspend fun getTransactionByBlockNumberAndIndex(
        blockNumber: ULong,
        index: Int
    ): ApiResult<Transaction?> = "eth_getTransactionByBlockNumberAndIndex"(blockNumber, index)

    override suspend fun getTransactionReceipt(hash: Hash): ApiResult<TransactionReceipt?> =
        "eth_getTransactionReceipt"(hash)

    override suspend fun getUncleByBlockHashAndIndex(
        blockHash: Hash,
        index: Int
    ): ApiResult<Block?> = "eth_getUncleByBlockHashAndIndex"(blockHash, index)

    override suspend fun getUncleByBlockNumberAndIndex(
        blockNumber: ULong,
        index: Int
    ): ApiResult<Block?> = "eth_getUncleByBlockNumberAndIndex"(blockNumber, index)

    override suspend fun getLogs(request: GetLogsRequest): ApiResult<List<Log>> = "eth_getLogs"(request)

    override suspend fun getBalance(
        address: Address,
        target: BlockReference
    ): ApiResult<HexBigInt?> = "eth_getBalance"(address, target)

    override suspend fun getCode(
        address: Address,
        target: BlockReference
    ): ApiResult<HexData?> = "eth_getCode"(address, target)

    override suspend fun call(
        request: CallRequest,
        target: BlockReference
    ): ApiResult<HexData?> = "eth_call"(request, target)

    override suspend fun estimateGas(request: CallRequest): ApiResult<HexBigInt> = "eth_estimateGas"(request)

    override suspend fun sendRawTransaction(signedTransactionData: String): ApiResult<Hash> =
        "eth_sendRawTransaction"(signedTransactionData)

    override suspend fun sendTransaction(
        account: AccountWithPrivateKey,
        build: suspend TransactionBuilder.() -> Unit
    ): ApiResult<Hash> {
        val client = EthJsonRpcApi(client.toImmediateClient())
        val tx = TransactionBuilder().apply { build() }
        if (tx.gasPrice.number == BigInteger.ZERO) {
            tx.gasPrice = client.gasPrice().awaitOrThrow()
        }
        if (tx.gasLimit.number == BigInteger.ZERO && (tx.input != "" && tx.input != "0x")) {
            tx.gasLimit = client.estimateGas(
                CallRequest(
                    from = account.address.hex,
                    to = tx.to?.hex,
                    gasPrice = tx.gasPrice,
                    data = tx.input,
                    value = tx.value
                )
            ).awaitOrThrow()
        }
        val signedMessage = TransactionEncoder.signMessage(
            tx.toWeb3jTransaction(),
            tx.chainId?.number?.toLong() ?: client.chainId().awaitOrThrow().number.toLong(),
            Credentials.create(account.privateKey)
        )
        return sendRawTransaction(Numeric.toHexString(signedMessage))
    }
}
