package ethereum.core

import ethereum.config.ChainConfig
import ethereum.consensus.BeaconEngin
import ethereum.consensus.EthashEngin
import ethereum.consensus.FakerEngin
import ethereum.core.BlockFactory.fromGenesis
import ethereum.core.database.TreeDatabase
import ethereum.core.header.DefaultHeaderChain
import ethereum.core.repository.ChainRepository
import ethereum.core.repository.ContractCodeRepository
import ethereum.db.KeyValueDatabase
import ethereum.type.Block
import io.github.jyc228.ethereum.state.OnchainStateDatabase

class BlockChainBuilder(val db: KeyValueDatabase, val genesis: Genesis) {

    val chainDB = ChainRepository(db)
    val genesisBlock = Block.fromGenesis(genesis)

    suspend fun commitGenesis() {
        val config = genesis.config ?: ChainConfig.allEthashProtocolChanges()
        val root = genesis.commitAlloc(OnchainStateDatabase.empty(TreeDatabase(db), ContractCodeRepository(db)))
        if (root != null) {
            TreeDatabase(db).commit(root)
        }
        chainDB.writeTotalDifficulty(genesisBlock.hash, genesisBlock.number, genesisBlock.header.difficulty!!)
        chainDB.writeBlock(genesisBlock) // todo
        chainDB.writeReceipts(genesisBlock.hash, genesisBlock.number, emptyList())
        chainDB.writeCanonicalHash(genesisBlock.hash, genesisBlock.number)
        chainDB.writeHeadBlockHash(genesisBlock.hash)
        chainDB.writeHeadFastBlockHash(genesisBlock.hash)
        chainDB.writeHeadHeaderHash(genesisBlock.hash)
        chainDB.writeChainConfig(genesisBlock.hash, config)
    }

    suspend fun build(): DefaultBlockChain {
        commitGenesis()
        return DefaultBlockChain(
            chainDB,
            FakerEngin(BeaconEngin(EthashEngin())),
            genesisBlock,
            DefaultHeaderChain(chainDB)
        )
    }
}
