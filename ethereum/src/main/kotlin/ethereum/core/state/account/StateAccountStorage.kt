package ethereum.core.state.account

import ethereum.collections.Hash
import ethereum.collections.MerkleTreeDirtyNodes
import ethereum.collections.MerkleTreeWithMetrics
import ethereum.core.state.Journal
import ethereum.core.state.JournalEntry
import ethereum.evm.Address
import ethereum.rlp.RLPEncoder

class StateAccountStorage(
    private val journal: Journal,
    private val owner: Address,
    private val tree: MerkleTreeWithMetrics,
    private val isDestruct: (owner: Address) -> Boolean
) {
    val rootHash get() = tree.rootHash()?.let(::Hash) ?: Hash.EMPTY_MPT_ROOT

    /** Storage cache of original entries to dedup rewrites, reset for every transaction */
    private val origin = mutableMapOf<Hash, Hash>()

    /** Storage entries that need to be flushed to disk, at the end of an entire block */
    private val pending = mutableMapOf<Hash, Hash>()

    /** Storage entries that have been modified in the current transaction execution */
    val dirty = mutableMapOf<Hash, Hash>()

    operator fun set(key: Hash, value: Hash) {
        val prev = get(key)
        if (prev != value) {
            journal.append { JournalEntry.StorageChange(owner, key, prev) }
            dirty[key] = value
        }
    }

    /** retrieves a value from the account storage trie. */
    operator fun get(key: Hash): Hash = dirty[key] ?: getCommittedState(key)

    fun getCommittedState(key: Hash): Hash {
        return pending[key]
            ?: origin[key]
            ?: when (isDestruct(owner)) {
                true -> Hash.EMPTY
                false -> tree[key.bytes]?.let(Hash::fromByteArray)?.also { origin[key] = it }
            }
            ?: Hash.EMPTY
    }

    fun collectDirties(): MerkleTreeDirtyNodes? {
        applyPending()
        return tree.collectDirties(false)
    }

    /** moves all dirty storage slots into the pending area to be hashed or committed later. It is invoked at the end of every transaction. */
    fun dirtyToPending(): Set<Hash> {
        return dirty.keys.asSequence()
            .onEach { pending[it] = dirty.remove(it)!! }
            .filter { pending[it] != origin[it] }
            .toSet()
    }

    fun applyPending(): Set<Hash> {
        dirtyToPending()
        if (pending.isEmpty()) return emptySet()
        return pending.keys.filter { k -> updateTree(k, origin[k], pending[k]!!) }.toSet()
    }

    private fun updateTree(key: Hash, prev: Hash?, next: Hash): Boolean {
        if (next == prev) return false

        if (next == Hash.EMPTY) tree -= key.bytes
        else tree[key.bytes] = RLPEncoder.encode { addBytes(next.bytes.dropWhile { it == 0.toByte() }.toByteArray()) }
        return true
    }
}
