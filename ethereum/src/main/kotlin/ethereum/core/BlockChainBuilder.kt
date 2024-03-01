package ethereum.core

import ethereum.collections.Hash
import ethereum.config.ChainConfig
import ethereum.consensus.BeaconEngin
import ethereum.consensus.EthashEngin
import ethereum.consensus.FakerEngin
import ethereum.core.BlockFactory.fromGenesis
import ethereum.core.database.TreeDatabase
import ethereum.core.header.DefaultHeaderChain
import ethereum.core.repository.ChainRepository
import ethereum.core.state.StateDatabaseImpl
import ethereum.db.KeyValueDatabase
import ethereum.type.Block

fun newBlockChain(db: KeyValueDatabase, genesis: Genesis, init: BlockChainBuilder.() -> Unit): DefaultBlockChain {
    return BlockChainBuilder(db, genesis).build()
}

class BlockChainBuilder(val db: KeyValueDatabase, val genesis: Genesis) {

    val chainDB = ChainRepository(db)
    val genesisBlock = Block.fromGenesis(genesis)

    fun commitGenesis() {
        val config = genesis.config ?: ChainConfig.allEthashProtocolChanges()
        val root = genesis.commitAlloc(StateDatabaseImpl.empty(TreeDatabase(db)))
        if (root != Hash.EMPTY) {
            TreeDatabase(db).commit(root)
        }
        chainDB.writeTotalDifficulty(genesisBlock.hash, genesisBlock.number, genesisBlock.header.difficulty!!)
        chainDB.writeBlock(genesisBlock)
        chainDB.writeReceipts(genesisBlock.hash, genesisBlock.number, emptyList())
        chainDB.writeCanonicalHash(genesisBlock.hash, genesisBlock.number)
        chainDB.writeHeadBlockHash(genesisBlock.hash)
        chainDB.writeHeadFastBlockHash(genesisBlock.hash)
        chainDB.writeHeadHeaderHash(genesisBlock.hash)
        chainDB.writeChainConfig(genesisBlock.hash, config)
    }

    fun build(): DefaultBlockChain {
        commitGenesis()
        return DefaultBlockChain(
            chainDB,
            FakerEngin(BeaconEngin(EthashEngin())),
            genesisBlock,
            DefaultHeaderChain(chainDB)
        )
    }
}
