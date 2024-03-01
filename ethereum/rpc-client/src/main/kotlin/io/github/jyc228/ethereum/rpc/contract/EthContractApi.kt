package io.github.jyc228.ethereum.rpc.contract

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.contract.Contract
import io.github.jyc228.ethereum.rpc.eth.EthApi

class EthContractApi(
    private val eth: EthApi,
    private val contracts: MutableMap<Address, Contract.Factory<*>> = mutableMapOf()
) : ContractApi {
    constructor(eth: EthApi, contract: EthContractApi) : this(eth, contract.contracts)

    override fun <T : Contract<*>> set(address: Address, factory: Contract.Factory<T>) {
        contracts[address] = factory
    }

    override fun <T : Contract<*>> get(address: Address): T {
        return contracts.getValue(address).create(address, eth) as T
    }

    override fun <T : Contract<*>> create(address: Address, factory: Contract.Factory<T>): T {
        return factory.create(address, eth)
    }
}