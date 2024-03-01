package ethereum.core.state

import ethereum.collections.Hash
import ethereum.core.state.account.StateAccount
import ethereum.evm.Address

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
    fun createAccount(address: Address, callback: ((StateAccount) -> Unit)? = null)

    fun applyAccountOrCreate(address: Address, callback: (StateAccount) -> Unit): StateAccount
    fun applyAccountOrThrow(address: Address, callback: (StateAccount) -> Unit): StateAccount
    fun applyAccountOrNull(address: Address, callback: (StateAccount?) -> Unit): StateAccount?

    fun <R> withAccountOrCreate(address: Address, transform: (StateAccount) -> R): R
    fun <R> withAccountOrThrow(address: Address, transform: (StateAccount) -> R): R
    fun <R> withAccountOrNull(address: Address, transform: (StateAccount?) -> R): R

    fun commit(deleteEmpty: Boolean): Hash

    /**
     * computes the current root hash of the state tree.
     * It is called in between transactions to get the root hash that goes into transaction receipts.
     */
    fun intermediateRoot(deleteEmpty: Boolean): Hash

    fun snapshot(): Int
    fun revertSnapshot(id: Int)
    fun dump()
}