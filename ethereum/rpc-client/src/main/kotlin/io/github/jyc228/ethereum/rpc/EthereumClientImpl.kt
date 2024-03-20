package io.github.jyc228.ethereum.rpc

import io.github.jyc228.ethereum.rpc.contract.ContractApi
import io.github.jyc228.ethereum.rpc.contract.EthContractApi
import io.github.jyc228.ethereum.rpc.engin.EngineApi
import io.github.jyc228.ethereum.rpc.engin.EngineJsonRpcApi
import io.github.jyc228.ethereum.rpc.eth.EthApi
import io.github.jyc228.ethereum.rpc.eth.EthJsonRpcApi
import io.github.jyc228.ethereum.rpc.txpool.TxpoolApi
import io.github.jyc228.ethereum.rpc.txpool.TxpoolJsonRpcApi
import io.github.jyc228.jsonrpc.KtorJsonRpcClient
import kotlin.time.Duration

class DefaultEthereumClient(
    private val client: KtorJsonRpcClient
) : EthereumClient {
    private val immediateCall = ImmediateJsonRpcClient(client)
    override val eth: EthApi = EthJsonRpcApi(immediateCall)
    override val engin: EngineApi = EngineJsonRpcApi(immediateCall)
    override val txpool: TxpoolApi = TxpoolJsonRpcApi(immediateCall)
    override val contract = EthContractApi(eth)

    override suspend fun <R> batch(init: suspend EthereumClient.() -> List<ApiResult<R>>): List<ApiResult<R>> {
        return BatchEthereumClient(client, contract).batch(init)
    }
}

class BatchEthereumClient(
    client: KtorJsonRpcClient,
    contract: EthContractApi
) : EthereumClient {
    private val batchCall = BatchJsonRpcClient(client)
    override val eth: EthApi = EthJsonRpcApi(batchCall)
    override val engin: EngineApi = EngineJsonRpcApi(batchCall)
    override val txpool: TxpoolApi = TxpoolJsonRpcApi(batchCall)
    override val contract: ContractApi = EthContractApi(eth, contract)
    override suspend fun <R> batch(init: suspend EthereumClient.() -> List<ApiResult<R>>) =
        batchCall.execute(init(this))
}

class ScheduledBatchEthereumClient(
    client: KtorJsonRpcClient,
    interval: Duration
) : EthereumClient {
    private val scheduledCall = ScheduledJsonRpcClient(client, interval)
    override val eth: EthApi = EthJsonRpcApi(scheduledCall)
    override val engin: EngineApi = EngineJsonRpcApi(scheduledCall)
    override val txpool: TxpoolApi = TxpoolJsonRpcApi(scheduledCall)
    override val contract: ContractApi = EthContractApi(eth)
    override suspend fun <R> batch(init: suspend EthereumClient.() -> List<ApiResult<R>>) = init(this)
}
