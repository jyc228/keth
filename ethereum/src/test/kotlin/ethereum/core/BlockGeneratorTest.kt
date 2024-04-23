package ethereum.core

import ethereum.config.ForkConfig
import ethereum.consensus.EthashEngin
import ethereum.db.InMemoryKeyValueDatabase
import ethereum.evm.Address
import org.junit.jupiter.api.Test

class BlockGeneratorTest {
    @Test
    fun test() {
        val addr1 = Address.fromString("aa")
        val addr2 = Address.fromString("aa")
        val genesis = Genesis(baseFee = null)
        val generator = BlockGenerator.fromGenesis(
            ForkConfig.allNonForked().copy(homestead = ForkConfig.BlockNumber(0u)),
            genesis,
            EthashEngin(),
            InMemoryKeyValueDatabase()
        )
        val blocks = generator.generate(5) { builder, db ->

        }

        blocks.forEach {
            println(it.hash.toHexString())
        }
    }
}