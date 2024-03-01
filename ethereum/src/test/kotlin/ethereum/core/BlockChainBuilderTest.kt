package ethereum.core

import ethereum.db.InMemoryKeyValueDatabase
import org.junit.jupiter.api.Test

class BlockChainBuilderTest {
    @Test
    fun test() {
        BlockChainBuilder(InMemoryKeyValueDatabase(), Genesis.dev(100.toBigInteger())).build()
    }
}