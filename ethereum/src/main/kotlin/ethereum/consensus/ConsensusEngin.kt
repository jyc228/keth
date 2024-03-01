package ethereum.consensus

import ethereum.collections.Hash
import ethereum.core.state.StateDatabase
import ethereum.evm.Address
import ethereum.type.Block
import ethereum.type.BlockBody
import ethereum.type.BlockHeader
import ethereum.type.Receipt
import ethereum.type.builder.BlockHeaderBuilder
import java.math.BigInteger

interface ConsensusEngin {

    fun author(header: BlockHeader): Address

    // VerifyHeader checks whether a header conforms to the consensus rules of a given engine.
    fun verifyHeader(chain: ChainHeaderReader, header: BlockHeader)

    // VerifyHeaders is similar to VerifyHeader, but verifies a batch of headers
    // concurrently. The method returns a quit channel to abort the operations and
    // a results channel to retrieve the async verifications (the order is that of
    // the input slice).
    fun verifyHeaders(chain: ChainHeaderReader, headers: List<BlockHeader>)

    // VerifyUncles verifies that the given block's uncles conform to the consensus
    // rules of a given engine.
    fun verifyUncles(chain: ChainReader, block: Block)

    // Prepare initializes the consensus fields of a block header according to the
    // rules of a particular engine. The changes are executed inline.
    fun prepare(chain: ChainHeaderReader, header: BlockHeader)

    /**
     * runs any post-transaction state modifications (e.g. block rewards or process withdrawals) but does not assemble the block.
     *
     * Note: The [StateDatabase] might be updated to reflect any consensus rules that happen at finalization (e.g. block rewards).
     */
    fun finalize(
        chain: ChainHeaderReader,
        header: BlockHeaderBuilder,
        state: StateDatabase,
        body: BlockBody
    )

    /**
     * runs any post-transaction state modifications (e.g. block rewards or process withdrawals) and assembles the final block.
     *
     * Note: The [BlockHeader] and [StateDatabase] might be updated to reflect any consensus rules that happen at finalization (e.g. block rewards).
     */
    fun finalizeAndAssemble(
        chain: ChainHeaderReader,
        header: BlockHeaderBuilder,
        state: StateDatabase,
        body: BlockBody,
        receipts: List<Receipt>
    ): Block

    // Seal generates a new sealing request for the given input block and pushes
    // the result into the given channel.
    //
    // Note, the method returns immediately and will send the result async. More
    // than one result may also be returned depending on the consensus algorithm.
    fun seal(chain: ChainHeaderReader, block: Block)

    // SealHash returns the hash of a block prior to it being sealed.
    fun sealHash(header: BlockHeader): Hash

    // CalcDifficulty is the difficulty adjustment algorithm. It returns the difficulty
    // that a new block should have.
    fun calcDifficulty(chain: ChainHeaderReader, time: ULong, parent: BlockHeader): BigInteger

    // APIs returns the RPC APIs this consensus engine provides.
    fun api(chain: ChainHeaderReader)

    // Close terminates any background threads maintained by the consensus engine.
    fun close()
}
