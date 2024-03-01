package ethereum.p2p.node.record

import ethereum.rlp.RLPEncoder
import ethereum.rlp.rlpToObject
import ethereum.rlp.toRlp
import java.util.SortedMap
import kotlin.reflect.full.companionObjectInstance

// Record represents a node record. The zero value is an empty record.

class Record(
    private var signature: ByteArray? = null, // the signature
    private var raw: ByteArray? = null, // RLP encoded record
    private val entryBlobById: SortedMap<String, ByteArray> = sortedMapOf()
) {
    var seq: ULong = 0u // sequence number
        private set(value) {
            field = value; signature = null; raw = null
        }

    fun setSig(scheme: IdentityScheme, sig: ByteArray) {
        scheme.verify(this, sig)
        this.signature = sig
        this.raw = RLPEncoder.encodeArray {
            addBytes(sig)
            addULong(seq)
            entryBlobById.forEach { (k, v) ->
                addString(k)
                addBytes(v)
            }
        }
    }

    fun withEntry(entry: ENREntry): Record = apply { this += entry }

    operator fun plusAssign(entry: ENREntry) {
        if (signature != null) seq++
        entryBlobById[(entry::class.companionObjectInstance as ENREntry.Key<*>).id] = entry.toRlp()
    }

    operator fun <T : ENREntry> get(key: ENREntry.Key<T>): T? {
        return entryBlobById[key.id]?.rlpToObject(key.clazz)
    }

    companion object {
        fun fromEntries(vararg entry: ENREntry) = Record().apply { entry.forEach { this += it } }
    }
}