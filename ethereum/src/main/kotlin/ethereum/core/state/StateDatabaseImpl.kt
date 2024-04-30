package ethereum.core.state

import ethereum.collections.Hash
import ethereum.core.database.TreeDatabase
import ethereum.core.state.account.ManagedStateAccount
import ethereum.core.state.account.StateAccount
import ethereum.core.state.account.StateAccountTree
import ethereum.evm.Address
import java.math.BigInteger

class StateDatabaseImpl(val accountTree: StateAccountTree) : StateDatabase {
    val accessList = AccessList()

    override suspend fun createAccount(address: Address, callback: (suspend (ManagedStateAccount) -> Unit)?) {
        val account = accountTree.create(address) { prev, next ->
            accountTree.journal.disable { next.balance = prev.balance }
        }
        callback?.invoke(account)
    }

    override suspend fun findAccount(address: Address): StateAccount? = accountTree[address]

    override suspend fun applyAccountOrCreate(
        address: Address,
        callback: suspend (ManagedStateAccount) -> Unit
    ): ManagedStateAccount = (accountTree[address] ?: accountTree.create(address)).also { callback(it) }

    override suspend fun applyAccountOrThrow(
        address: Address,
        callback: suspend (ManagedStateAccount) -> Unit
    ): ManagedStateAccount = accountTree[address]?.also { callback(it) } ?: error("account $address not exists")

    override suspend fun applyAccountOrNull(
        address: Address,
        callback: suspend (ManagedStateAccount?) -> Unit
    ): ManagedStateAccount? = accountTree[address].also { callback(it) }

    override suspend fun applyAccount(
        address: Address,
        callback: suspend (ManagedStateAccount) -> Unit
    ): StateAccount? = accountTree[address]?.also { callback(it) }

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

    data class Dump(val root: Hash, val accounts: Map<Hash, DumpAccount>)

    class DumpAccount(
        val balance: BigInteger,
        val nonce: ULong,
        val root: Hash,
        val codeHash: Hash,
        val code: ByteArray?,
        val storage: Map<Hash, String>?,
        // Address only present in iterative (line-by-line) mode
        val address: Address,
        // If we don't have address, we can output the key
        val key: ByteArray?
    )

    companion object {
        fun of(root: Hash, database: TreeDatabase) = StateDatabaseImpl(StateAccountTree(root, database))
        fun from(db: StateDatabaseImpl) = StateDatabaseImpl(StateAccountTree.from(db.accountTree))
        fun empty(database: TreeDatabase = TreeDatabase.memory()) =
            StateDatabaseImpl(StateAccountTree(null, database))
    }
}
