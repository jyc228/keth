package ethereum.db

class InMemoryKeyValueDatabase : KeyValueDatabase {
    private val storage = HashMap<Key, ByteArray>()

    override fun get(key: ByteArray): ByteArray? = storage[Key(key)]
    override fun set(key: ByteArray, value: ByteArray) = run { storage[Key(key)] = value }
    override fun minusAssign(key: ByteArray) = run { storage -= Key(key) }

    override fun iterator(prefix: ByteArray?, start: ByteArray?): Iterator<Pair<ByteArray, ByteArray?>> {
        val startKey = start?.let(::Key)?.toString() ?: ""
        return storage.keys.asSequence()
            .filter { it.startsWith(prefix) }
            .map { it.toString() }
            .sorted()
            .dropWhile { it < startKey }
            .map { it.encodeToByteArray() to storage[Key(it.encodeToByteArray())] }
            .iterator()
    }

    private class Key(val bytes: ByteArray) {
        override fun equals(other: Any?): Boolean =
            this === other || other is Key && this.bytes contentEquals other.bytes

        override fun hashCode(): Int = bytes.contentHashCode()
        override fun toString(): String = bytes.map { it.toInt().toChar() }.joinToString("")
        fun startsWith(other: ByteArray?): Boolean {
            if (other == null) return true
            if (bytes.size < other.size) return false
            return other.withIndex().all { (i, v) -> bytes[i] == v }
        }
    }
}