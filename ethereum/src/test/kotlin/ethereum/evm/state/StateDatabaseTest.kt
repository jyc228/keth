package ethereum.evm.state

import ethereum.collections.Hash
import ethereum.core.state.StateDatabaseImpl
import ethereum.evm.Address
import io.kotest.matchers.shouldBe
import java.math.BigInteger
import org.junit.jupiter.api.Test

class StateDatabaseTest {

    @Test
    fun `touch delete`() {
        var db = StateDatabaseImpl.empty()
        db.accountTree.create(Address.EMPTY)
        db.commit(false)
        db = StateDatabaseImpl.from(db)
        val snapshot = db.snapshot()
        db.withAccountOrCreate(Address.EMPTY) { it.balance += BigInteger.ZERO }
        db.revertSnapshot(snapshot)
    }

    @Test
    fun `test null`() {
        val db = StateDatabaseImpl.empty()
        val address = Address.fromHexString("0x823140710bf13990e4500136726d8b55")
        db.createAccount(address)
        db.withAccountOrCreate(address) { it.storage[Hash.EMPTY] = Hash.EMPTY }
        db.accountTree.commit(false)

        db.withAccountOrNull(address) {
            requireNotNull(it)
            it.storage[Hash.EMPTY] shouldBe Hash.EMPTY
            it.storage.getCommittedState(Hash.EMPTY) shouldBe Hash.EMPTY
        }

        db.applyAccountOrThrow(address) {
            it.balance += 1200.toBigInteger()
        }
    }

    @Test
    fun `test snapshot empty`() {
        val db = StateDatabaseImpl.empty()
        db.revertSnapshot(db.snapshot())
    }

    @Test
    fun `test snapshot1`() {
        val db = StateDatabaseImpl.empty()
        val address = Address.fromString("aa")

        val genesis = db.snapshot()

        db.withAccountOrCreate(address) { it.storage[Hash.EMPTY] = Hash.new { set(31, 42) } }
        val snapshot = db.snapshot()

        db.withAccountOrCreate(address) { it.storage[Hash.EMPTY] = Hash.new { set(31, 43) } }
        db.revertSnapshot(snapshot)

        db.withAccountOrNull(address) {
            requireNotNull(it)
            it.storage[Hash.EMPTY] shouldBe Hash.new { set(31, 42) }
            it.storage.getCommittedState(Hash.EMPTY) shouldBe Hash.EMPTY
        }

        db.revertSnapshot(genesis)
        db.withAccountOrNull(address) { it shouldBe null }
    }

    @Test
    fun `test snapshot`() {
        var db = StateDatabaseImpl.empty()

        val addr0 = Address.fromString("so0")
        val addr1 = Address.fromString("so1")

        db.withAccountOrCreate(addr0) { it.storage[Hash.EMPTY] = Hash.new { set(31, 17) } }
        db.withAccountOrCreate(addr1) { it.storage[Hash.EMPTY] = Hash.new { set(31, 18) } }
        val so0 = db.applyAccountOrThrow(addr0) {
            it.balance = 42.toBigInteger()
            it.nonce = 43u
            it.suicided = false
            it.deleted = false
            it.code = byteArrayOf('c'.code.toByte(), 'a'.code.toByte(), 'f'.code.toByte(), 'e'.code.toByte())
        }

        db.accountTree.commit(false)
        db = StateDatabaseImpl.from(db)
        val so1 = db.applyAccountOrThrow(addr1) {
            it.balance = 52.toBigInteger()
            it.nonce = 53u
            it.suicided = true
            it.deleted = true
            it.code = byteArrayOf(
                'c'.code.toByte(),
                'a'.code.toByte(),
                'f'.code.toByte(),
                'e'.code.toByte(),
                '2'.code.toByte()
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
            so0.root shouldBe it.root
            so0.codeHash shouldBe it.codeHash
//        so0.account.code shouldBe so0Restored.account.code
        }
        db.withAccountOrNull(addr1) { it shouldBe null }
    }

    @Test
    fun `dump`() {
        val db = StateDatabaseImpl.empty()
        val acc1 = db.applyAccountOrCreate(Address.fromBytes(1)) {
            it.balance += 22.toBigInteger()
        }
        val acc2 = db.applyAccountOrCreate(Address.fromBytes(1, 2)) {
            it.balance = 22.toBigInteger()
            it.code = byteArrayOf(3, 3, 3, 3, 3, 3, 3)
        }
        db.withAccountOrCreate(Address.fromBytes(2)) {
            it.balance = 44.toBigInteger()
        }
    }
}
