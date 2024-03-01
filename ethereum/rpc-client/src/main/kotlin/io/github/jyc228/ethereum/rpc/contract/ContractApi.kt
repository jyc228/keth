package io.github.jyc228.ethereum.rpc.contract

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.contract.Contract

interface ContractApi {
    operator fun <T : Contract<*>> set(address: Address, factory: Contract.Factory<T>)
    operator fun <T : Contract<*>> get(address: Address): T
    fun <T : Contract<*>> create(address: Address, factory: Contract.Factory<T>): T
}
