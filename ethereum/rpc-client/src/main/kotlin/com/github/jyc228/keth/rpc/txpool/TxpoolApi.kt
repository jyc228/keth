package com.github.jyc228.keth.rpc.txpool

import com.github.jyc228.keth.rpc.ApiResult
import com.github.jyc228.keth.type.Address

interface TxpoolApi {
    suspend fun content(): ApiResult<TxpoolContent>
    suspend fun contentFrom(address: Address): ApiResult<TxpoolContentFrom>
    suspend fun inspect(): ApiResult<TxpoolInspect>
    suspend fun status(): ApiResult<TxpoolStatus>
}
