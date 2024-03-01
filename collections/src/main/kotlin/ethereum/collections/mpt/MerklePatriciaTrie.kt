package ethereum.collections.mpt

import ethereum.collections.MerkleTree
import ethereum.collections.MerkleTreeDirtyNodes
import ethereum.collections.MissingNodeError

class MerklePatriciaTrie(
    private var root: MerklePatriciaTrieNode?,
    private val findNodeByHash: (ByteArray) -> ByteArray?,
    private val hasher: NodeHasher = NodeHasher.newKeccak256(),
) : MerkleTree {
    override fun rootHash(): ByteArray? = computeRootHash()?.hash

    override fun collectDirties(includeLeaf: Boolean): MerkleTreeDirtyNodes? {
        return computeRootHash()?.let { root -> collectDirties(root, includeLeaf) }
    }

    private fun computeRootHash(): MerklePatriciaTrieNode? = root?.also { hasher.hash(it, true) }

    operator fun get(key: String): ByteArray? = get(key.toByteArray())
    operator fun set(key: String, value: String) = set(key.toByteArray(), value.toByteArray())

    override fun get(key: ByteArray): ByteArray? {
        val getResult = get(root, HexKey.fromByteArray(key), 0)
        if (getResult?.didResolved == true) {
            root = getResult.node
        }
        return getResult?.value
    }

    private fun get(
        node: MerklePatriciaTrieNode?,
        key: HexKey,
        depth: Int
    ): GetResult? = when (node) {
        null -> null
        is ValueNode -> GetResult(node.bytes, node, false)
        is HashNode -> get(resolveHashNode(node.hash, key[0..<depth]), key, depth)
        is ExtensionNode -> when (key.size - depth < node.key.size || !node.key.nibbles.contentEquals(key[depth..<depth + node.key.size].nibbles)) {
            true -> null
            false -> get(node.value, key, depth + node.key.size)
        }

        is BranchNode -> get(node[key[depth]], key, depth + 1)
    }

    override fun set(key: ByteArray, value: ByteArray) {
        root = when (value.isNotEmpty()) {
            true -> update(VisitPath(HexKey.empty(), HexKey.fromByteArray(key)), ValueNode(value), root)
            false -> delete(VisitPath(HexKey.empty(), HexKey.fromByteArray(key)), root)
        }
    }

    private fun update(
        path: VisitPath,
        value: MerklePatriciaTrieNode,
        node: MerklePatriciaTrieNode? = null,
    ): MerklePatriciaTrieNode {
        if (path.suffix.nibbles.isEmpty()) return value
        return when (node) {
            null -> ExtensionNode(path.suffix, value)
            is ValueNode -> error("invalid node: $node")
            is HashNode -> update(path, value, resolveHashNode(node.hash, path.prefix))

            is ExtensionNode -> {
                val forkIdx = path.suffix.findForkIndex(node.key)
                if (forkIdx == null) {
                    val newNode = update(path.divideSuffix(node.key.size), value, node.value)
                    return ExtensionNode(node.key, newNode)
                }
                val branch = BranchNode {
                    when (it) {
                        node.key[forkIdx] -> update(path.divideSuffix(forkIdx + 1, node.key), node.value)
                        path.suffix[forkIdx] -> update(path.divideSuffix(forkIdx + 1), value)
                        else -> null
                    }
                }
                if (forkIdx == 0) return branch
                return ExtensionNode(path.suffix[0..<forkIdx], branch)
            }

            is BranchNode -> node.also { it[path.suffix[0]] = update(path.divideSuffix(1), value, it[path.suffix[0]]) }
        }
    }

    override operator fun minusAssign(key: ByteArray) {
        root = delete(VisitPath(HexKey.empty(), HexKey.fromByteArray(key)), root)
    }

    private fun delete(path: VisitPath, node: MerklePatriciaTrieNode?): MerklePatriciaTrieNode? {
        return when (node) {
            null -> null
            is ValueNode -> null
            is HashNode -> delete(path, resolveHashNode(node.hash, path.prefix))
            is ExtensionNode -> {
                val forkIdx = path.suffix.findForkIndex(node.key)
                if (forkIdx == null || forkIdx <= node.key.lastIndex) {
                    return null
                }
                val newNode = delete(path.divideSuffix(node.key.size), node.value) ?: return node
                if (newNode is ExtensionNode) {
                    return ExtensionNode(node.key + newNode.key, newNode)
                }
                return ExtensionNode(node.key, newNode)
            }

            is BranchNode -> {
                node[path.suffix[0]] = delete(path.divideSuffix(1), node[path.suffix[0]])
                if (node[path.suffix[0]] != null) {
                    return node // 제거한 하위노드 자리가 null 이 아니면 2개 이상의 children 이 있다는 것이므로 ExtensionNode 로 변환하지 않음
                }
                val notnullNodeIdx = node.children.indexOfFirst { it != null }
                if (0 <= notnullNodeIdx && notnullNodeIdx < node.children.indexOfLast { it != null }) {
                    return node // n still contains at least two values and cannot be reduced.
                }
                var child = node[notnullNodeIdx]!!
                if (child is HashNode) {
                    child = resolveHashNode(child.hash)
                }
                if (child is ExtensionNode) {
                    return ExtensionNode(HexKey.just(notnullNodeIdx.toByte()) + child.key, child.value)
                }
                // n is replaced by a one-nibble short node containing the child.
                return ExtensionNode(HexKey.just(notnullNodeIdx.toByte()), child)
            }
        }
    }

    private fun resolveHashNode(hash: ByteArray, path: HexKey? = null): MerklePatriciaTrieNode {
        val nodeRlp = findNodeByHash(hash) ?: throw MissingNodeError()
        return MerklePatriciaTrieNode.decodeFromRlp(hash, nodeRlp)
    }

    class GetResult(
        val value: ByteArray,
        val node: MerklePatriciaTrieNode,
        val didResolved: Boolean
    )

    class VisitPath(var prefix: HexKey, var suffix: HexKey) {
        fun divideSuffix(index: Int, newSuffix: HexKey? = null): VisitPath {
            val suffix = newSuffix ?: suffix
            return VisitPath(prefix + suffix[0..<index], suffix[index..suffix.lastIndex])
        }

        override fun toString(): String = "key=[${prefix.nibbles.joinToString()} | ${suffix.nibbles.joinToString()}]"
    }

    override fun toString(): String = root?.toString() ?: "null root"

    companion object {
        fun empty(findNodeByHash: (ByteArray) -> ByteArray?) = MerklePatriciaTrie(null, findNodeByHash)
        fun fromHash(hash: ByteArray, findNodeByHash: (ByteArray) -> ByteArray?): MerklePatriciaTrie {
            val nodeRlp = findNodeByHash(hash) ?: throw MissingNodeError()
            return MerklePatriciaTrie(MerklePatriciaTrieNode.decodeFromRlp(hash, nodeRlp), findNodeByHash)
        }
    }
}
