package ethereum.core.state

import ethereum.collections.Hash
import ethereum.core.state.account.ManagedStateAccount
import ethereum.evm.Address
import java.math.BigInteger

sealed interface JournalEntry {
    val dirtyAddress: Address?
    fun revert(db: StateDatabaseImpl) {}

    data class CreateObjectChange(override val dirtyAddress: Address) : JournalEntry {
        override fun revert(db: StateDatabaseImpl) {
            db.accountTree.accountByAddress -= dirtyAddress
            db.accountTree.dirtyAddress -= dirtyAddress
        }
    }

    data class ResetObjectChange(
        override val dirtyAddress: Address,
        val prev: ManagedStateAccount,
        val prevdestruct: Boolean
    ) : JournalEntry {
        override fun revert(db: StateDatabaseImpl) {
            db.accountTree.accountByAddress[prev.address] = prev
//            if (!prevdestruct && db.snap != null) {
//                delete(s.snapDestructs, prev.addressHash)
//            }
        }
    }

    data class BalanceChange(override val dirtyAddress: Address, val prevAmount: BigInteger) : JournalEntry {
        override fun revert(db: StateDatabaseImpl) {
            db.accountTree[dirtyAddress]?.balance = prevAmount
        }
    }

    data class NonceChange(override val dirtyAddress: Address, val prevNonce: ULong) : JournalEntry {
        override fun revert(db: StateDatabaseImpl) {
            db.accountTree[dirtyAddress]?.nonce = prevNonce
        }
    }

    data class StorageChange(override val dirtyAddress: Address, val key: Hash, val prevValue: Hash) : JournalEntry {
        override fun revert(db: StateDatabaseImpl) {
            db.accountTree[dirtyAddress]?.storage?.dirty?.set(key, prevValue)
        }
    }

    data class TouchChange(override val dirtyAddress: Address) : JournalEntry

    data class CodeChange(
        override val dirtyAddress: Address,
        private val prevCode: ByteArray?,
        private val prevHash: ByteArray
    ) : JournalEntry
}
