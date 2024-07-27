package ethereum.consensus

import com.github.jyc228.keth.state.StateDatabase
import com.github.jyc228.keth.type.Address
import com.github.jyc228.keth.type.Hash
import com.github.jyc228.keth.type.TransactionReceipt
import ethereum.type.Block
import ethereum.type.BlockBody
import ethereum.type.BlockHeader
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

    override suspend fun finalize(
        chain: ChainHeaderReader,
        header: BlockHeaderBuilder,
        state: StateDatabase,
        body: BlockBody
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun finalizeAndAssemble(
        chain: ChainHeaderReader,
        header: BlockHeaderBuilder,
        state: StateDatabase,
        body: BlockBody,
        receipts: List<TransactionReceipt>
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
