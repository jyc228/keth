package ethereum.collections

class MerkleTreeDirtyNodes(
    // the set of updated nodes(newly inserted, updated)
    val middleByPath: MutableMap<ByteArray, MerkleTreeNode?> = mutableMapOf(),
    // the list of dirty leaves
    val leaves: MutableList<Leaf> = mutableListOf()
) {
    var updates = 0
    var deletes = 0

    fun addMiddleNode(path: ByteArray, element: MerkleTreeNode?) {
        if (element == null || element.hash.isEmpty()) {
            middleByPath[path] = null
            deletes++
        } else {
            middleByPath[path] = element
            updates++
        }
    }

    fun addLeafNode(hash: ByteArray, data: ByteArray) {
        leaves += Leaf(hash, data)
    }

    fun forEachByPath(callback: (MerkleTreeNode?) -> Unit) {
        middleByPath.keys.sortedByDescending { it.toString() }.forEach { path -> callback(middleByPath[path]) }
    }

    fun merge(other: MerkleTreeDirtyNodes): MerkleTreeDirtyNodes {
        other.middleByPath.forEach { (p, n) ->
            if (p in middleByPath) when (middleByPath[p] == null) {
                true -> deletes--
                false -> updates--
            }
            addMiddleNode(p, n)
        }
        return this
    }

    class Leaf(val hash: ByteArray, val data: ByteArray) {
        override fun toString(): String {
            return "key=[${toUByteString(hash)}]\nval=[${toUByteString(data)}]"
        }

        private fun toUByteString(bytes: ByteArray): String {
            return bytes.joinToString(", ") { it.toUByte().toString() }
        }
    }
}
