package ethereum.core.state.account

import ethereum.collections.Hash
import ethereum.collections.MerkleTree
import ethereum.collections.MerkleTreeDirtyNodes
import ethereum.collections.MerkleTreeWithMetrics
import ethereum.collections.fromRootState
import ethereum.collections.lazyFromRootState
import ethereum.core.database.TreeDatabase
import ethereum.core.repository.ContractCodeRepository
import ethereum.core.state.Journal
import ethereum.core.state.JournalEntry
import ethereum.evm.Address
import ethereum.rlp.rlpToObject
import ethereum.type.Account

class StateAccountTree(
    private var originalRoot: Hash,
    private val database: TreeDatabase,
    private val codeRepository: ContractCodeRepository = ContractCodeRepository(database.db)
) {
    private val tree = MerkleTree.fromRootState(originalRoot, database::node)
    val accountByAddress = mutableMapOf<Address, StateAccount>()
    val pendingAddress = mutableSetOf<Address>() // State objects finalized but not yet written to the trie
    val dirtyAddress = mutableSetOf<Address>() // State objects modified in the current execution
    val destructAddress = mutableSetOf<Address>() // State objects destructed in the block

    val journal = Journal()
    var refund = 0

    fun create(address: Address, handlePrev: ((prev: StateAccount, next: StateAccount) -> Unit)? = null): StateAccount {
        val prev = findDeletedOrNull(address)
        val next = StateAccount.new(address).also { accountByAddress[address] = it }
        when (prev == null) {
            true -> journal.append { JournalEntry.CreateObjectChange(address) }
            false -> {
                journal.append { JournalEntry.ResetObjectChange(address, prev, true) }
                destructAddress += prev.address
                handlePrev?.invoke(prev, next)
            }
        }
        return next
    }

    operator fun get(address: Address): StateAccount? = findDeletedOrNull(address)?.takeIf { !it.deleted }

    /**
     * similar to [findOrNull], but instead of returning nil for a deleted state object, it returns the actual object with the deleted flag set.
     *
     * This is needed by the state [journal] to revert to the correct s- destructed object instead of wiping all knowledge about the state account.
     */
    private fun findDeletedOrNull(address: Address): StateAccount? {
        var stateAccount = accountByAddress[address]
        if (stateAccount == null) {
            val account = findFromSnapshotOrNull() ?: tree[address.bytes]?.rlpToObject<Account.Default>() ?: return null
            stateAccount = StateAccount.new(address, account)
            accountByAddress[address] = stateAccount
        }
        return stateAccount
    }

    fun findFromSnapshotOrNull(): Account? = null

    fun commit(deleteEmptyObjects: Boolean): Hash {
        intermediateRoot(deleteEmptyObjects)

        val storageDirtyNodes = mutableMapOf<Hash, MerkleTreeDirtyNodes>()
        dirtyAddress.forEachAccount { account ->
            if (account.deleted) return@forEachAccount
            if (account.code != null && account.dirtyCode) {
                codeRepository.saveCode(account.codeHash, account.code!!)
                account.dirtyCode = false
            }
            account.storage.collectDirties()?.let {
                val addrHash = Hash.keccak256FromBytes(account.address.bytes)
                storageDirtyNodes.merge(addrHash, it) { prev, next -> prev.merge(next) }
            }
        }

        val accountDirtyNodes = tree.collectDirties(true)
        if (originalRoot != tree.rootHash()?.let(::Hash)) {
            database.update(accountDirtyNodes!!, storageDirtyNodes)
            originalRoot = tree.rootHash()?.let(::Hash) ?: Hash.EMPTY_MPT_ROOT
        }
        return tree.rootHash()?.let(::Hash) ?: Hash.EMPTY_MPT_ROOT
    }

    fun intermediateRoot(deleteEmptyObject: Boolean): Hash {
        // Finalise all the dirty storage states and write them into the tries
        finalise(deleteEmptyObject)

        pendingAddress.forEachAccount { if (!it.deleted) it.storage.applyPending() }
        pendingAddress.forEachAccount { account ->
            when (account.deleted) {
                true -> tree -= account.address.bytes
                false -> tree[account.address.bytes] = account.encodeToRlp()
            }
        }

        return tree.rootHash()?.let(::Hash) ?: Hash.EMPTY_MPT_ROOT
    }


    /**
     * finalises the state by removing the destructed objects and clears the journal as well as the refunds
     * [finalise], however, will not push any updates into the tries just yet. Only [intermediateRoot] or [commit] will do that.
     */
    fun finalise(deleteEmptyObject: Boolean) {
        val addressesToPrefetch = journal.dirtyAddresses().mapNotNull { addr ->
            // ripeMD is 'touched' at block 1714175, in tx 0x1237f737031e40bcde4a8b7e717b2d15e3ecadfe49bb1bbc71ee9deb09c6fcf2
            // That tx goes out of gas, and although the notion of 'touched' does not exist there, the
            // touch-event will still be recorded in the journal. Since ripeMD is a special snowflake,
            // it will persist in the journal even though the journal is reverted. In this special circumstance,
            // it may exist in `s.journal.dirties` but not in `s.stateObjects`.
            // Thus, we can safely ignore it here
            val account = accountByAddress[addr] ?: return@mapNotNull null
            if (account.suicided || deleteEmptyObject && account.empty) {
                account.deleted = true
                destructAddress += account.address
            } else {
                account.storage.dirtyToPending()
            }
            pendingAddress += addr
            dirtyAddress += addr
            addr
        }

        journal.clear()
        refund = 0
//        validRevisions = mutableListOf(validRevisions[0])
    }


    fun StateAccount.Companion.new(address: Address, account: Account? = null) = StateAccount(
        address = address,
        account = account ?: Account(),
        journal = journal,
        codeRepository = codeRepository,
        storage = StateAccountStorage(
            owner = address,
            journal = journal,
            tree = MerkleTreeWithMetrics(MerkleTree.lazyFromRootState(account?.root, database::node)),
            isDestruct = { it in destructAddress }
        )
    )

    private fun Set<Address>.forEachAccount(handle: (StateAccount) -> Unit) {
        for (address in this) accountByAddress[address]?.apply(handle)
    }

    private fun Set<Address>.asSequenceAccount(): Sequence<StateAccount> =
        asSequence().mapNotNull { accountByAddress[it] }

    companion object {
        fun from(m: StateAccountTree) = StateAccountTree(m.originalRoot, m.database, m.codeRepository)
    }
}
