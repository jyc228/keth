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

    override suspend fun getHeaderByHash(hash: Hash): ApiResult<SimpleBlockHeader> = "eth_getHeaderByHash"(hash.hex)
    override suspend fun getHeaderByNumber(number: ULong) = getHeaderByNumber(BlockReference(number))
    override suspend fun getHeaderByNumber(tag: String) = getHeaderByNumber(BlockReference.fromTag(tag))
    override suspend fun getHeaderByNumber(ref: BlockReference): ApiResult<SimpleBlockHeader> =
        "eth_getHeaderByNumber"(ref)

    override suspend fun getBlockByHash(
        hash: Hash,
        fullTx: Boolean
    ): ApiResult<out Block?> = when (fullTx) {
        true -> getFullBlockByHash(hash)
        false -> getSimpleBlockByHash(hash)
    }

    private suspend fun getFullBlockByHash(hash: Hash): ApiResult<FullBlock> = "eth_getBlockByHash"(hash.hex, true)
    private suspend fun getSimpleBlockByHash(hash: Hash): ApiResult<SimpleBlock> = "eth_getBlockByHash"(hash.hex, false)

    override suspend fun getBlockByNumber(number: ULong, fullTx: Boolean) =
        getBlockByNumber(BlockReference(number), fullTx)

    override suspend fun getBlockByNumber(tag: String, fullTx: Boolean) =
        getBlockByNumber(BlockReference.fromTag(tag), fullTx)

    override suspend fun getBlockByNumber(
        ref: BlockReference,
        fullTx: Boolean
    ): ApiResult<out Block?> = when (fullTx) {
        true -> "eth_getBlockByNumber"<FullBlock, BlockReference, Boolean>(ref, true)
        false -> "eth_getBlockByNumber"<SimpleBlock, BlockReference, Boolean>(ref, false)
    }

    override suspend fun getBlockTransactionCountByHash(hash: Hash): ApiResult<HexULong> =
        "eth_getBlockTransactionCountByHash"(hash.hex)

    override suspend fun getBlockTransactionCountByNumber(number: ULong): ApiResult<HexULong> =
        getBlockTransactionCountByNumber(BlockReference(number))

    override suspend fun getBlockTransactionCountByNumber(tag: String): ApiResult<HexULong> =
        getBlockTransactionCountByNumber(BlockReference.fromTag(tag))

    override suspend fun getBlockTransactionCountByNumber(ref: BlockReference): ApiResult<HexULong> =
        "eth_getBlockTransactionCountByNumber"(ref)

    override suspend fun getTransactionCount(address: Address, ref: BlockReference): ApiResult<HexULong> =
        "eth_getTransactionCount"(address, ref)

    override suspend fun getRawTransactionByHash(hash: Hash): ApiResult<HexData?> = "eth_getRawTransactionByHash"(hash)

    override suspend fun getRawTransactionByBlockHashAndIndex(
        blockHash: Hash,
        index: Int
    ): ApiResult<HexData?> = "eth_getRawTransactionByBlockHashAndIndex"(blockHash, index)

    override suspend fun getRawTransactionByBlockNumberAndIndex(
        blockNumber: ULong,
        index: Int
    ): ApiResult<HexData?> = "eth_getRawTransactionByBlockNumberAndIndex"(blockNumber, index)

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
        ref: BlockReference
    ): ApiResult<HexBigInt?> = "eth_getBalance"(address, ref)

    override suspend fun getCode(
        address: Address,
        ref: BlockReference
    ): ApiResult<HexData?> = "eth_getCode"(address, ref)

    override suspend fun call(
        request: CallRequest,
        ref: BlockReference
    ): ApiResult<HexData?> = "eth_call"(request, ref)

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
