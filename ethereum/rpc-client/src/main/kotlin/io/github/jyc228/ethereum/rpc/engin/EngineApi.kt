package io.github.jyc228.ethereum.rpc.engin

import io.github.jyc228.ethereum.rpc.RpcCall

interface EngineApi {
    suspend fun getPayloadV1(payloadId: PayloadId): RpcCall<ExecutionPayload>
    suspend fun newPayloadV1(payload: ExecutionPayload): RpcCall<PayloadStatusV1>
    suspend fun forkchoiceUpdatedV1(
        state: ForkchoiceState,
        attr: PayloadAttributes? = null
    ): RpcCall<ForkchoiceUpdatedResult>
}
