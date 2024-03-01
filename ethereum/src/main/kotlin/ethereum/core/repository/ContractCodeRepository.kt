package ethereum.core.repository

import ethereum.collections.Hash
import ethereum.db.KeyValueDatabase

class ContractCodeRepository(private val db: KeyValueDatabase) {
    fun saveCode(codeHash: Hash, code: ByteArray) {
        db[codeHash.bytes] = code
    }

    fun findCodeByCodeHash(codeHash: Hash): ByteArray? = db[codeHash.bytes]
}
