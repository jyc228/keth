package io.github.jyc228.ethereum.state

import io.github.jyc228.ethereum.state.account.Address
import io.github.jyc228.ethereum.state.account.ManagedStateAccount

abstract class AbstractStateDatabase<T : ManagedStateAccount> : StateDatabase {
    abstract override suspend fun createAccount(address: Address, callback: (suspend (ManagedStateAccount) -> Unit)?): T
    abstract override suspend fun findAccount(address: Address): T?

    override suspend fun applyAccountOrCreate(
        address: Address,
        callback: suspend (ManagedStateAccount) -> Unit
    ): T = findAccount(address)?.also { callback(it) } ?: createAccount(address, callback)

    override suspend fun applyAccountOrThrow(
        address: Address,
        callback: suspend (ManagedStateAccount) -> Unit
    ): T = findAccount(address)?.also { callback(it) } ?: error("account 0x${address.hex} not exists")

    override suspend fun applyAccountOrNull(
        address: Address,
        callback: suspend (ManagedStateAccount?) -> Unit
    ): T? = findAccount(address).also { callback(it) }

    override suspend fun applyAccount(
        address: Address,
        callback: suspend (ManagedStateAccount) -> Unit
    ): T? = findAccount(address)?.also { callback(it) }

    override suspend fun <R> withAccountOrCreate(
        address: Address,
        transform: suspend (ManagedStateAccount) -> R
    ): R = transform(findAccount(address) ?: createAccount(address))

    override suspend fun <R> withAccountOrThrow(
        address: Address,
        transform: suspend (ManagedStateAccount) -> R
    ): R = transform(findAccount(address) ?: error("account 0x${address.hex} not exists"))

    override suspend fun <R> withAccountOrNull(
        address: Address,
        transform: suspend (ManagedStateAccount?) -> R
    ): R = transform(findAccount(address))

    override suspend fun <R> withAccount(
        address: Address,
        transform: suspend (ManagedStateAccount) -> R
    ): R? = findAccount(address)?.let { transform(it) }
}