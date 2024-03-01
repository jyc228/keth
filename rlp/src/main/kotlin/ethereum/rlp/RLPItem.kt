package ethereum.rlp

import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed interface RLPItem {
    fun castByte() = this as Byte
    fun castStr() = this as Str
    fun castArr() = this as Arr

    fun toULong(): ULong = error("")
    fun toBigInt(): BigInteger = error("")

    class Byte(val value: kotlin.Byte) : RLPItem {
        override fun toULong(): ULong = value.toULong()
        override fun toBigInt(): BigInteger = value.toLong().toBigInteger()
    }

    open class Str(val value: String) : RLPItem {
        override fun toULong(): ULong {
            val buffer = ByteBuffer.allocate(ULong.SIZE_BYTES)
            value.indices.reversed().forEach { buffer.put(value[it].code.toByte()) }
            return buffer.order(ByteOrder.LITTLE_ENDIAN).position(0).getLong().toULong()
        }

        override fun toBigInt(): BigInteger {
            val buffer = ByteBuffer.allocate(ULong.SIZE_BYTES)
            value.indices.reversed().forEach { buffer.put(value[it].code.toByte()) }
            return buffer.order(ByteOrder.LITTLE_ENDIAN).position(0).getLong().toBigInteger()
        }
    }

    object EmptyStr : Str("") {
        override fun toString(): String = ""
    }

    open class Arr(value: List<RLPItem>) : RLPItem, List<RLPItem> by value

    object EmptyArr : Arr(emptyList()) {
        override fun toString(): String = "[]"
    }

    companion object {
        fun string(value: String): Str = if (value.isEmpty()) EmptyStr else Str(value)
        fun array(value: List<RLPItem>): Arr = if (value.isEmpty()) EmptyArr else Arr(value)
    }
}