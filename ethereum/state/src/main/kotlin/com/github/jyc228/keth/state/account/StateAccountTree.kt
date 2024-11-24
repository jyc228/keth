package com.github.jyc228.keth.state.account

import com.github.jyc228.keth.collections.MerkleTree
import com.github.jyc228.keth.collections.MerkleTreeDirtyNodes
import com.github.jyc228.keth.state.ContractCodeDatabase
import com.github.jyc228.keth.state.Journal
import com.github.jyc228.keth.state.JournalEntry
import com.github.jyc228.keth.state.TreeDatabase

class StateAccountTree(
    private var originalRoot: StateRoot?,
    private val database: TreeDatabase,
    private val codeRepository: ContractCodeDatabase,
    private val eip158: Boolean
) {
    private val tree = MerkleTree.fromRootState(originalRoot?.bytes, database::node)
    val accountByAddress = mutableMapOf<Address, OnchainManagedStateAccount>()
    val pendingAddress = mutableSetOf<Address>() // State objects finalized but not yet written to the trie
    val dirtyAddress = mutableSetOf<Address>() // State objects modified in the current execution
    val destructAddress = mutableSetOf<Address>() // State objects destructed in the block

    val journal = Journal()
    var refund = 0

    fun create(
        address: Address,
        handlePrev: ((prev: ManagedStateAccount, next: ManagedStateAccount) -> Unit)? = null
    ): OnchainManagedStateAccount {
        val prev = findDeletedOrNull(address)
        val next = OnchainManagedStateAccount.new(address).also { accountByAddress[address] = it }
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

    operator fun get(address: Address): OnchainManagedStateAccount? = findDeletedOrNull(address)?.takeIf { !it.deleted }

    /**
     * similar to [findOrNull], but instead of returning nil for a deleted state object, it returns the actual object with the deleted flag set.
     *
     * This is needed by the state [journal] to revert to the correct s- destructed object instead of wiping all knowledge about the state account.
     */
    private fun findDeletedOrNull(address: Address): OnchainManagedStateAccount? {
        var stateAccount = accountByAddress[address]
        if (stateAccount == null) {
            val account = findFromSnapshotOrNull() ?: tree[address.bytes]?.let(AccountRlp::decode) ?: return null
            stateAccount = OnchainManagedStateAccount.new(address, account)
            accountByAddress[address] = stateAccount
        }
        return stateAccount
    }

    fun findFromSnapshotOrNull(): ManagedStateAccount? = null

    fun commit(): StateRoot? {
        intermediateRoot()

        val storageDirtyNodes = mutableMapOf<AddressHash, MerkleTreeDirtyNodes>()
        dirtyAddress.forEachAccount { account ->
            if (account.deleted) return@forEachAccount
            if (account.code != null && account.dirtyCode) {
                codeRepository.saveCode(account.codeHash!!, account.code!!)
                account.dirtyCode = false
            }
            account.storage.collectDirties()?.let {
                val addrHash = AddressHash.keccak256FromBytes(account.address.bytes)
                storageDirtyNodes.merge(addrHash, it) { prev, next -> prev.merge(next) }
            }
        }

        val accountDirtyNodes = tree.collectDirties(true)
        if (!originalRoot?.bytes.contentEquals(tree.rootHash())) {
            database.update(accountDirtyNodes!!, storageDirtyNodes)
            originalRoot = tree.rootHash()?.let(::StateRoot)
        }
        return tree.rootHash()?.let(::StateRoot)
    }

    fun intermediateRoot(): StateRoot? {
        // Finalise all the dirty storage states and write them into the tries
        finalise()

        pendingAddress.forEachAccount { if (!it.deleted) it.storage.applyPending() }
        pendingAddress.forEachAccount { account ->
            when (account.deleted) {
                true -> tree -= account.address.bytes
                false -> tree[account.address.bytes] = AccountRlp.encode(account)
            }
        }

        return tree.rootHash()?.let(::StateRoot)
    }


    /**
     * finalises the state by removing the destructed objects and clears the journal as well as the refunds
     * [finalise], however, will not push any updates into the tries just yet. Only [intermediateRoot] or [commit] will do that.
     */
    fun finalise() {
        val addressesToPrefetch = journal.dirtyAddresses().mapNotNull { addr ->
            // ripeMD is 'touched' at block 1714175, in tx 0x1237f737031e40bcde4a8b7e717b2d15e3ecadfe49bb1bbc71ee9deb09c6fcf2
            // That tx goes out of gas, and although the notion of 'touched' does not exist there, the
            // touch-event will still be recorded in the journal. Since ripeMD is a special snowflake,
            // it will persist in the journal even though the journal is reverted. In this special circumstance,
            // it may exist in `s.journal.dirties` but not in `s.stateObjects`.
            // Thus, we can safely ignore it here
            val account = accountByAddress[addr] ?: return@mapNotNull null
            if (account.suicided || eip158 && account.empty) {
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

    fun OnchainManagedStateAccount.Companion.new(address: Address, account: StateAccount? = null) =
        OnchainManagedStateAccount(
            address = address,
            account = account ?: StateAccount.of(0uL, 0.toBigInteger(), null, null),
            journal = journal,
            codeRepository = codeRepository,
            storage = StateAccountStorage(
                owner = address,
                journal = journal,
                tree = MerkleTree.lazyFromRootState(account?.root?.bytes, database::node),
                isDestruct = { it in destructAddress }
            )
        )

    private fun Set<Address>.forEachAccount(handle: (OnchainManagedStateAccount) -> Unit) {
        for (address in this) accountByAddress[address]?.apply(handle)
    }

    companion object {
        fun from(m: StateAccountTree) = StateAccountTree(m.originalRoot, m.database, m.codeRepository, m.eip158)
    }
}
