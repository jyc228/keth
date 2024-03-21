package io.github.jyc228.ethereum.contract

import io.github.jyc228.ethereum.AccountWithPrivateKey
import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.HexBigInt
import io.github.jyc228.ethereum.HexULong
import io.github.jyc228.ethereum.rpc.ApiResult
import io.github.jyc228.ethereum.rpc.eth.Access
import io.github.jyc228.ethereum.rpc.eth.BlockReference
import io.github.jyc228.ethereum.rpc.eth.CallRequest
import io.github.jyc228.ethereum.rpc.eth.EthApi

interface ContractFunctionRequest<R> {
    suspend fun call(build: suspend CallBuilder.() -> Unit): ApiResult<R>
    suspend fun transaction(
        account: AccountWithPrivateKey,
        build: suspend TransactionBuilder.() -> Unit
    ): ApiResult<Hash>

    interface CallBuilder {
        var from: String?
        var gasPrice: HexBigInt?
        var gasLimit: HexBigInt?
        var value: HexBigInt?
        var targetBlock: BlockReference
    }

    interface TransactionBuilder {
        var chainId: HexULong?
        var nonce: HexULong?
        var gasPrice: HexBigInt?
        var gasLimit: HexBigInt?
        var value: HexBigInt?
        var accessList: List<Access>
        var maxFeePerGas: HexBigInt?
        var maxPriorityFeePerGas: HexBigInt?
    }
}

class EthContractFunctionRequest<R>(
    private val contractAddress: Address,
    private val function: AbstractContractFunction<R>,
    private val eth: EthApi,
    private val data: String
) : ContractFunctionRequest<R>,
    ContractFunctionRequest.CallBuilder,
    ContractFunctionRequest.TransactionBuilder {
    override var from: String? = null
    override var gasLimit: HexBigInt? = null
    override var gasPrice: HexBigInt? = null
    override var value: HexBigInt? = null
    override var targetBlock: BlockReference = BlockReference.latest
    override var chainId: HexULong? = null
    override var nonce: HexULong? = null
    override var accessList: List<Access> = emptyList()
    override var maxFeePerGas: HexBigInt? = null
    override var maxPriorityFeePerGas: HexBigInt? = null

    override suspend fun call(build: suspend ContractFunctionRequest.CallBuilder.() -> Unit): ApiResult<R> {
        val builder: ContractFunctionRequest.CallBuilder = this.apply { build() }
        val result = eth.call(
            CallRequest(
                data = data,
                to = contractAddress.hex,
                from = builder.from,
                gas = builder.gasLimit,
                gasPrice = builder.gasPrice,
                value = builder.value,
            ),
            targetBlock
        )
        return result.map { function.decodeResult(it) }
    }

    override suspend fun transaction(
        account: AccountWithPrivateKey,
        build: suspend ContractFunctionRequest.TransactionBuilder.() -> Unit
    ): ApiResult<Hash> {
        val builder: ContractFunctionRequest.TransactionBuilder = this.apply { build() }
        return eth.sendTransaction(account) {
            this.input = data
            this.to = contractAddress
            this.nonce = builder.nonce ?: HexULong(0u)
            this.chainId = builder.chainId
            this.gasPrice = builder.gasPrice ?: HexBigInt.ZERO
            this.gasLimit = builder.gasLimit ?: HexBigInt.ZERO
            this.value = builder.value ?: HexBigInt.ZERO
            this.accessList += builder.accessList
            this.maxFeePerGas = builder.maxFeePerGas ?: HexBigInt.ZERO
            this.maxPriorityFeePerGas = builder.maxPriorityFeePerGas ?: HexBigInt.ZERO
        }
    }
}
