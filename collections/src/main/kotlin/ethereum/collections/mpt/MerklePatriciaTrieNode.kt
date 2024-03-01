package ethereum.collections.mpt

import ethereum.collections.MerkleTreeNode

sealed interface MerklePatriciaTrieNode : MerkleTreeNode {
    override fun forEachChildrenHash(callback: (hash: ByteArray) -> Unit): Unit = when (this) {
        is ExtensionNode -> value.forEachChildrenHash(callback)
        is BranchNode -> forEach { _, child -> callback(child.hash) }.let { Unit }
        is HashNode -> Unit
        is ValueNode -> Unit
    }

    companion object
}

sealed class AbstractMerklePatriciaTrieNode : MerklePatriciaTrieNode {
    abstract override var hash: ByteArray
    var dirty: Boolean = true

    override fun encode(collapse: Boolean): ByteArray = encodeToRLP(collapse)
}

class BranchNode private constructor(
    val children: Array<MerklePatriciaTrieNode?>, // length == 17
    override var hash: ByteArray = emptyByteArray
) : AbstractMerklePatriciaTrieNode() {
    constructor(
        hash: ByteArray? = null,
        init: (Int) -> MerklePatriciaTrieNode?
    ) : this(Array(17, init), hash ?: emptyByteArray)

    fun forEach(callback: (Int, MerklePatriciaTrieNode) -> Unit): BranchNode {
        for (i in 0..15) {
            callback(i, children[i] ?: continue)
        }
        return this
    }

    operator fun get(idx: Int): MerklePatriciaTrieNode? = children[idx]
    operator fun set(idx: Int, child: MerklePatriciaTrieNode?) {
        children[idx] = child
        hash = emptyByteArray
    }

    override fun toString(): String {
        val children = children.asSequence()
            .withIndex()
            .filter { it.value != null }
            .joinToString { (i, v) -> "${i}:${v!!::class.simpleName?.first().toString()}" }
        return "B[${children}]"
    }
}

class ExtensionNode(
    var key: HexKey,
    var value: MerklePatriciaTrieNode,
    override var hash: ByteArray = emptyByteArray
) : AbstractMerklePatriciaTrieNode() {

    internal val compactKey by lazy(LazyThreadSafetyMode.NONE) { key.decodeToCompact() }
    override fun toString(): String = "E${key.nibbles.contentToString()} = $value"
}

class ValueNode(val bytes: ByteArray) : MerklePatriciaTrieNode {
    override val hash: ByteArray get() = emptyByteArray
    override fun encode(collapse: Boolean): ByteArray = emptyByteArray
    override fun toString(): String = "V${bytes.contentToString()}"
}

class HashNode(override val hash: ByteArray) : MerklePatriciaTrieNode {
    override fun encode(collapse: Boolean): ByteArray = error("")
    override fun toString(): String = "H${hash.contentToString()}"
}

private val emptyByteArray = byteArrayOf()
