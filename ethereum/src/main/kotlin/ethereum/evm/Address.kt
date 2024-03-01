package ethereum.evm

import ethereum.collections.Hash
import ethereum.crypto.ECDSAPublicKey
import ethereum.rlp.RLPEncoder
import ethereum.toKeccak256
import java.nio.ByteBuffer
import java.util.HexFormat

class Address(val bytes: ByteArray) {
    val hash by lazy(LazyThreadSafetyMode.NONE) { Hash.keccak256FromBytes(bytes) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return bytes.contentEquals((other as Address).bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()
    override fun toString(): String = bytes.contentToString()

    companion object {
        const val LENGTH = 20
        val EMPTY = Address(ByteArray(LENGTH))
        val RIPEMD = Address(ByteArray(LENGTH).also { it[LENGTH - 1] = 3 })

        fun fromByte(byte: Byte): Address = Address(ByteArray(LENGTH).also { it[LENGTH - 1] = byte })
        fun fromHexString(input: String): Address = Address(HexFormat.of().parseHex(input.removePrefix("0x")))
        fun fromByteArray(v: ByteArray) = Address(v)
        fun fromBytes(vararg bytes: Byte) = Address(ByteBuffer.allocate(20).put(bytes).array())
        fun fromString(v: String): Address {
            val bytes = v.toByteArray()
            val address = Address(ByteArray(LENGTH))
            bytes.forEachIndexed { i, b -> address.bytes[LENGTH - bytes.size + i] = b }
            return address
        }

        fun fromPublicKey(key: ECDSAPublicKey): Address {
            return fromByteArray(key.bytes().drop(1).toByteArray().toKeccak256().drop(12).toByteArray())
        }

        fun fromPrivateKey() {

        }

        fun new(from: Address, nonce: ULong): Address {
            val data = RLPEncoder.encodeArray { addBytes(from.bytes).addULong(nonce) }
            return Address(Hash.keccak256FromBytes(data).bytes.copyOfRange(12, Hash.SIZE))
        }
    }
}