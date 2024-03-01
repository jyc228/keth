package ethereum.db

interface KeyValueDatabase {
    operator fun get(key: ByteArray): ByteArray?
    operator fun set(key: ByteArray, value: ByteArray)
    operator fun minusAssign(key: ByteArray)

    fun iterator(prefix: ByteArray? = null, start: ByteArray? = null): Iterator<Pair<ByteArray, ByteArray?>>
}