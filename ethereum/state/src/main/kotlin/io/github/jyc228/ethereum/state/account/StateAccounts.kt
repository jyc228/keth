package io.github.jyc228.ethereum.state.account

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.state.ContractCodeDatabase
import io.github.jyc228.ethereum.state.Journal
import io.github.jyc228.ethereum.state.JournalEntry
import java.math.BigInteger

data class ImmutableStateAccount(
    override val nonce: ULong,
    override val balance: BigInteger,
    override val root: StorageRoot?,
    override val codeHash: CodeHash?
) : StateAccount

class OnchainManagedStateAccount(
    override val address: Address,
    override val storage: StateAccountStorage,
    private val journal: Journal,
    private val codeRepository: ContractCodeDatabase,
    account: StateAccount,
) : ManagedStateAccount {
    override val root: StorageRoot? get() = storage.rootHash
    override var codeHash: CodeHash? = account.codeHash
    override var nonce: ULong by journal.observable(account.nonce) { old, _ -> JournalEntry.NonceChange(address, old) }
    override var balance: BigInteger by journal.observable(account.balance) { old, new ->
        if (old != new) JournalEntry.BalanceChange(address, old)
        else if (empty) JournalEntry.TouchChange(address)
        else null
    }

    var code: ByteArray? = null
        private set

    override suspend fun getCode(): ByteArray? {
        if (code == null && codeHash != null) {
            code = codeRepository.findCodeByCodeHash(codeHash!!)
        }
        return code
    }

    override suspend fun setCode(code: ByteArray?) {
        code ?: return
        journal.append { JournalEntry.CodeChange(address, this.code, codeHash) }
        this.code = code
        codeHash = CodeHash.keccak256FromBytes(code)
        dirtyCode = true
    }

    var suicided = false
    var deleted = false
    var dirtyCode = false

    val empty: Boolean get() = 0u.toULong() == nonce && balance == BigInteger.ZERO && codeHash == null

    override fun toString(): String = when (codeHash == null) {
        true -> "EOA  nonce: $nonce, balance: $balance"
        false -> "CA nonce: $nonce, balance: $balance,  root: $root"
    }

    companion object
}
