package com.github.jyc228.keth.state

import com.github.jyc228.keth.collections.MerkleTreeDirtyNodes
import com.github.jyc228.keth.collections.MerkleTreeNode
import com.github.jyc228.keth.state.account.AccountRlp
import com.github.jyc228.keth.state.account.AddressHash
import com.github.jyc228.keth.state.account.CodeHash
import com.github.jyc228.keth.state.account.StateRoot
import com.github.jyc228.keth.type.Hash

interface TreeDatabase {
    fun node(hash: ByteArray): ByteArray?

    /**
     * inserts the dirty nodes in provided nodeset into database and link the account trie with multiple storage tries if necessary.
     */
    fun update(accountDirties: MerkleTreeDirtyNodes, storageDirties: Map<AddressHash, MerkleTreeDirtyNodes>)

    fun commit(hash: StateRoot)
}

interface ContractCodeDatabase {
    fun saveCode(codeHash: CodeHash, code: ByteArray)
    fun findCodeByCodeHash(codeHash: CodeHash): ByteArray?
}

class ContractCodeRepository(private val db: KeyValueDatabase) : ContractCodeDatabase {
    override fun saveCode(codeHash: CodeHash, code: ByteArray) {
        db[codeHash.bytes] = code
    }

    override fun findCodeByCodeHash(codeHash: CodeHash): ByteArray? = db[codeHash.bytes]
}

interface KeyValueDatabase {
    operator fun get(key: ByteArray): ByteArray?
    operator fun set(key: ByteArray, value: ByteArray)
    operator fun minusAssign(key: ByteArray)

    fun iterator(prefix: ByteArray? = null, start: ByteArray? = null): Iterator<Pair<ByteArray, ByteArray?>>
}

class InMemoryKeyValueDatabase : KeyValueDatabase {
    private val storage = HashMap<Key, ByteArray>()

    override fun get(key: ByteArray): ByteArray? = storage[Key(key)]
    override fun set(key: ByteArray, value: ByteArray) = run { storage[Key(key)] = value }
    override fun minusAssign(key: ByteArray) = run { storage -= Key(key) }

    override fun iterator(prefix: ByteArray?, start: ByteArray?): Iterator<Pair<ByteArray, ByteArray?>> {
        val startKey = start?.let(::Key)?.toString() ?: ""
        return storage.keys.asSequence()
            .filter { it.startsWith(prefix) }
            .map { it.toString() }
            .sorted()
            .dropWhile { it < startKey }
            .map { it.encodeToByteArray() to storage[Key(it.encodeToByteArray())] }
            .iterator()
    }

    private class Key(val bytes: ByteArray) {
        override fun equals(other: Any?): Boolean =
            this === other || other is Key && this.bytes contentEquals other.bytes

        override fun hashCode(): Int = bytes.contentHashCode()
        override fun toString(): String = bytes.map { it.toInt().toChar() }.joinToString("")
        fun startsWith(other: ByteArray?): Boolean {
            if (other == null) return true
            if (bytes.size < other.size) return false
            return other.withIndex().all { (i, v) -> bytes[i] == v }
        }
    }
}

class DefaultTreeDatabase(val db: KeyValueDatabase) : TreeDatabase {
    private val repository = TreeRepository(db)
    private var dirties: MutableMap<Hash, CachedNode> = mutableMapOf()
    private var oldest = Hash(byteArrayOf())
    private var newest = Hash(byteArrayOf())
    var dirtiesSize = 0
    var childrenSize: Int = 0

    override fun node(hash: ByteArray): ByteArray? {
        val dirty = dirties[Hash(hash)]
        if (dirty != null) {
            return dirty.node.encode(false)
        }
        return repository.readLegacyTrieNode(hash)
    }

    // Update inserts the dirty nodes in provided nodeset into database and
    // link the account trie with multiple storage tries if necessary.
    override fun update(accountDirties: MerkleTreeDirtyNodes, storageDirties: Map<AddressHash, MerkleTreeDirtyNodes>) {
        // Insert dirty nodes into the database. In the same tree, it must be
        // ensured that children are inserted first, then parent so that children
        // can be linked with their parent correctly.
        //
        // Note, the storage tries must be flushed before the account trie to
        // retain the invariant that children go into the dirty cache first.
        storageDirties.forEach { (_, dirties) ->
            dirties.forEachByPath { n -> n?.let { insert(n) } }
        }

        accountDirties.forEachByPath { n -> n?.let { insert(n) } }
        accountDirties.leaves.forEach {
            val account = AccountRlp.decode(it.data)
            if (account.root != null) {
                reference(Hash(account.root.bytes), Hash(it.hash))
            }
        }
    }

    // reference is the private locked version of Reference.
    private fun reference(child: Hash, parent: Hash) {
        val childNode = dirties[child] ?: return
        if (parent == Hash(byteArrayOf())) {
            childNode.parents++
            return
        }
        val parentNode = dirties[parent] ?: error("")
        if (child in parentNode.external) return
        childNode.parents++
        parentNode.external += child
    }

    // insert inserts a simplified trie node into the memory database.
    // All nodes inserted by this function will be reference tracked
    // and in theory should only used for **trie nodes** insertion.
    private fun insert(node: MerkleTreeNode) {
        if (Hash(node.hash) in dirties) return
        val entry = CachedNode(node, flushPrev = newest)
        entry.forEachChildren { child -> dirties[child]?.parents?.inc() }
        dirties[Hash(node.hash)] = entry

        if (oldest == Hash(byteArrayOf())) {
            oldest = Hash(node.hash)
        } else {
            dirties[newest]!!.flushNext = Hash(node.hash)
        }
        newest = Hash(node.hash)
        dirtiesSize += 32
    }

    override fun commit(hash: StateRoot) {
        commit(Hash(hash.bytes))
    }

    private fun commit(hash: Hash) {
        val node = dirties[hash] ?: return
        node.forEachChildren(::commit)
        repository.writeLegacyTrieNode(hash.bytes, node.node.encode(false))
//        node.children.forEach { (child, u) -> commit(child, callback) }
//        if (node.node is RawNode) {
//        }

//        db[hash] = node.node.encode().encodedData
    }

    private class CachedNode(
        val node: MerkleTreeNode,
        var flushPrev: Hash,
        var flushNext: Hash? = null
    ) {
        val external: MutableSet<Hash> = mutableSetOf()
        var parents: Int = 0

        fun forEachChildren(callback: (Hash) -> Unit) {
            external.forEach(callback)
            node.forEachChildrenHash { callback(Hash(it)) }
        }
    }

    private class Hash(val bytes: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Hash) return false
            return bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int = bytes.contentHashCode()
    }


    companion object {
        fun memory() = DefaultTreeDatabase(InMemoryKeyValueDatabase())
    }
}

class TreeRepository(
    private val database: KeyValueDatabase
) {
    fun readLegacyTrieNode(hash: ByteArray): ByteArray? = database[hash]

    fun writeLegacyTrieNode(hash: ByteArray, node: ByteArray) {
        database[hash] = node
    }

    fun hasAccountTrieNode(path: ByteArray, hash: Hash) {

    }

    fun readAccountTrieNode(path: ByteArray) {

    }

    fun writeAccountTrieNode(path: ByteArray, node: ByteArray) {

    }

    fun deleteAccountTrieNode(path: ByteArray) {

    }

    fun hasStorageTrieNode() {

    }

    fun readStorageTrieNode() {

    }

    fun writeStorageTrieNode() {

    }

    fun deleteStorageTrieNode() {

    }

    fun hasTrieNode(hash: Hash) {

    }

    fun readTrieNode(hash: Hash) {

    }

    fun writeTrieNode(hash: Hash, node: ByteArray) {

    }

    fun deleteTrieNode(hash: Hash) {

    }
}
