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

/**
 * Beacon is a consensus engine that combines the [eth1] consensus and proof-of-stake algorithm.
 * There is a special flag inside to decide whether to use legacy consensus rules or new rules.
 * The transition rule is described in the eth1/2 merge spec.
 * [eip-3675](https://github.com/ethereum/EIPs/blob/master/EIPS/eip-3675.md)
 *
 * The beacon here is a half-functional consensus engine with partial functions which is only used for necessary consensus checks.
 * The legacy consensus engine can be any engine implements the consensus interface (except the beacon itself).
 */
class BeaconEngin(private val eth1: ConsensusEngin) : ConsensusEngin {
    override fun author(header: BlockHeader): Address {
        if (header.pos) return header.coinbase
        return eth1.author(header)
    }

    // VerifyHeader checks whether a header conforms to the consensus rules of the stock Ethereum consensus engine.
    override fun verifyHeader(chain: ChainHeaderReader, header: BlockHeader) {
        TODO("Not yet implemented")
    }

    override fun verifyHeaders(chain: ChainHeaderReader, headers: List<BlockHeader>) {
        chain
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
        return difficulty
    }

    override fun api(chain: ChainHeaderReader) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    private val BlockHeader.pos get() = this.difficulty == difficulty

    companion object {
        private val difficulty = BigInteger.ZERO
    }
}
