package ethereum.core.database

import ethereum.collections.MerkleTreeDirtyNodes
import ethereum.collections.MerkleTreeNode
import ethereum.core.repository.TreeRepository
import ethereum.db.InMemoryKeyValueDatabase
import ethereum.db.KeyValueDatabase
import ethereum.type.fromStateRoot
import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.state.account.AccountRlp
import io.github.jyc228.ethereum.state.account.AddressHash
import io.github.jyc228.ethereum.state.account.StateRoot

class TreeDatabase(val db: KeyValueDatabase) : io.github.jyc228.ethereum.state.TreeDatabase {
    private val repository = TreeRepository(db)
    private var dirties: MutableMap<Hash, CachedNode> = mutableMapOf()
    var oldest = Hash.unsafe("")
    var newest = Hash.unsafe("")
    var dirtiesSize = 0
    var childrenSize: Int = 0

    override fun node(hash: ByteArray): ByteArray? {
        val dirty = dirties[Hash.fromByteArray(hash)]
        if (dirty != null) {
            return dirty.node.encode(false)
        }
        return repository.readLegacyTrieNode(Hash.fromByteArray(hash))
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
                reference(Hash.fromByteArray(account.root!!.bytes), Hash.fromByteArray(it.hash))
            }
        }
    }

    // reference is the private locked version of Reference.
    private fun reference(child: Hash, parent: Hash) {
        val childNode = dirties[child] ?: return
        if (parent == Hash.unsafe("")) {
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
        if (Hash.fromByteArray(node.hash) in dirties) return
        val entry = CachedNode(node, flushPrev = newest)
        entry.forEachChildren { child -> dirties[child]?.parents?.inc() }
        dirties[Hash.fromByteArray(node.hash)] = entry

        if (oldest == Hash.unsafe("")) {
            oldest = Hash.fromByteArray(node.hash)
        } else {
            dirties[newest]!!.flushNext = Hash.fromByteArray(node.hash)
        }
        newest = Hash.fromByteArray(node.hash)
        dirtiesSize += 32
    }

    override fun commit(hash: StateRoot) = commit(Hash.fromStateRoot(hash))

    private fun commit(hash: Hash) {
        val node = dirties[hash] ?: return
        node.forEachChildren(::commit)
        repository.writeLegacyTrieNode(hash, node.node.encode(false))
//        node.children.forEach { (child, u) -> commit(child, callback) }
//        if (node.node is RawNode) {
//        }

//        db[hash] = node.node.encode().encodedData
    }

    class CachedNode(
        val node: MerkleTreeNode,
        var flushPrev: Hash,
        var flushNext: Hash? = null
    ) {
        val external: MutableSet<Hash> = mutableSetOf()
        var parents: Int = 0

        fun forEachChildren(callback: (Hash) -> Unit) {
            external.forEach(callback)
            node.forEachChildrenHash { callback(Hash.fromByteArray(it)) }
        }
    }


    companion object {
        fun memory() = TreeDatabase(InMemoryKeyValueDatabase())
    }
}

