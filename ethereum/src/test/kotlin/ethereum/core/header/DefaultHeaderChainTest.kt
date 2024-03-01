package ethereum.core.header

import ethereum.config.ChainConfig
import ethereum.config.ForkConfig
import ethereum.consensus.EthashEngin
import ethereum.core.BlockChainBuilder
import ethereum.core.BlockGenerator
import ethereum.core.Genesis
import ethereum.core.repository.ChainRepository
import ethereum.db.InMemoryKeyValueDatabase
import ethereum.history.EIP1559
import io.kotest.core.spec.style.StringSpec

class DefaultHeaderChainTest : StringSpec({
    "test insert" {
        val db = InMemoryKeyValueDatabase()
        val config = ChainConfig.allEthashProtocolChanges()
        val genesis = Genesis(config = config, baseFee = EIP1559.initialBaseFee)
        BlockChainBuilder(db, genesis).commitGenesis()
        val blocks = BlockGenerator.fromGenesis(ForkConfig.from(config), genesis, EthashEngin(), db).generate(128)

        val hc = DefaultHeaderChain(ChainRepository(db))
        hc.writeHeadersAndSetHead(blocks.map { it.header })
    }
})
