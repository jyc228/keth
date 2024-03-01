package ethereum.rlp

import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RLPBuilder {
    private val items = mutableListOf<ByteArray>()
    private var optionalIndex: Int? = null

    fun addBytes(input: ByteArray) = apply {
        items += if (input.size == 1) RLPEncoder.encode(input[0]) else RLPEncoder.encode(input)
    }

    fun addNull(optional: Boolean) =
        addEmptyString().also { if (optional && optionalIndex == null) optionalIndex = items.lastIndex }

    fun addEmptyString() = apply { items += RLPEncoder.encode(128.toByte()) }
    fun addByte(input: Byte) = apply { items += RLPEncoder.encode(input) }
    fun addString(input: String) = apply { items += RLPEncoder.encode(input) }
    fun addArray(init: RLPBuilder.() -> Unit) = apply { items += RLPBuilder().apply(init).buildArray() }

    fun addUInt(input: UInt) = apply {
        items += if (input == UInt.MIN_VALUE) RLPEncoder.encode(128.toByte())
        else if (input < 128u) RLPEncoder.encode(input.toByte())
        else RLPEncoder.encode(input.toString())
    }

    fun addULong(input: ULong) = apply {
        items += if (input == ULong.MIN_VALUE) RLPEncoder.encode(128.toByte())
        else if (input < 128u) RLPEncoder.encode(input.toByte())
        else RLPEncoder.encode(input.toBigEndian())
    }

    fun addBigInt(input: BigInteger): RLPBuilder {
        if (input.bitLength() <= 64) {
            return addULong(input.toString().toULong())
        }
        // Integer is larger than 64 bits, encode from i.Bits().
        // The minimal byte length is bitlen rounded up to the next
        // multiple of 8, divided by 8.
        val bytes = input.toByteArray()
        return addBytes(bytes.sliceArray(1..bytes.lastIndex))
    }

    internal fun build(): ByteArray = items.flatMap { it.toList() }.toByteArray()
    internal fun buildArray(): ByteArray {
        optionalIndex?.let {
            for (i in items.lastIndex downTo it) {
                when (items[i].size == 1 && items[i][0] == 128.toByte()) {
                    true -> items.removeAt(i)
                    false -> break
                }
            }
        }
        return RLPEncoder.encode(items)
    }

    override fun toString(): String {
        val rlp = items.flatMap { bytes -> bytes.map { it.toUByte() } }
        return rlp.joinToString(" ", prefix = "size : ${rlp.size} [", postfix = "]")
    }

    private fun ULong.toBigEndian() = ByteBuffer.allocate(ULong.SIZE_BYTES)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putLong(toLong())
        .position(0)
        .limit(littleEndianBitCount(this))
        .run { ByteArray(limit()).also(::get).reversedArray() }


    private fun littleEndianBitCount(i: ULong): Int {
        var idx = 0uL
        while (i < idx shr 8) idx++
        return idx.toInt() + 1
    }
}
