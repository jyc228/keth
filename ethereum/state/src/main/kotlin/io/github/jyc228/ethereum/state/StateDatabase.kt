package io.github.jyc228.ethereum.state

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.state.account.ManagedStateAccount
import io.github.jyc228.ethereum.state.account.StateAccount
import io.github.jyc228.ethereum.state.account.StateRoot

interface StateDatabase {
    /**
     * explicitly creates a state object.
     * If a state object with the address already exists the balance is carried over to the new account.
     *
     * [createAccount] is called during the EVM CREATE operation.
     * The situation might arise that a contract does the following:
     *
     * 1. sends funds to sha(account ++ (nonce + 1))
     * 2. tx_create(sha(account ++ nonce)) (note that this gets the address of 1)
     *
     * Carrying over the balance ensures that Ether doesn't disappear.
     */
    suspend fun createAccount(address: Address, callback: (suspend (ManagedStateAccount) -> Unit)? = null): StateAccount
    suspend fun findAccount(address: Address): StateAccount?

    suspend fun applyAccountOrCreate(address: Address, callback: suspend (ManagedStateAccount) -> Unit): StateAccount
    suspend fun applyAccountOrThrow(address: Address, callback: suspend (ManagedStateAccount) -> Unit): StateAccount
    suspend fun applyAccountOrNull(address: Address, callback: suspend (ManagedStateAccount?) -> Unit): StateAccount?
    suspend fun applyAccount(address: Address, callback: suspend (ManagedStateAccount) -> Unit): StateAccount?

    suspend fun <R> withAccountOrCreate(address: Address, transform: suspend (ManagedStateAccount) -> R): R
    suspend fun <R> withAccountOrThrow(address: Address, transform: suspend (ManagedStateAccount) -> R): R
    suspend fun <R> withAccountOrNull(address: Address, transform: suspend (ManagedStateAccount?) -> R): R
    suspend fun <R> withAccount(address: Address, transform: suspend (ManagedStateAccount) -> R): R?

    suspend fun commit(deleteEmpty: Boolean): StateRoot?

    /**
     * computes the current root hash of the state tree.
     * It is called in between transactions to get the root hash that goes into transaction receipts.
     */
    suspend fun intermediateRoot(deleteEmpty: Boolean): StateRoot?

    fun snapshot(): Int
    fun revertSnapshot(id: Int)
    fun dump()
}
