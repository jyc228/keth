package ethereum.core

import ethereum.collections.Hash
import ethereum.core.header.HeaderChain
import ethereum.type.Block
import ethereum.type.BlockBody
import ethereum.type.BlockHeader
import ethereum.type.Receipt

interface BlockChain : HeaderChain {
    val state: State

    // GetBlockByHash retrieves a block from the local chain.
    fun getBlockByHash(hash: Hash): Block?

    // GetBlockByHash retrieves a block from the local chain.
    fun getBlockByNumber(number: ULong): Block?

    fun getBlock(hash: Hash, number: ULong): Block?

    fun getBlockBody(hash: Hash): BlockBody?

    // HasBlock verifies a block's presence in the local chain.
    fun hasBlock(hash: Hash): Boolean

    // HasFastBlock verifies a snap block's presence in the local chain.
    fun hasFastBlock(hash: Hash, blockNumber: ULong): Boolean

    fun hasState(hash: Hash): Boolean

    // CurrentBlock retrieves the head block from the local chain.
    fun currentBlock(): BlockHeader

    // InsertChain inserts a batch of blocks into the local chain.
    fun insertChain(blocks: List<Block>): Int

    // InsertReceiptChain inserts a batch of receipts into the local chain.
    fun insertReceiptChain(blocks: List<Block>, receipts: List<Receipt>, ancientLimit: ULong): Int

    interface State {
        // Current head of the chain
        fun currentBlock(): BlockHeader

        // Current head of snap-sync
        fun currentSnapBlock(): BlockHeader

        // Latest (consensus) finalized block
        fun currentFinalBlock(): BlockHeader

        // Latest (consensus) safe block
        fun currentSafeBlock(): BlockHeader
    }
}

