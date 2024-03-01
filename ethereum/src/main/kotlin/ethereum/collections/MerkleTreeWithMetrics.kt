package ethereum.collections

class MerkleTreeWithMetrics(
    private val self: MerkleTree,
    val measureExecutionTime: Boolean = false
) : MerkleTree by self {
    var update = 0
    var delete = 0

    override fun set(key: ByteArray, value: ByteArray) {
        self[key] = value
        update++
    }

    override fun minusAssign(key: ByteArray) {
        self -= key
        delete++
    }
}