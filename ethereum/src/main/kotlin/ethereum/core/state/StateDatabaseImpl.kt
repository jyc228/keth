package ethereum.core.state

import ethereum.collections.Hash
import ethereum.core.database.TreeDatabase
import ethereum.core.state.account.StateAccount
import ethereum.core.state.account.StateAccountTree
import ethereum.evm.Address
import java.math.BigInteger

class StateDatabaseImpl(val accountTree: StateAccountTree) : StateDatabase {
    val accessList = AccessList()

    override fun createAccount(address: Address, callback: ((StateAccount) -> Unit)?) {
        val account = accountTree.create(address) { prev, next ->
            accountTree.journal.disable { next.balance = prev.balance }
        }
        callback?.invoke(account)
    }

    override fun applyAccountOrCreate(
        address: Address,
        callback: (StateAccount) -> Unit
    ): StateAccount = (accountTree[address] ?: accountTree.create(address)).also(callback)

    override fun applyAccountOrThrow(
        address: Address,
        callback: (StateAccount) -> Unit
    ): StateAccount = accountTree[address]?.also(callback) ?: error("")

    override fun applyAccountOrNull(
        address: Address,
        callback: (StateAccount?) -> Unit
    ): StateAccount? = accountTree[address]?.also(callback)

    override fun <R> withAccountOrCreate(
        address: Address,
        transform: (StateAccount) -> R
    ) = transform((accountTree[address] ?: accountTree.create(address)))

    override fun <R> withAccountOrThrow(
        address: Address,
        transform: (StateAccount) -> R
    ): R = transform(accountTree[address] ?: error(""))

    override fun <R> withAccountOrNull(
        address: Address,
        transform: (StateAccount?) -> R
    ): R = transform(accountTree[address])

    override fun snapshot(): Int = accountTree.journal.snapshot()

    override fun revertSnapshot(id: Int) = accountTree.journal.revertSnapshot(id, this)

    override fun dump() {
        TODO("Not yet implemented")
    }

    override fun commit(deleteEmpty: Boolean) = accountTree.commit(deleteEmpty)

    override fun intermediateRoot(deleteEmpty: Boolean) = accountTree.intermediateRoot(deleteEmpty)

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
            StateDatabaseImpl(StateAccountTree(Hash.EMPTY, database))
    }
}
