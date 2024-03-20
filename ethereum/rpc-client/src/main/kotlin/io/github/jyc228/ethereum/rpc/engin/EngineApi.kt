package io.github.jyc228.ethereum.rpc.engin

import io.github.jyc228.ethereum.rpc.ApiResult

interface EngineApi {
    suspend fun getPayloadV1(payloadId: PayloadId): ApiResult<ExecutionPayload>
    suspend fun newPayloadV1(payload: ExecutionPayload): ApiResult<PayloadStatusV1>
    suspend fun forkchoiceUpdatedV1(
        state: ForkchoiceState,
        attr: PayloadAttributes? = null
    ): ApiResult<ForkchoiceUpdatedResult>
}
