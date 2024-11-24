package com.github.jyc228.keth.state.account

class Address(val bytes: ByteArray) {
    @OptIn(ExperimentalStdlibApi::class)
    constructor(hexString: String) : this(hexString.removePrefix("0x").hexToByteArray())

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "0x${bytes.toHexString()}"
    override fun equals(other: Any?) = other is Address && bytes.contentEquals(other.bytes)
    override fun hashCode() = bytes.contentHashCode()

    companion object {
        val empty = Address(byteArrayOf())

        fun build(size: Int = 20, action: (ByteArray) -> Unit) = Address(ByteArray(size).apply(action))
    }
}