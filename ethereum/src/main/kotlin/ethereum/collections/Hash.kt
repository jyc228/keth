package ethereum.collections

import java.nio.ByteBuffer
import java.util.HexFormat
import org.bouncycastle.jcajce.provider.digest.Keccak

class Hash(val bytes: ByteArray) {
    val size get() = bytes.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return bytes.contentEquals((other as Hash).bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()
    override fun toString(): String = bytes.map { it.toUByte() }.joinToString(", ", prefix = "[", postfix = "]")

    fun toHexString() = "0x${HexFormat.of().formatHex(bytes)}"

    companion object {
        const val SIZE = 32
        val EMPTY = Hash(ByteArray(SIZE))
        val EMPTY_MPT_ROOT = fromHexString("56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421")
        val EMPTY_TX_HASH = fromHexString("56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421")
        val EMPTY_RECEIPT_HASH = fromHexString("56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421")
        val EMPTY_WITHDRAWAL_HASH = fromHexString("56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421")
        val EMPTY_UNCLE_HASH = fromHexString("1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347")
        val EMPTY_CODE = keccak256FromBytes(byteArrayOf())

        fun fromString(input: String): Hash = Hash(input.map { it.code.toByte() }.toByteArray())

        fun fromHexString(input: String): Hash = Hash(HexFormat.of().parseHex(input))
        fun fromBytes(vararg input: Byte): Hash = fromByteArray(input)
        fun fromByteArray(input: ByteArray): Hash {
            if (input.size != 32) {
                return Hash(ByteBuffer.allocate(32).put(input).array())
            }
            if (input.contentEquals(EMPTY_MPT_ROOT.bytes)) {
                return EMPTY_TX_HASH
            }
            return Hash(input)
        }

        fun keccak256FromBytes(input: ByteArray) = Hash(Keccak.Digest256().digest(input))

        fun new(callback: ByteArray.() -> Unit) = Hash(ByteArray(32).apply(callback))
    }
}