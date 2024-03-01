package io.github.jyc228.ethereum.rpc.engin

import io.github.jyc228.ethereum.rpc.AbstractJsonRpcApi
import io.github.jyc228.ethereum.rpc.JsonRpcClient
import io.github.jyc228.ethereum.rpc.RpcCall

class EngineJsonRpcApi(client: JsonRpcClient) : EngineApi, AbstractJsonRpcApi(client) {
    override suspend fun getPayloadV1(payloadId: PayloadId): RpcCall<ExecutionPayload> =
        "engine_getPayloadV1"(payloadId)

    override suspend fun newPayloadV1(payload: ExecutionPayload): RpcCall<PayloadStatusV1> =
        "engine_newPayloadV1"(payload)

    override suspend fun forkchoiceUpdatedV1(
        state: ForkchoiceState,
        attr: PayloadAttributes?
    ): RpcCall<ForkchoiceUpdatedResult> = "engine_forkchoiceUpdatedV1"(state, attr)
}