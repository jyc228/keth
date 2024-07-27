package com.github.jyc228.keth.state

import com.github.jyc228.keth.type.Address
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Journal {
    private var disable = false
    private val entries = mutableListOf<JournalEntry>()
    private val dirties = mutableMapOf<Address, Int>()

    private var lastRevisionId = -1
    private var revisions = mutableListOf<Revision>()

    fun <T> observable(
        initialValue: T,
        onChange: (old: T, new: T) -> JournalEntry?
    ): ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {
        override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
            if (disable) return
            onChange(oldValue, newValue)?.let(::addEntry)
        }
    }

    fun append(createEntry: () -> JournalEntry) {
        if (disable) return
        addEntry(createEntry())
    }

    private fun addEntry(e: JournalEntry) {
        entries += e
        val dirtyAddress = e.dirtyAddress ?: return
        dirties.compute(dirtyAddress) { _, v -> (v ?: 0) + 1 }
        if (e is JournalEntry.TouchChange && dirtyAddress.hex == "0000000000000000000000000000000000000003") { // RIPEMD address
            // Explicitly put it in the dirty-cache, which is otherwise generated from flattened journals.
            // dirty explicitly sets an address to dirty, even if the change entries would
            // otherwise suggest it as clean. This method is an ugly hack to handle the RIPEMD
            // precompile consensus exception.
            dirties[dirtyAddress]?.inc()
        }
    }

    fun snapshot(): Int {
        revisions += Revision(++lastRevisionId, entries.size)
        return lastRevisionId
    }

    fun revertSnapshot(id: Int, db: OnchainStateDatabase) {
        val revision = revisions.firstOrNull { it.id >= id } ?: return
        revisions = revisions.subList(0, revisions.indexOf(revision))
        for (idx in entries.lastIndex downTo revision.journalIndex) {
            val e = entries.removeAt(idx)
            e.revert(db)
            e.dirtyAddress?.let { dirties.computeIfPresent(it) { _, v -> if (v == 1) null else v - 1 } }
        }
    }

    fun disable(callback: () -> Unit) {
        disable = true
        callback()
        disable = false
    }

    fun dirtyAddresses(): Set<Address> = dirties.keys

    fun clear() {
        entries.clear()
        dirties.clear()
        revisions.clear()
        lastRevisionId = -1
    }

    override fun toString(): String {
        return "dirty account ${dirties.size}, entries ${entries.size}"
    }

    private data class Revision(val id: Int, val journalIndex: Int)
}
