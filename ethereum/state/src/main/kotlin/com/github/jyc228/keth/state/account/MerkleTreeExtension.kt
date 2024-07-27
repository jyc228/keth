package com.github.jyc228.keth.state.account

import com.github.jyc228.keth.collections.HashKeyMerkleTree
import com.github.jyc228.keth.collections.MerkleTree
import com.github.jyc228.keth.collections.MissingNodeError
import com.github.jyc228.keth.collections.mpt.HashNode
import com.github.jyc228.keth.collections.mpt.MerklePatriciaTrie
import com.github.jyc228.keth.collections.mpt.MerklePatriciaTrieNode
import com.github.jyc228.keth.collections.mpt.decodeFromRlp

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
