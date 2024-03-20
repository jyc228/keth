package io.github.jyc228.ethereum.rpc.txpool

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.rpc.ApiResult

interface TxpoolApi {
    suspend fun content(): ApiResult<TxpoolContent>
    suspend fun contentFrom(address: Address): ApiResult<TxpoolContentFrom>
    suspend fun inspect(): ApiResult<TxpoolInspect>
    suspend fun status(): ApiResult<TxpoolStatus>
}
