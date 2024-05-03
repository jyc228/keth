package ethereum.evm.state

import ethereum.core.database.TreeDatabase
import ethereum.core.repository.ContractCodeRepository
import io.github.jyc228.ethereum.state.OnchainStateDatabase
import io.github.jyc228.ethereum.state.account.Address
import io.github.jyc228.ethereum.state.account.OnchainManagedStateAccount
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import java.math.BigInteger
import org.junit.jupiter.api.Test

class StateDatabaseTest {
    val emptyDB = {
        val db = TreeDatabase.memory()
        OnchainStateDatabase.empty(db, ContractCodeRepository(db.db))
    }

    @Test
    fun `touch delete`() = runBlocking {
        var db = emptyDB()
        db.accountTree.create(Address.EMPTY)
        db.commit(false)
        db = OnchainStateDatabase.from(db)
        val snapshot = db.snapshot()
        db.withAccountOrCreate(Address.EMPTY) { it.balance += BigInteger.ZERO }
        db.revertSnapshot(snapshot)
    }

    @Test
    fun `test snapshot empty`() {
        val db = emptyDB()
        db.revertSnapshot(db.snapshot())
    }

    @Test
    fun `test snapshot1`() = runBlocking<Unit> {
        val db = emptyDB()
        val address = Address.fromString("aa")

        val genesis = db.snapshot()

        db.withAccountOrCreate(address) { it.storage.set(byteArrayOf(1), byteArrayOf(31, 42)) }
        val snapshot = db.snapshot()

        db.withAccountOrCreate(address) { it.storage.set(byteArrayOf(1), byteArrayOf(31, 43)) }
        db.revertSnapshot(snapshot)

        db.withAccountOrNull(address) {
            requireNotNull(it)
            it.storage.get(byteArrayOf(1)) shouldBe byteArrayOf(31, 42)
            it.storage.getCommittedState(byteArrayOf(1)) shouldBe null
        }

        db.revertSnapshot(genesis)
        db.withAccountOrNull(address) { it shouldBe null }
    }

    @Test
    fun `test snapshot`() = runBlocking<Unit> {
        var db = emptyDB()

        val addr0 = Address.fromString("so0")
        val addr1 = Address.fromString("so1")

        db.withAccountOrCreate(addr0) { it.storage.set(byteArrayOf(1), byteArrayOf(31, 17)) }
        db.withAccountOrCreate(addr1) { it.storage.set(byteArrayOf(1), byteArrayOf(31, 18)) }
        val so0 = db.applyAccountOrThrow(addr0) {
            it as OnchainManagedStateAccount
            it.balance = 42.toBigInteger()
            it.nonce = 43u
            it.suicided = false
            it.deleted = false
            it.setCode(byteArrayOf('c'.code.toByte(), 'a'.code.toByte(), 'f'.code.toByte(), 'e'.code.toByte()))
        }

        db.accountTree.commit(false)
        db = OnchainStateDatabase.from(db)
        val so1 = db.applyAccountOrThrow(addr1) {
            it as OnchainManagedStateAccount
            it.balance = 52.toBigInteger()
            it.nonce = 53u
            it.suicided = true
            it.deleted = true
            it.setCode(
                byteArrayOf(
                    'c'.code.toByte(),
                    'a'.code.toByte(),
                    'f'.code.toByte(),
                    'e'.code.toByte(),
                    '2'.code.toByte()
                )
            )
        }

        db.accountTree.accountByAddress[addr1] = so1
        db.withAccountOrNull(addr1) { it shouldBe null }

        val id = db.snapshot()
        db.revertSnapshot(id)
        db.withAccountOrThrow(addr0) {
            so0.address shouldBe it.address
            so0.balance shouldBe it.balance
            so0.nonce shouldBe it.nonce
            so0.root?.bytes shouldBe it.root?.bytes
            so0.codeHash?.bytes.contentToString() shouldBe it.codeHash?.bytes.contentToString()
//        so0.account.code shouldBe so0Restored.account.code
        }
        db.withAccountOrNull(addr1) { it shouldBe null }
    }

    @Test
    fun `dump`() = runBlocking {
        val db = emptyDB()
        val acc1 = db.applyAccountOrCreate(Address.fromBytes(1)) {
            it.balance += 22.toBigInteger()
        }
        val acc2 = db.applyAccountOrCreate(Address.fromBytes(1, 2)) {
            it as OnchainManagedStateAccount
            it.balance = 22.toBigInteger()
            it.setCode(byteArrayOf(3, 3, 3, 3, 3, 3, 3))
        }
        db.withAccountOrCreate(Address.fromBytes(2)) {
            it.balance = 44.toBigInteger()
        }
    }
}
