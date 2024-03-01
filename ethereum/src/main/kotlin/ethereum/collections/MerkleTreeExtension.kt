package ethereum.collections

import ethereum.collections.mpt.HashNode
import ethereum.collections.mpt.MerklePatriciaTrie
import ethereum.collections.mpt.MerklePatriciaTrieNode
import ethereum.collections.mpt.decodeFromRlp

fun MerkleTree.Companion.fromEmptyState(findNodeByHash: ((Hash) -> ByteArray?)? = null): MerkleTree {
    return new(null, findNodeByHash ?: { null })
}

fun MerkleTree.Companion.fromRootState(hash: Hash, findNodeByHash: (Hash) -> ByteArray?): MerkleTree {
    if (hash == Hash.EMPTY || hash == Hash.EMPTY_MPT_ROOT) {
        return fromEmptyState(findNodeByHash)
    }
    val nodeData = findNodeByHash(hash) ?: throw MissingNodeError()
    return new(MerklePatriciaTrieNode.decodeFromRlp(hash.bytes, nodeData), findNodeByHash)
}

fun MerkleTree.Companion.lazyFromRootState(hash: Hash?, findNodeByHash: (Hash) -> ByteArray?): MerkleTree {
    if (hash == null || hash == Hash.EMPTY || hash == Hash.EMPTY_MPT_ROOT) {
        return fromEmptyState(findNodeByHash)
    }
    return new(HashNode(hash.bytes), findNodeByHash)
}

fun MerkleTree.Companion.new(root: MerklePatriciaTrieNode?, findNodeByHash: (Hash) -> ByteArray?): MerkleTree {
    return HashKeyMerkleTree.keccak256(MerklePatriciaTrie(root, { findNodeByHash(Hash(it)) }))
}
