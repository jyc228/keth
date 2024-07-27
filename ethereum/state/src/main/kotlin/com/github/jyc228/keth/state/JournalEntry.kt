package com.github.jyc228.keth.state

import com.github.jyc228.keth.state.account.CodeHash
import com.github.jyc228.keth.state.account.OnchainManagedStateAccount
import com.github.jyc228.keth.type.Address
import java.math.BigInteger

sealed interface JournalEntry {
    val dirtyAddress: Address?
    fun revert(db: OnchainStateDatabase) {}

    data class CreateObjectChange(override val dirtyAddress: Address) : JournalEntry {
        override fun revert(db: OnchainStateDatabase) {
            db.accountTree.accountByAddress -= dirtyAddress
            db.accountTree.dirtyAddress -= dirtyAddress
        }
    }

    data class ResetObjectChange(
        override val dirtyAddress: Address,
        val prev: OnchainManagedStateAccount,
        val prevdestruct: Boolean
    ) : JournalEntry {
        override fun revert(db: OnchainStateDatabase) {
            db.accountTree.accountByAddress[prev.address] = prev
//            if (!prevdestruct && db.snap != null) {
//                delete(s.snapDestructs, prev.addressHash)
//            }
        }
    }

    data class BalanceChange(override val dirtyAddress: Address, val prevAmount: BigInteger) : JournalEntry {
        override fun revert(db: OnchainStateDatabase) {
            db.accountTree[dirtyAddress]?.balance = prevAmount
        }
    }

    data class NonceChange(override val dirtyAddress: Address, val prevNonce: ULong) : JournalEntry {
        override fun revert(db: OnchainStateDatabase) {
            db.accountTree[dirtyAddress]?.nonce = prevNonce
        }
    }

    class StorageChange(
        override val dirtyAddress: Address,
        val key: ByteArray,
        val prevValue: ByteArray?
    ) : JournalEntry {
        override fun revert(db: OnchainStateDatabase) {
            db.accountTree[dirtyAddress]?.storage?.setDirty(key, prevValue)
        }
    }

    data class TouchChange(override val dirtyAddress: Address) : JournalEntry

    data class CodeChange(
        override val dirtyAddress: Address,
        private val prevCode: ByteArray?,
        private val prevHash: CodeHash?
    ) : JournalEntry
}
