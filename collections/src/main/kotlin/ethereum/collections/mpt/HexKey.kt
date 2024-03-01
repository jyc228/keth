package ethereum.collections.mpt

import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.min

@JvmInline
value class HexKey(val nibbles: ByteArray) {
    val size get() = nibbles.size
    val lastIndex get() = nibbles.lastIndex
    val isEmpty get() = nibbles.isEmpty()
    val hasTerminator get() = nibbles.lastOrNull() == 16.toByte()

    operator fun plus(other: HexKey): HexKey = HexKey(nibbles + other.nibbles)
    operator fun get(index: Int): Int = nibbles[index].toInt()
    operator fun get(intRange: IntRange): HexKey = HexKey(nibbles.sliceArray(intRange))

    fun findForkIndex(other: HexKey): Int? {
        return (0..min(nibbles.lastIndex, other.nibbles.lastIndex)).firstOrNull { nibbles[it] != other.nibbles[it] }
    }

    private fun decodeNibbles(nibbles: List<Byte>): List<Int> = buildList {
        for (i in nibbles.indices step 2) {
            this += nibbles[i].toInt() shl 4 or nibbles[i + 1].toInt()
        }
    }

    fun decodeToCompact(): ByteArray {
        var nibbles = excludeTerminator()
        return ByteArray(nibbles.size / 2 + 1).also {
            it[0] = ((this.nibbles.size - nibbles.size) shl 5).toByte() // the terminator flag byte
            if (nibbles.size and 1 == 1) {
                it[0] = it[0] or (1 shl 4).toByte() // odd flag
                it[0] = it[0] or nibbles[0] // first nibble is contained in the first byte
                nibbles = nibbles.sliceArray(1..nibbles.lastIndex)
            }
            for (i in nibbles.indices step 2) {
                it[i / 2 + 1] = (nibbles[i].toInt() shl 4 or nibbles[i + 1].toInt()).toByte()
            }
        }
    }

    fun decodeToByteArray() = ByteArray(nibbles.size / 2).also {
        val nibbles = excludeTerminator()
        for (i in nibbles.indices step 2) {
            it[i / 2] = (nibbles[i].toInt() shl 4 or nibbles[i + 1].toInt()).toByte()
        }
    }

    private fun excludeTerminator() = when (hasTerminator) {
        true -> nibbles.sliceArray(nibbles.indices.first..<nibbles.indices.last)
        false -> nibbles
    }

    companion object {
        fun empty() = HexKey(byteArrayOf())
        fun just(vararg b: Byte) = HexKey(b)

        fun fromString(v: String) = fromByteArray(v.toByteArray())
        fun fromBytes(vararg b: Byte) = fromByteArray(b)
        fun fromByteArray(bytes: ByteArray): HexKey {
            val nibbles = ByteArray(bytes.size * 2 + 1)
            bytes.forEachIndexed { index, b ->
                nibbles[index * 2] = (b.toUByte() / 16u).toByte()
                nibbles[index * 2 + 1] = (b.toUByte() % 16u).toByte()
            }
            nibbles[nibbles.lastIndex] = 16
            return HexKey(nibbles)
        }

        fun fromCompact(compact: ByteArray): HexKey {
            var nibble = fromByteArray(compact).nibbles
            if (nibble[0] < 2) {
                // delete terminator flag
                nibble = nibble.sliceArray(0..<nibble.lastIndex)
            }
            // apply odd flag
            val chop = 2 - (nibble[0] and 1)
            return HexKey(nibble.sliceArray(chop..nibble.lastIndex))
        }
    }
}