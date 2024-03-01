package ethereum.core

import ethereum.config.ChainConfig
import ethereum.db.InMemoryKeyValueDatabase
import ethereum.history.EIP1559
import org.junit.jupiter.api.Test

class BlockChainTest {
    @Test
    fun testLastBlock() {
        val builder = BlockChainBuilder(
            InMemoryKeyValueDatabase(),
            Genesis(
                baseFee = EIP1559.initialBaseFee,
                config = ChainConfig.allEthashProtocolChanges()
            )
        )
        val blockChain = builder.build()
    }
}
