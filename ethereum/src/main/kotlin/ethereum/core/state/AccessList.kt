package ethereum.core.state

import ethereum.collections.Hash
import ethereum.evm.Address

class AccessList(
    private val slotIndexByAddress: MutableMap<Address, Int> = mutableMapOf(),
    val slots: MutableList<Set<Hash>> = mutableListOf()
) {
    operator fun contains(address: Address): Boolean = address in slotIndexByAddress
    operator fun get(address: Address): Set<Hash> = slotIndexByAddress[address]?.let { slots[it] } ?: emptySet()
    operator fun plusAssign(address: Address) {

    }
}