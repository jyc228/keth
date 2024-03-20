package io.github.jyc228.ethereum.rpc.txpool

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.rpc.AbstractJsonRpcApi
import io.github.jyc228.ethereum.rpc.ApiResult
import io.github.jyc228.ethereum.rpc.JsonRpcClient

class TxpoolJsonRpcApi(client: JsonRpcClient) : TxpoolApi, AbstractJsonRpcApi(client) {
    override suspend fun content(): ApiResult<TxpoolContent> = "txpool_content"()
    override suspend fun contentFrom(address: Address): ApiResult<TxpoolContentFrom> = "txpool_contentFrom"(address)
    override suspend fun inspect(): ApiResult<TxpoolInspect> = "txpool_inspect"()
    override suspend fun status(): ApiResult<TxpoolStatus> = "txpool_status"()
}
