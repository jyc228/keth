package io.github.jyc228.ethereum.rpc.engin

import io.github.jyc228.ethereum.rpc.AbstractJsonRpcApi
import io.github.jyc228.ethereum.rpc.ApiResult
import io.github.jyc228.ethereum.rpc.JsonRpcClient

class EngineJsonRpcApi(client: JsonRpcClient) : EngineApi, AbstractJsonRpcApi(client) {
    override suspend fun getPayloadV1(payloadId: PayloadId): ApiResult<ExecutionPayload> =
        "engine_getPayloadV1"(payloadId)

    override suspend fun newPayloadV1(payload: ExecutionPayload): ApiResult<PayloadStatusV1> =
        "engine_newPayloadV1"(payload)

    override suspend fun forkchoiceUpdatedV1(
        state: ForkchoiceState,
        attr: PayloadAttributes?
    ): ApiResult<ForkchoiceUpdatedResult> = "engine_forkchoiceUpdatedV1"(state, attr)
}
