package com.github.jyc228.keth.state

import com.github.jyc228.keth.state.account.ManagedStateAccount
import com.github.jyc228.keth.state.account.OnchainManagedStateAccount
import com.github.jyc228.keth.state.account.StateAccountTree
import com.github.jyc228.keth.state.account.StateRoot
import com.github.jyc228.keth.type.Address

class OnchainStateDatabase(val accountTree: StateAccountTree) : AbstractStateDatabase<OnchainManagedStateAccount>() {
    override suspend fun createAccount(
        address: Address,
        callback: (suspend (ManagedStateAccount) -> Unit)?
    ): OnchainManagedStateAccount {
        val account = accountTree.create(address) { prev, next ->
            accountTree.journal.disable { next.balance = prev.balance }
        }
        callback?.invoke(account)
        return account
    }

    override suspend fun findAccount(address: Address): OnchainManagedStateAccount? = accountTree[address]

    override fun snapshot(): Int = accountTree.journal.snapshot()

    override fun revertSnapshot(id: Int) = accountTree.journal.revertSnapshot(id, this)

    override fun dump() {
        TODO("Not yet implemented")
    }

    override suspend fun commit() = accountTree.commit()

    override suspend fun intermediateRoot() = accountTree.intermediateRoot()

    companion object {
        fun of(root: StateRoot?, database: TreeDatabase, codeDatabase: ContractCodeDatabase, eip158: Boolean = true) =
            OnchainStateDatabase(StateAccountTree(root, database, codeDatabase, eip158))

        fun from(db: OnchainStateDatabase) = OnchainStateDatabase(StateAccountTree.from(db.accountTree))
        fun empty(database: TreeDatabase, codeDatabase: ContractCodeDatabase, eip158: Boolean = true) =
            OnchainStateDatabase(StateAccountTree(null, database, codeDatabase, eip158))
    }
}
