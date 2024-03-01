package io.github.jyc228.ethereum.rpc.txpool

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.rpc.RpcCall

interface TxpoolApi {
    suspend fun content(): RpcCall<TxpoolContent>
    suspend fun contentFrom(address: Address): RpcCall<TxpoolContentFrom>
    suspend fun inspect(): RpcCall<TxpoolInspect>
    suspend fun status(): RpcCall<TxpoolStatus>
}
