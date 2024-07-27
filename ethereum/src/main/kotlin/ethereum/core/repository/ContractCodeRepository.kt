package ethereum.core.repository

import com.github.jyc228.keth.state.ContractCodeDatabase
import com.github.jyc228.keth.state.account.CodeHash
import ethereum.db.KeyValueDatabase

class ContractCodeRepository(private val db: KeyValueDatabase) : ContractCodeDatabase {
    override fun saveCode(codeHash: CodeHash, code: ByteArray) {
        db[codeHash.bytes] = code
    }

    override fun findCodeByCodeHash(codeHash: CodeHash): ByteArray? = db[codeHash.bytes]
}
