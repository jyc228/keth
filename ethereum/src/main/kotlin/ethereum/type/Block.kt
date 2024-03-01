package ethereum.type

import java.time.LocalDateTime

class Block(
    val header: BlockHeader,
    val body: BlockBody,

    val receivedAt: LocalDateTime? = null,
    val receivedFrom: Any? = null
) {
    val number = header.number.toString().toULong()
    val hash by lazy(LazyThreadSafetyMode.NONE) { header.hash }
    val size by lazy(LazyThreadSafetyMode.NONE) { 0 }

    override fun toString(): String {
        return "$number : ${hash.toHexString()}"
    }

    companion object
}