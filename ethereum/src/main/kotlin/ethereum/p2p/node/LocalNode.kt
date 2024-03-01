package ethereum.p2p.node

import ethereum.p2p.node.record.ENREntry

class LocalNode(
    val id: ByteArray
) {
    var current: Node? = null
    val entries: MutableMap<String, ENREntry> = mutableMapOf()
    fun resolveNod(): Node {

        return current ?: error("")
    }

    fun sign(): Node {
        error("")
    }
}