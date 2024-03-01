package ethereum.p2p.node.record

import java.math.BigInteger
import kotlin.reflect.KClass

/**
 * https://eips.ethereum.org/EIPS/eip-778
 */
interface ENREntry {
    abstract class Key<ENTRY : ENREntry>(val id: String, val clazz: KClass<ENTRY>) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Key<*>) return false
            return id == other.id
        }

        override fun hashCode(): Int = id.hashCode()
        override fun toString(): String = id
    }
}

abstract class ULongENREntry(val value: ULong) : ENREntry {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ULongENREntry) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

abstract class BytesENREntry(val bytes: ByteArray) : ENREntry {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BytesENREntry) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

class TcpENREntry(value: ULong) : ULongENREntry(value) {
    companion object : ENREntry.Key<TcpENREntry>("tcp", TcpENREntry::class)
}

class Tcp6ENREntry(value: ULong) : ULongENREntry(value) {
    companion object : ENREntry.Key<Tcp6ENREntry>("tcp6", Tcp6ENREntry::class)
}

class UdpENREntry(value: ULong) : ULongENREntry(value) {
    companion object : ENREntry.Key<UdpENREntry>("udp", UdpENREntry::class)
}

class Udp6ENREntry(value: ULong) : ULongENREntry(value) {
    companion object : ENREntry.Key<Udp6ENREntry>("udp6", Udp6ENREntry::class)
}

class IdENREntry(bytes: ByteArray) : BytesENREntry(bytes) {
    constructor(id: String) : this(id.toByteArray())

    override fun toString(): String = "$id ${bytes.decodeToString()}"

    companion object : ENREntry.Key<IdENREntry>("id", IdENREntry::class)
}

class IPv4ENREntry(address: ByteArray) : BytesENREntry(address) {
    constructor(address: String) : this(address.split(".").map { it.toUByte().toByte() }.toByteArray())

    override fun toString(): String = "$id ${bytes.joinToString(".") { it.toUByte().toString() }}"

    companion object : ENREntry.Key<IPv4ENREntry>("ip", IPv4ENREntry::class)
}

class IPv6ENREntry(address: ByteArray) : BytesENREntry(address) {
    constructor(address: String) : this(address.split(".").map { it.toUByte().toByte() }.toByteArray())

    override fun toString(): String = "$id ${bytes.joinToString(".") { it.toUByte().toString() }}"

    companion object : ENREntry.Key<IPv6ENREntry>("ip6", IPv6ENREntry::class)
}

class Secp256k1ENREntry(val x: BigInteger, val y: BigInteger) : ENREntry {
    companion object : ENREntry.Key<Secp256k1ENREntry>("secp256k1", Secp256k1ENREntry::class)
}