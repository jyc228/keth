package ethereum.collections

interface MerkleTree {
    fun rootHash(): ByteArray?
    fun collectDirties(includeLeaf: Boolean): MerkleTreeDirtyNodes?
    operator fun get(key: ByteArray): ByteArray?
    operator fun set(key: ByteArray, value: ByteArray)
    operator fun minusAssign(key: ByteArray)

    companion object
}

interface MerkleTreeNode {
    val hash: ByteArray

    fun encode(collapse: Boolean = true): ByteArray
    fun forEachChildrenHash(callback: (hash: ByteArray) -> Unit)

    companion object
}
