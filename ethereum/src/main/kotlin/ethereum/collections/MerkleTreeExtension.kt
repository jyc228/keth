package ethereum.collections

import ethereum.collections.mpt.HashNode
import ethereum.collections.mpt.MerklePatriciaTrie
import ethereum.collections.mpt.MerklePatriciaTrieNode
import ethereum.collections.mpt.decodeFromRlp

fun MerkleTree.Companion.fromRootState(hash: ByteArray?, findNodeByHash: (ByteArray) -> ByteArray?): MerkleTree {
    if (hash == null) {
        return new(null, findNodeByHash)
    }
    val nodeData = findNodeByHash(hash) ?: throw MissingNodeError()
    return new(MerklePatriciaTrieNode.decodeFromRlp(hash, nodeData), findNodeByHash)
}

fun MerkleTree.Companion.lazyFromRootState(hash: ByteArray?, findNodeByHash: (ByteArray) -> ByteArray?): MerkleTree {
    if (hash == null) {
        return new(null, findNodeByHash)
    }
    return new(HashNode(hash), findNodeByHash)
}

fun MerkleTree.Companion.new(root: MerklePatriciaTrieNode?, findNodeByHash: (ByteArray) -> ByteArray?): MerkleTree {
    return HashKeyMerkleTree.keccak256(MerklePatriciaTrie(root, findNodeByHash))
}
