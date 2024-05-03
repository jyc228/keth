package ethereum.core

import ethereum.collections.Hash
import ethereum.config.ForkConfig
import ethereum.consensus.ChainHeaderReader
import ethereum.consensus.ConsensusEngin
import ethereum.core.database.TreeDatabase
import ethereum.core.repository.ContractCodeRepository
import ethereum.db.KeyValueDatabase
import ethereum.history.EIP1559
import ethereum.type.Block
import ethereum.type.BlockBody
import ethereum.type.builder.BlockBuilder
import ethereum.type.builder.BlockHeaderBuilder
import io.github.jyc228.ethereum.state.OnchainStateDatabase
import io.github.jyc228.ethereum.state.StateDatabase
import io.github.jyc228.ethereum.state.account.StateRoot

class BlockGenerator(
    override val config: ForkConfig,
    var parent: Block,
    val engin: ConsensusEngin,
    val db: KeyValueDatabase
) : ChainHeaderReader {
    suspend fun generate(count: Int, mutateBlock: ((BlockBuilder, StateDatabase) -> Unit)? = null): List<Block> {
        return (0 until count).map {
            val trieDatabase = TreeDatabase(db)
            val codeDatabase = ContractCodeRepository(db)
            val db =
                OnchainStateDatabase.of(StateRoot.fromByteArray(parent.header.root.bytes), trieDatabase, codeDatabase)
            val header = makeHeaderBuilder(parent, db)
            val body = BlockBody(emptyList(), emptyList(), emptyList())
            val block = engin.finalizeAndAssemble(this, header, db, body, listOf())
            val root = db.commit(false)
            if (root != null) trieDatabase.commit(root)
            parent = block
            block
        }
    }

    fun makeBlock(index: Int) {

    }

    private suspend fun makeHeaderBuilder(parent: Block, db: StateDatabase): BlockHeaderBuilder {
        return BlockHeaderBuilder(parent.header).mutate {
            root = Hash.fromStateRoot(db.intermediateRoot(config.eip158.forked(parent.number)))
            difficulty = engin.calcDifficulty(this@BlockGenerator, parent.header.time + 10u, parent.header)
            if (config.london.forked(number)) {
                baseFee = EIP1559.computeBaseFee(config, parent.header)
                if (!config.london.forked(parent.number)) {
                    val parentGasLimit = parent.header.gasLimit * EIP1559.elasticityMultiplier
                    gasLimit = EIP1559.computeGasLimit(parentGasLimit, parentGasLimit)
                }
            }
        }
    }

    companion object {
        suspend fun fromGenesis(
            config: ForkConfig,
            genesis: Genesis,
            engin: ConsensusEngin,
            db: KeyValueDatabase
        ) = BlockChainBuilder(db, genesis).run {
            commitGenesis()
            BlockGenerator(config, genesisBlock, engin, db)
        }
    }
}

