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

class FakerEngin(engin: BeaconEngin) : ConsensusEngin by engin {
    override fun author(header: BlockHeader): Address {
        TODO("Not yet implemented")
    }

    override fun verifyHeader(chain: ChainHeaderReader, header: BlockHeader) {
        TODO("Not yet implemented")
    }

    override fun verifyHeaders(chain: ChainHeaderReader, headers: List<BlockHeader>) {
        TODO("Not yet implemented")
    }

    override fun verifyUncles(chain: ChainReader, block: Block) {
        TODO("Not yet implemented")
    }

    override fun prepare(chain: ChainHeaderReader, header: BlockHeader) {
        TODO("Not yet implemented")
    }

    override fun finalize(chain: ChainHeaderReader, header: BlockHeaderBuilder, state: StateDatabase, body: BlockBody) {
        TODO("Not yet implemented")
    }

    override fun finalizeAndAssemble(
        chain: ChainHeaderReader,
        header: BlockHeaderBuilder,
        state: StateDatabase,
        body: BlockBody,
        receipts: List<Receipt>
    ): Block {
        TODO("Not yet implemented")
    }

    override fun seal(chain: ChainHeaderReader, block: Block) {
        TODO("Not yet implemented")
    }

    override fun sealHash(header: BlockHeader): Hash {
        TODO("Not yet implemented")
    }

    override fun calcDifficulty(chain: ChainHeaderReader, time: ULong, parent: BlockHeader): BigInteger {
        TODO("Not yet implemented")
    }

    override fun api(chain: ChainHeaderReader) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}
