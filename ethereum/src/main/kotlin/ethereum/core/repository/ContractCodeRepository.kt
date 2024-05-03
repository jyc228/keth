package ethereum.core.repository

import ethereum.db.KeyValueDatabase
import io.github.jyc228.ethereum.state.ContractCodeDatabase
import io.github.jyc228.ethereum.state.account.CodeHash

class ContractCodeRepository(private val db: KeyValueDatabase) : ContractCodeDatabase {
    override fun saveCode(codeHash: CodeHash, code: ByteArray) {
        db[codeHash.bytes] = code
    }

    override fun findCodeByCodeHash(codeHash: CodeHash): ByteArray? = db[codeHash.bytes]
}
