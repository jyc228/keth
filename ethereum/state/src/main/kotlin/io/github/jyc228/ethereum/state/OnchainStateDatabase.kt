package io.github.jyc228.ethereum.state

import io.github.jyc228.ethereum.state.account.Address
import io.github.jyc228.ethereum.state.account.ManagedStateAccount
import io.github.jyc228.ethereum.state.account.OnchainManagedStateAccount
import io.github.jyc228.ethereum.state.account.StateAccountTree
import io.github.jyc228.ethereum.state.account.StateRoot

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

    override suspend fun commit(deleteEmpty: Boolean) = accountTree.commit(deleteEmpty)

    override suspend fun intermediateRoot(deleteEmpty: Boolean) = accountTree.intermediateRoot(deleteEmpty)

    companion object {
        fun of(root: StateRoot?, database: TreeDatabase, codeDatabase: ContractCodeDatabase) =
            OnchainStateDatabase(StateAccountTree(root, database, codeDatabase))

        fun from(db: OnchainStateDatabase) = OnchainStateDatabase(StateAccountTree.from(db.accountTree))
        fun empty(database: TreeDatabase, codeDatabase: ContractCodeDatabase) =
            OnchainStateDatabase(StateAccountTree(null, database, codeDatabase))
    }
}
