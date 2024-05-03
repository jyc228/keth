package ethereum.core.state

import ethereum.core.state.account.Address
import ethereum.core.state.account.ManagedStateAccount
import ethereum.core.state.account.OnchainManagedStateAccount
import ethereum.core.state.account.StateAccountTree
import ethereum.core.state.account.StateRoot

class StateDatabaseImpl(val accountTree: StateAccountTree) : StateDatabase {
    override suspend fun createAccount(address: Address, callback: (suspend (ManagedStateAccount) -> Unit)?) {
        val account = accountTree.create(address) { prev, next ->
            accountTree.journal.disable { next.balance = prev.balance }
        }
        callback?.invoke(account)
    }

    override suspend fun findAccount(address: Address): OnchainManagedStateAccount? = accountTree[address]

    override suspend fun applyAccountOrCreate(
        address: Address,
        callback: suspend (ManagedStateAccount) -> Unit
    ): OnchainManagedStateAccount = (accountTree[address] ?: accountTree.create(address)).also { callback(it) }

    override suspend fun applyAccountOrThrow(
        address: Address,
        callback: suspend (ManagedStateAccount) -> Unit
    ): OnchainManagedStateAccount = accountTree[address]?.also { callback(it) } ?: error("account $address not exists")

    override suspend fun applyAccountOrNull(
        address: Address,
        callback: suspend (ManagedStateAccount?) -> Unit
    ): OnchainManagedStateAccount? = accountTree[address].also { callback(it) }

    override suspend fun applyAccount(
        address: Address,
        callback: suspend (ManagedStateAccount) -> Unit
    ): OnchainManagedStateAccount? = accountTree[address]?.also { callback(it) }

    override suspend fun <R> withAccountOrCreate(
        address: Address,
        transform: suspend (ManagedStateAccount) -> R
    ) = transform((accountTree[address] ?: accountTree.create(address)))

    override suspend fun <R> withAccountOrThrow(
        address: Address,
        transform: suspend (ManagedStateAccount) -> R
    ): R = transform(accountTree[address] ?: error("account $address not exists"))

    override suspend fun <R> withAccountOrNull(
        address: Address,
        transform: suspend (ManagedStateAccount?) -> R
    ): R = transform(accountTree[address])

    override suspend fun <R> withAccount(
        address: Address,
        transform: suspend (ManagedStateAccount) -> R
    ): R? = accountTree[address]?.let { transform(it) }

    override fun snapshot(): Int = accountTree.journal.snapshot()

    override fun revertSnapshot(id: Int) = accountTree.journal.revertSnapshot(id, this)

    override fun dump() {
        TODO("Not yet implemented")
    }

    override suspend fun commit(deleteEmpty: Boolean) = accountTree.commit(deleteEmpty)

    override suspend fun intermediateRoot(deleteEmpty: Boolean) = accountTree.intermediateRoot(deleteEmpty)

    companion object {
        fun of(root: StateRoot?, database: TreeDatabase, codeDatabase: ContractCodeDatabase) =
            StateDatabaseImpl(StateAccountTree(root, database, codeDatabase))

        fun from(db: StateDatabaseImpl) = StateDatabaseImpl(StateAccountTree.from(db.accountTree))
        fun empty(database: TreeDatabase, codeDatabase: ContractCodeDatabase) =
            StateDatabaseImpl(StateAccountTree(null, database, codeDatabase))
    }
}
