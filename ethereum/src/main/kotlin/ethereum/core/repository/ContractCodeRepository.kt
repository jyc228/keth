package ethereum.core.repository

import ethereum.core.state.account.CodeHash
import ethereum.db.KeyValueDatabase

class ContractCodeRepository(private val db: KeyValueDatabase) {
    fun saveCode(codeHash: CodeHash, code: ByteArray) {
        db[codeHash.bytes] = code
    }

    fun findCodeByCodeHash(codeHash: CodeHash): ByteArray? = db[codeHash.bytes]
}
