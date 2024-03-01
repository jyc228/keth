package ethereum.core

import ethereum.collections.Hash
import ethereum.config.ForkConfig
import ethereum.consensus.ChainHeaderReader
import ethereum.consensus.ConsensusEngin
import ethereum.core.header.HeaderChain
import ethereum.core.repository.ChainRepository
import ethereum.core.state.StateDatabase
import ethereum.type.Block
import ethereum.type.BlockBody
import ethereum.type.BlockHeader
import ethereum.type.Receipt
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DefaultBlockChain(
    private val repository: ChainRepository,
    private val engin: ConsensusEngin,
    private val genesisBlock: Block,
    headerChain: HeaderChain
) : BlockChain, ChainHeaderReader, HeaderChain by headerChain {
    val cacheConfig = CacheConfig()
    val futureBlocks = mutableMapOf<Hash, Block>()

    override val config: ForkConfig get() = ForkConfig.allForked() // todo
    override val state: BlockChain.State = State.load(repository, headerChain)

    init {
        // The first thing the node will do is reconstruct the verification data for
        // the head block (ethash cache or clique voting snapshot). Might as well do it in advance.
        engin.verifyHeader(this, state.currentBlock())
        CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(5.seconds)
            }
        }
    }

    override fun getBlockByHash(hash: Hash): Block? {
        val number = repository.readHeaderNumber(hash) ?: return null
        return getBlock(hash, number)
    }

    override fun getBlockByNumber(number: ULong): Block? {
        repository.readCanonicalHash(number)
        return null
    }

    override fun getBlock(hash: Hash, number: ULong): Block? {
        return repository.readBlock(hash, number)
    }

    override fun getBlockBody(hash: Hash): BlockBody? {
        TODO("Not yet implemented")
    }

    override fun hasBlock(hash: Hash): Boolean {
        TODO("Not yet implemented")
    }

    override fun hasFastBlock(hash: Hash, blockNumber: ULong): Boolean {
        TODO("Not yet implemented")
    }

    override fun hasState(hash: Hash): Boolean {
        TODO("Not yet implemented")
    }

    override fun currentBlock(): BlockHeader {
        TODO("Not yet implemented")
    }

    override fun insertChain(blocks: List<Block>): Int {
        for (i in 1..blocks.indices.last) {
            val prev = blocks[i - 1]
            val next = blocks[i]
            if (prev.number + 1u == next.number && prev.hash == next.header.parentHash) continue
            error("...")
        }
        engin.verifyHeaders(this, blocks.map { it.header })
        for (i in 1..blocks.indices.last) {
            val prev = blocks[i - 1]
            val next = blocks[i]
            if (prev.number + 1u == next.number && prev.hash == next.header.parentHash) continue
            error("...")
        }
        return 0
    }

    fun validateBody(block: Block) {
    }

    fun skipBlock(parent: Block?, block: Block): Boolean {
//        val parentRoot = parent?.header?.root ?: headerChain.getHeaderByHash(block.header.parentHash)?.root
        return false
    }

    override fun insertReceiptChain(blocks: List<Block>, receipts: List<Receipt>, ancientLimit: ULong): Int {
        TODO("Not yet implemented")
    }


    fun writeBlockWithState(block: Block, receipts: List<Receipt>, state: StateDatabase) {
        val ptd = getTotalDifficulty(block.header.parentHash, block.number - 1u) ?: error("ErrUnknownAncestor")
        val externTd = ptd + (block.header.difficulty ?: BigInteger.ZERO)

        // Irrelevant of the canonical status, write the block itself to the database.
        // Note all the components of block(td, hash->number map, header, body, receipts)
        // should be written atomically. BlockBatch is used for containing all components.
        repository.writeTotalDifficulty(block.hash, block.number, externTd)
        repository.writeBlock(block)
        repository.writeReceipts(block.hash, block.number, receipts)
//        repository.writePreimage(state.preimage())
        // Commit all cached state changes into underlying memory database.
        val root = state.commit(config.eip158.forked(block.number))
        // If we're running an archive node, always flush
        if (cacheConfig.trieDirtyDisabled) {
//            return bc.triedb.Commit(root, false)
        }
        // Full but not archive node, do proper garbage collection
    }


    fun reset() {

    }

    private data class State(
        var currentBlock: BlockHeader, // Current head of the chain
        var currentSnapBlock: BlockHeader, // Current head of snap-sync
        var currentFinalBlock: BlockHeader, // Latest (consensus) finalized block
        var currentSafeBlock: BlockHeader, // Latest (consensus) safe block
    ) : BlockChain.State {
        override fun currentBlock(): BlockHeader = currentBlock
        override fun currentSnapBlock(): BlockHeader = currentSnapBlock
        override fun currentFinalBlock(): BlockHeader = currentFinalBlock
        override fun currentSafeBlock(): BlockHeader = currentSafeBlock

        companion object {
            fun load(db: ChainRepository, hc: HeaderChain): State {
                val hash = db.readHeadBlockHash() ?: error("Empty database, resetting chain")
                val headBlockHeader = hc.getHeaderByHash(hash) ?: error("Head block missing, resetting chain")
//                val header = db.readHeadHeaderHash()?.let { hc.getHeaderByHash(it) } ?: headBlockHeader
//                val header = db.readHeadFastBlockHash()?.let { hc.getHeaderByHash(it) } ?: headBlockHeader
                val currentFinalBlock = db.readFinalizedBlockHash()?.let(hc::getHeaderByHash) ?: headBlockHeader
                return State(
                    currentBlock = headBlockHeader,
                    currentSnapBlock = db.readHeadFastBlockHash()?.let(hc::getHeaderByHash) ?: headBlockHeader,
                    currentFinalBlock = currentFinalBlock,
                    currentSafeBlock = currentFinalBlock
                )
            }
        }
    }

    data class CacheConfig(
        // Memory allowance (MB) to use for caching trie nodes in memory
        val trieCleanLimit: Int = 256,
        // Disk journal for saving clean cache entries.
        val trieCleanJournal: String = "",
        // Time interval to dump clean cache to disk periodically
        val trieCleanRejournal: Duration = 0.seconds,
        // Whether to disable heuristic state prefetching for followup blocks
        val trieCleanNoPrefetch: Boolean = false,
        // Memory limit (MB) at which to start flushing dirty trie nodes to disk
        val trieDirtyLimit: Int = 256,
        // Whether to disable trie write caching and GC altogether (archive node)
        val trieDirtyDisabled: Boolean = false,
        // Time limit after which to flush the current in-memory trie to disk
        val trieTimeLimit: Duration = 5.minutes,
        // Memory allowance (MB) to use for caching snapshot entries in memory
        val snapshotLimit: Int = 256,
        // Whether to store preimage of trie key to the disk
        val preimages: Boolean = false,
        // Whether the background generation is allowed
        val snapshotNoBuild: Boolean = false,
        // Wait for snapshot construction on startup. TODO(karalabe): This is a dirty hack for testing, nuke it
        val snapshotWait: Boolean = true,
    )
}

