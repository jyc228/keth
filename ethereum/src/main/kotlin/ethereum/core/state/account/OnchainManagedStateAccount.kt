package ethereum.core.state.account

import ethereum.collections.Hash
import ethereum.core.repository.ContractCodeRepository
import ethereum.core.state.Journal
import ethereum.core.state.JournalEntry
import ethereum.evm.Address
import java.math.BigInteger

class OnchainManagedStateAccount(
    override val address: Address,
    override val storage: StateAccountStorage,
    private val journal: Journal,
    private val codeRepository: ContractCodeRepository,
    account: StateAccount,
) : ManagedStateAccount {
    override val root: Hash get() = storage.rootHash
    override var codeHash: Hash = account.codeHash
    override var nonce: ULong by journal.observable(account.nonce) { old, _ -> JournalEntry.NonceChange(address, old) }
    override var balance: BigInteger by journal.observable(account.balance) { old, new ->
        if (old != new) JournalEntry.BalanceChange(address, old)
        else if (empty) JournalEntry.TouchChange(address)
        else null
    }

    var code: ByteArray? = null

    override suspend fun getCode(): ByteArray? {
        if (code == null) {
            code = codeRepository.findCodeByCodeHash(codeHash)
        }
        return code
    }

    override suspend fun setCode(code: ByteArray?) {
        code ?: return
        journal.append { JournalEntry.CodeChange(address, this.code, codeHash.bytes) }
        this.code = code
        codeHash = Hash.keccak256FromBytes(code)
        dirtyCode = true
    }

    var suicided = false
    var deleted = false
    var dirtyCode = false

    val empty: Boolean get() = 0u.toULong() == nonce && balance == BigInteger.ZERO && codeHash == Hash.EMPTY_CODE

    override fun toString(): String = when (codeHash == Hash.EMPTY_CODE) {
        true -> "EOA  nonce: $nonce, balance: $balance"
        false -> "CA nonce: $nonce, balance: $balance,  root: $root"
    }

    companion object
}
