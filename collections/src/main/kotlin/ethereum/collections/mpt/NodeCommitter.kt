package ethereum.collections.mpt

import ethereum.collections.MerkleTreeDirtyNodes

fun collectDirties(node: MerklePatriciaTrieNode, collectLeaf: Boolean): MerkleTreeDirtyNodes? {
    if ((node as? AbstractMerklePatriciaTrieNode)?.dirty == true) {
        return MerkleTreeDirtyNodes().apply { addMPTDirtyNode(HexKey.empty(), node, collectLeaf) }
    }
    return null
}

fun MerkleTreeDirtyNodes.addMPTDirtyNode(path: HexKey, node: MerklePatriciaTrieNode, collectLeaf: Boolean) {
    if ((node as? AbstractMerklePatriciaTrieNode)?.dirty == true) {
        addMiddleNode(path.nibbles, node)
        when (node) {
            is ExtensionNode -> {
                addMPTDirtyNode(path + node.key, node.value, collectLeaf)
                if (collectLeaf && node.value is ValueNode) {
                    addLeafNode(node.hash, (node.value as ValueNode).bytes)
                }
            }

            is BranchNode -> node.forEach { i, child ->
                addMPTDirtyNode(path + HexKey.just(i.toByte()), child, collectLeaf)
            }
        }
    }
}
