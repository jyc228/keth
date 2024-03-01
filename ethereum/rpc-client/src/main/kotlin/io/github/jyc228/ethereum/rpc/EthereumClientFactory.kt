package io.github.jyc228.ethereum.rpc

import io.github.jyc228.jsonrpc.KtorJsonRpcClient
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class EthereumClientConfig(
    var interval: Duration = 0.milliseconds,
    var adminJwtSecret: String? = null
)

fun EthereumClient.Companion.fromRpcUrl(
    url: String,
    initConfig: (EthereumClientConfig.() -> Unit)? = null
): EthereumClient {
    val config = EthereumClientConfig().apply { initConfig?.invoke(this) }
    if (config.interval.isPositive()) {
        return ScheduledBatchEthereumClient(KtorJsonRpcClient(url, config.adminJwtSecret), config.interval)
    }
    return DefaultEthereumClient(KtorJsonRpcClient(url, config.adminJwtSecret))
}

fun EthereumClient.Companion.mainnet(): EthereumClient = fromRpcUrl("https://rpc.sepolia.org")

fun EthereumClient.Companion.sepolia(): EthereumClient = fromRpcUrl("https://rpc2.sepolia.org")

