package ethereum.core.state.account

import ethereum.collections.Hash
import ethereum.core.repository.ContractCodeRepository
import ethereum.core.state.Journal
import ethereum.core.state.JournalEntry
import ethereum.evm.Address
import ethereum.type.Account
import ethereum.type.MutableAccount
import java.math.BigInteger

class StateAccount(
    val address: Address,
    val storage: StateAccountStorage,
    private val journal: Journal,
    private val codeRepository: ContractCodeRepository,
    account: Account,
) : MutableAccount {
    override val root: Hash get() = storage.rootHash
    override var codeHash: Hash = account.codeHash
    override var nonce: ULong by journal.observable(account.nonce) { old, _ -> JournalEntry.NonceChange(address, old) }
    override var balance: BigInteger by journal.observable(account.balance) { old, new ->
        if (old != new) JournalEntry.BalanceChange(address, old)
        else if (empty) JournalEntry.TouchChange(address)
        else null
    }

    var suicided = false
    var deleted = false
    var dirtyCode = false

    val empty: Boolean get() = 0u.toULong() == nonce && balance == BigInteger.ZERO && codeHash == Hash.EMPTY_CODE

    val codeSize: Int get() = code?.size ?: 0
    var code: ByteArray? = null
        get() {
            if (field == null && codeHash != Hash.EMPTY_CODE) {
                field = codeRepository.findCodeByCodeHash(codeHash)
            }
            return field
        }
        set(value) {
            value ?: return
            journal.append { JournalEntry.CodeChange(address, field, codeHash.bytes) }
            field = value
            codeHash = Hash.keccak256FromBytes(value)
            dirtyCode = true
        }

    override fun toString(): String = when (codeHash == Hash.EMPTY_CODE) {
        true -> "CA  nonce: $nonce, balance: $balance, root: $root"
        false -> "EOA nonce: $nonce, balance: $balance"
    }

    companion object
}
