package ethereum.core.repository

import ethereum.collections.Hash
import ethereum.db.KeyValueDatabase

class TreeRepository(
    private val database: KeyValueDatabase
) {
    fun readLegacyTrieNode(hash: Hash): ByteArray? = database[hash.bytes]

    fun writeLegacyTrieNode(hash: Hash, node: ByteArray) {
        database[hash.bytes] = node
    }

    fun hasAccountTrieNode(path: ByteArray, hash: Hash) {

    }

    fun readAccountTrieNode(path: ByteArray) {

    }

    fun writeAccountTrieNode(path: ByteArray, node: ByteArray) {

    }

    fun deleteAccountTrieNode(path: ByteArray) {

    }

    fun hasStorageTrieNode() {

    }

    fun readStorageTrieNode() {

    }

    fun writeStorageTrieNode() {

    }

    fun deleteStorageTrieNode() {

    }

    fun hasTrieNode(hash: Hash) {

    }

    fun readTrieNode(hash: Hash) {

    }

    fun writeTrieNode(hash: Hash, node: ByteArray) {

    }

    fun deleteTrieNode(hash: Hash) {

    }
}

