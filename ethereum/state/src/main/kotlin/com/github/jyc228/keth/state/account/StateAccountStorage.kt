package com.github.jyc228.keth.state.account

import com.github.jyc228.keth.collections.MerkleTree
import com.github.jyc228.keth.collections.MerkleTreeDirtyNodes
import com.github.jyc228.keth.rlp.RLPEncoder
import com.github.jyc228.keth.state.Journal
import com.github.jyc228.keth.state.JournalEntry
import com.github.jyc228.keth.type.Address

class StateAccountStorage(
    private val journal: Journal,
    private val owner: Address,
    private val tree: MerkleTree,
    private val isDestruct: (owner: Address) -> Boolean
) : ManagedStateAccount.Storage {
    val rootHash get() = tree.rootHash()?.let(::StorageRoot)

    /** Storage cache of original entries to dedup rewrites, reset for every transaction */
    private val origin = mutableMapOf<Key, ByteArray>()

    /** Storage entries that need to be flushed to disk, at the end of an entire block */
    private val pending = mutableMapOf<Key, ByteArray>()

    /** Storage entries that have been modified in the current transaction execution */
    private val dirty = mutableMapOf<Key, ByteArray?>()

    override suspend fun set(key: ByteArray, value: ByteArray?) {
        val prev = get(key)
        if (!prev.contentEquals(value)) {
            journal.append { JournalEntry.StorageChange(owner, key, prev) }
            dirty[Key(key)] = value
        }
    }

    fun setDirty(key: ByteArray, value: ByteArray?) {
        dirty[Key(key)] = value
    }

    /** retrieves a value from the account storage trie. */
    override suspend fun get(key: ByteArray): ByteArray? = Key(key).let { dirty[it] ?: getCommittedState(it) }
    override suspend fun getCommittedState(key: ByteArray): ByteArray? = getCommittedState(Key(key))

    private fun getCommittedState(key: Key): ByteArray? {
        return pending[key]
            ?: origin[key]
            ?: when (isDestruct(owner)) {
                true -> null
                false -> tree[key.bytes]?.also { origin[key] = it }
            }
    }

    fun collectDirties(): MerkleTreeDirtyNodes? {
        applyPending()
        return tree.collectDirties(false)
    }

    /** moves all dirty storage slots into the pending area to be hashed or committed later. It is invoked at the end of every transaction. */
    fun dirtyToPending(): Set<ByteArray> {
        return dirty.keys.asSequence()
            .onEach { pending[it] = dirty.remove(it)!! }
            .filterNot { pending[it].contentEquals(origin[it]) }
            .map { it.bytes }
            .toSet()
    }

    fun applyPending(): Set<ByteArray> {
        dirtyToPending()
        if (pending.isEmpty()) return emptySet()
        return pending.keys.mapNotNull { k -> if (updateTree(k, origin[k], pending[k])) k.bytes else null }.toSet()
    }

    private fun updateTree(key: Key, prev: ByteArray?, next: ByteArray?): Boolean {
        if (next.contentEquals(prev)) return false
        if (next == null) tree -= key.bytes
        else tree[key.bytes] = RLPEncoder.encode { addBytes(next.dropWhile { it == 0.toByte() }.toByteArray()) }
        return true
    }

    private class Key(val bytes: ByteArray) {
        override fun equals(other: Any?): Boolean = this === other || this.bytes contentEquals (other as? Key)?.bytes
        override fun hashCode(): Int = bytes.contentHashCode()
        override fun toString(): String = bytes.contentToString()
    }
}
