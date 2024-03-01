package io.github.jyc228.ethereum.rpc.txpool

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.rpc.AbstractJsonRpcApi
import io.github.jyc228.ethereum.rpc.JsonRpcClient
import io.github.jyc228.ethereum.rpc.RpcCall

class TxpoolJsonRpcApi(client: JsonRpcClient) : TxpoolApi, AbstractJsonRpcApi(client) {
    override suspend fun content(): RpcCall<TxpoolContent> = "txpool_content"()
    override suspend fun contentFrom(address: Address): RpcCall<TxpoolContentFrom> = "txpool_contentFrom"(address)
    override suspend fun inspect(): RpcCall<TxpoolInspect> = "txpool_inspect"()
    override suspend fun status(): RpcCall<TxpoolStatus> = "txpool_status"()
}
