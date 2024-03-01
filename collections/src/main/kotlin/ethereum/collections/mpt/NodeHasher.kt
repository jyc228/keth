package ethereum.collections.mpt

import org.bouncycastle.jcajce.provider.digest.Keccak

class NodeHasher(
    private val computeHash: (rlp: ByteArray) -> ByteArray,
    private val rlpMinSize: Int
) {
    fun hash(node: MerklePatriciaTrieNode, force: Boolean) {
        if (node.hash.isEmpty() && (node as? AbstractMerklePatriciaTrieNode)?.dirty == true) when (node) {
            is ExtensionNode -> node.apply { hash(value, force) }.computeHash(force)
            is BranchNode -> node.forEach { _, child -> hash(child, false) }.computeHash(force)
        }
    }

    private fun AbstractMerklePatriciaTrieNode.computeHash(force: Boolean) {
        val rlp = this.encode()
        if (force || rlpMinSize <= rlp.size) {
            this.hash = computeHash(rlp)
        }
    }

    companion object {
        fun newKeccak256() = NodeHasher({ Keccak.Digest256().digest(it) }, 32)
    }
}
