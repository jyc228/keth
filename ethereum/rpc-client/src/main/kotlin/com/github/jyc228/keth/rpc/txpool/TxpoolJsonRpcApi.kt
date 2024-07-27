package com.github.jyc228.keth.rpc.txpool

import com.github.jyc228.keth.rpc.AbstractJsonRpcApi
import com.github.jyc228.keth.rpc.ApiResult
import com.github.jyc228.keth.rpc.JsonRpcClient
import com.github.jyc228.keth.type.Address

class TxpoolJsonRpcApi(client: JsonRpcClient) : TxpoolApi, AbstractJsonRpcApi(client) {
    override suspend fun content(): ApiResult<TxpoolContent> = "txpool_content"()
    override suspend fun contentFrom(address: Address): ApiResult<TxpoolContentFrom> = "txpool_contentFrom"(address)
    override suspend fun inspect(): ApiResult<TxpoolInspect> = "txpool_inspect"()
    override suspend fun status(): ApiResult<TxpoolStatus> = "txpool_status"()
}
