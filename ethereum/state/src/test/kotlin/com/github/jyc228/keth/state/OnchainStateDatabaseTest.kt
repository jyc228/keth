package com.github.jyc228.keth.state

import com.github.jyc228.keth.state.account.Address
import com.github.jyc228.keth.state.account.ManagedStateAccount
import io.kotest.common.runBlocking
import org.junit.jupiter.api.Test

class OnchainStateDatabaseTest {

    // Tests that no intermediate state of an object is stored into the database,
    // only the one right before the commit.
    @Test
    fun TestIntermediateLeaks() = runBlocking {
        val addresses = (0..<255).map { Address(byteArrayOf(it.toByte())) }

        val prevDb = DefaultTreeDatabase.memory()
        val prevState = OnchainStateDatabase.empty(prevDb, ContractCodeRepository(prevDb.db))

        addresses.forEach { addr -> prevState.withAccountOrCreate(addr) { it.withTestData(0) } }

        prevState.intermediateRoot()

        val nextDb = DefaultTreeDatabase.memory()
        val nextState = OnchainStateDatabase.empty(nextDb, ContractCodeRepository(nextDb.db))
        addresses.forEach { addr ->
            prevState.withAccountOrCreate(addr) { it.withTestData(99) }
            nextState.withAccountOrCreate(addr) { it.withTestData(99) }
        }
        val prevRoot = requireNotNull(prevState.commit())
        prevDb.commit(prevRoot)

        val nextRoot = requireNotNull(nextState.commit())
        nextDb.commit(nextRoot)
        val r = nextDb.db.iterator().asSequence().toList()
        r.forEach { (k, v) ->
            println(k)
            println(v)
        }
    }

    private suspend fun ManagedStateAccount.withTestData(tweak: Byte) {
        val i = address.bytes[0]
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
