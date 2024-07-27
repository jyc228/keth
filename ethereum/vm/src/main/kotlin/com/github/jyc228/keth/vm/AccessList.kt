package com.github.jyc228.keth.vm

import com.github.jyc228.keth.type.Address

class AccessList(private val account: MutableMap<Address, MutableSet<Slot>?> = mutableMapOf()) {

    operator fun contains(address: Address): Boolean = address in account

    operator fun get(address: Address): MutableSet<Slot> {
        val slot = account[address] ?: mutableSetOf()
        if (account[address] == null) {
            account[address] = slot
        }
        return slot
    }

    operator fun plusAssign(address: Address) {
        account[address] = null
    }

    class Slot(private val bytes: ByteArray) {
        override fun equals(other: Any?): Boolean = this === other || this.bytes contentEquals (other as? Slot)?.bytes
        override fun hashCode(): Int = bytes.contentHashCode()
        override fun toString(): String = bytes.contentToString()
    }
}
