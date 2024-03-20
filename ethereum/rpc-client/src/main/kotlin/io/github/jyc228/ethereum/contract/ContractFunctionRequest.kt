package io.github.jyc228.ethereum.contract

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.HexInt
import io.github.jyc228.ethereum.rpc.ApiResult
import io.github.jyc228.ethereum.rpc.eth.BlockReference
import io.github.jyc228.ethereum.rpc.eth.CallRequest
import io.github.jyc228.ethereum.rpc.eth.EthApi

interface ContractFunctionRequest<R> {
    suspend fun call(build: CallBuilder.() -> Unit): ApiResult<R>
    suspend fun transaction(build: TransactionBuilder.() -> Unit): ApiResult<R> = error("")

    interface CallBuilder {
        var from: String?
        var gas: Int?
        var gasPrice: Int?
        var value: Int?
        var targetBlock: BlockReference
    }

    interface TransactionBuilder
}

class EthContractFunctionRequest<R>(
    private val address: Address,
    private val function: AbstractContractFunction<R>,
    private val eth: EthApi,
    private val data: String
) : ContractFunctionRequest<R>,
    ContractFunctionRequest.CallBuilder {
    override var from: String? = null
    override var gas: Int? = null
    override var gasPrice: Int? = null
    override var value: Int? = null
    override var targetBlock: BlockReference = BlockReference.latest

    override suspend fun call(build: ContractFunctionRequest.CallBuilder.() -> Unit): ApiResult<R> {
        build(this)
        val result = eth.call(
            CallRequest(
                from = from,
                to = address.hex,
                gas = gas?.let(::HexInt),
                gasPrice = gasPrice?.let(::HexInt),
                value = value?.let(::HexInt),
                data = data,
            ),
            targetBlock
        )
        return result.map { function.decodeResult(it) }
    }
}
