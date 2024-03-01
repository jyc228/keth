package ethereum.collections

import org.bouncycastle.jcajce.provider.digest.Keccak

class HashKeyMerkleTree private constructor(
    private val tree: MerkleTree,
    private val hash: (ByteArray) -> ByteArray
) : MerkleTree by tree {
    override operator fun get(key: ByteArray): ByteArray? = tree[hash(key)]
    override operator fun set(key: ByteArray, value: ByteArray) {
        tree[hash(key)] = value
    }

    override fun toString(): String = tree.toString()

    companion object {
        fun keccak256(tree: MerkleTree): HashKeyMerkleTree {
            if (tree is HashKeyMerkleTree) return tree
            return HashKeyMerkleTree(tree) { Keccak.Digest256().digest(it) }
        }
    }
}
