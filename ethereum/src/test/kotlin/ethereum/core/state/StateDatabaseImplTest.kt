package ethereum.core.state

import ethereum.collections.Hash
import ethereum.core.database.TreeDatabase
import ethereum.core.state.account.ManagedStateAccount
import ethereum.evm.Address
import org.junit.jupiter.api.Test

class StateDatabaseImplTest {

    // Tests that no intermediate state of an object is stored into the database,
// only the one right before the commit.
    @Test
    fun TestIntermediateLeaks() {
        val addresses = (0..<255).map { Address.fromBytes(it.toByte()) }

        val prevDb = TreeDatabase.memory()
        val prevState = StateDatabaseImpl.empty(prevDb)

        addresses.forEach { addr -> prevState.withAccountOrCreate(addr) { it.withTestData(0) } }

        prevState.intermediateRoot(false)

        val nextDb = TreeDatabase.memory()
        val nextState = StateDatabaseImpl.empty(nextDb)
        addresses.forEach { addr ->
            prevState.withAccountOrCreate(addr) { it.withTestData(99) }
            nextState.withAccountOrCreate(addr) { it.withTestData(99) }
        }
        val prevRoot = prevState.commit(false)
        prevDb.commit(prevRoot)

        val nextRoot = nextState.commit(false)
        nextDb.commit(nextRoot)
        val r = nextDb.db.iterator().asSequence().toList()
        r.forEach { (k, v) ->
            println(k)
            println(v)
        }
    }

    private fun ManagedStateAccount.withTestData(tweak: Byte) {
        val i = address.bytes[0]
        this.balance = (i.toUByte().toInt() * 11 + tweak).toBigInteger()
        this.nonce = (i.toUByte().toInt() * 42 + tweak).toULong()
        if (i % 2 == 0) {
            this.storage[Hash.fromBytes(i, i, i, tweak)] = Hash.EMPTY
            this.storage[Hash.fromBytes(i, i, i, tweak)] = Hash.fromBytes(i, i, i, i, tweak)
        }
        if (i % 3 == 0) {
            this.code = byteArrayOf(i, i, i, i, i, tweak)
        }
    }
}
