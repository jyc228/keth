package ethereum.rlp

import java.nio.ByteBuffer

object RLPDecoder {
    fun decode(input: ByteArray): RLPItem = ByteBuffer.wrap(input).decode()

    private fun ByteBuffer.decode(): RLPItem = when (val prefix = get()) {
        in 128.toByte()..183.toByte() -> decodeString(prefix - 128.toByte())
        in 184.toByte()..191.toByte() -> decodeString(decodeSize(prefix - 183.toByte()))
        in 192.toByte()..247.toByte() -> decodeList(prefix - 192.toByte())
        in 248.toByte()..255.toByte() -> decodeList(decodeSize(prefix - 247.toByte()))
        else -> RLPItem.Byte(prefix)
    }

    private fun ByteBuffer.decodeString(size: Int): RLPItem.Str {
        return RLPItem.string(String((0..<size).map { get().toInt().toChar() }.toCharArray()))
    }

    private fun ByteBuffer.decodeList(size: Int): RLPItem.Arr {
        val endIndex = position() + size
        val result = generateSequence { position() }.takeWhile { it < endIndex }.map { decode() }.toList()
        return RLPItem.array(result)
    }

    private fun ByteBuffer.decodeSize(size: Int): Int {
        return (0 until size).joinToString("") { get().toHexString() }.toInt(16)
    }

    private fun Byte.toHexString() = toUByte().toString(16).padStart(2, '0')
}
