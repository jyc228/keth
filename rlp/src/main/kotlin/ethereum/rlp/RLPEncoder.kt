package ethereum.rlp

object RLPEncoder {

    fun encode(input: Byte): ByteArray = byteArrayOf(input)

    fun encode(input: String): ByteArray = encode(input.map { it.code.toByte() }.toByteArray())

    fun encode(input: ByteArray): ByteArray {
        if (input.size <= 55) {
            return byteArrayOf((128 + input.size).toByte(), *input)
        }
        val prefix = input.size.toHexString().chunked(2)
        return byteArrayOf(
            (183 + prefix.size).toByte(),
            *prefix.map { it.toByte(16) }.toByteArray(),
            *input
        )
    }

    fun encode(input: List<ByteArray>): ByteArray {
        val size = input.sumOf { it.size }
        if (size <= 55) {
            return byteArrayOf((192 + size).toByte(), *input.flatMap { it.toList() }.toByteArray())
        }
        val prefix = size.toHexString().chunked(2)
        return byteArrayOf(
            (247 + prefix.size).toByte(),
            *prefix.map { it.toUByte(16).toByte() }.toByteArray(),
            *input.flatMap { it.toList() }.toByteArray()
        )
    }

    fun encode(init: RLPBuilder.() -> Unit): ByteArray = RLPBuilder().apply(init).build()
    fun encodeArray(init: RLPBuilder.() -> Unit): ByteArray = RLPBuilder().apply(init).buildArray()

    private fun Int.toHexString() = toString(16).let { if (it.length % 2 == 0) it else "0$it" }
}
