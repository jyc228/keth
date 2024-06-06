package ethereum.core.state

import ethereum.core.database.TreeDatabase
import ethereum.core.repository.ContractCodeRepository
import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.state.OnchainStateDatabase
import io.github.jyc228.ethereum.state.account.ManagedStateAccount
import io.kotest.common.runBlocking
import org.junit.jupiter.api.Test

class OnchainStateDatabaseTest {

    // Tests that no intermediate state of an object is stored into the database,
// only the one right before the commit.
    @Test
    fun TestIntermediateLeaks() = runBlocking {
        val addresses = (0..<255).map { Address.fromByteArray(byteArrayOf(it.toByte())) }

        val prevDb = TreeDatabase.memory()
        val prevState = OnchainStateDatabase.empty(prevDb, ContractCodeRepository(prevDb.db))

        addresses.forEach { addr -> prevState.withAccountOrCreate(addr) { it.withTestData(0) } }

        prevState.intermediateRoot(false)

        val nextDb = TreeDatabase.memory()
        val nextState = OnchainStateDatabase.empty(nextDb, ContractCodeRepository(nextDb.db))
        addresses.forEach { addr ->
            prevState.withAccountOrCreate(addr) { it.withTestData(99) }
            nextState.withAccountOrCreate(addr) { it.withTestData(99) }
        }
        val prevRoot = requireNotNull(prevState.commit(false))
        prevDb.commit(prevRoot)

        val nextRoot = requireNotNull(nextState.commit(false))
        nextDb.commit(nextRoot)
        val r = nextDb.db.iterator().asSequence().toList()
        r.forEach { (k, v) ->
            println(k)
            println(v)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun ManagedStateAccount.withTestData(tweak: Byte) {
        val i = address.hex.hexToByteArray()[0]
        this.balance = (i.toUByte().toInt() * 11 + tweak).toBigInteger()
        this.nonce = (i.toUByte().toInt() * 42 + tweak).toULong()
        if (i % 2 == 0) {
            this.storage.set(byteArrayOf(i, i, i, tweak), null)
            this.storage.set(byteArrayOf(i, i, i, tweak), byteArrayOf(i, i, i, i, tweak))
        }
        if (i % 3 == 0) {
            this.setCode(byteArrayOf(i, i, i, i, i, tweak))
        }
    }
}
