package ethereum.consensus

import ethereum.collections.Hash
import ethereum.core.BlockFactory.new
import ethereum.core.state.StateDatabase
import ethereum.evm.Address
import ethereum.history.fork.ArrowGlacierHardFork
import ethereum.history.fork.ByzantiumHardFork
import ethereum.history.fork.ConstantinopleHardFork
import ethereum.history.fork.FrontierHardFork
import ethereum.history.fork.GrayGlacierHardFork
import ethereum.history.fork.HomesteadHardFork
import ethereum.history.fork.LondonHardFork
import ethereum.history.fork.MuirGlacierHardFork
import ethereum.type.Block
import ethereum.type.BlockBody
import ethereum.type.BlockHeader
import ethereum.type.Receipt
import ethereum.type.builder.BlockHeaderBuilder
import java.math.BigInteger

/**
 * [EthashEngin] is a consensus engine based on proof-of-work implementing the ethash algorithm.
 */
class EthashEngin : ConsensusEngin {
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
        // Select the correct block reward based on chain progression
        val blockReward = when {
            chain.config.byzantium.forked(header.number) -> ByzantiumHardFork.blockReward
            chain.config.constantinople.forked(header.number) -> ConstantinopleHardFork.blockReward
            else -> FrontierHardFork.blockReward
        }
        val reward = body.uncles.fold(blockReward) { reward, uncle ->
            val r = (uncle.number + 8u - header.number).toString().toBigInteger() * blockReward / 8.toBigInteger()
            state.withAccountOrCreate(uncle.coinbase) { it.balance += r }
            reward + (r / 32.toBigInteger())
        }
        state.withAccountOrCreate(header.coinbase) { it.balance += reward }
    }

    override fun finalizeAndAssemble(
        chain: ChainHeaderReader,
        header: BlockHeaderBuilder,
        state: StateDatabase,
        body: BlockBody,
        receipts: List<Receipt>
    ): Block {
        require(body.withdrawals.isEmpty()) { "ethash does not support withdrawals" }
        finalize(chain, header, state, body)
        return Block.new(
            header = header.mutate { root = state.intermediateRoot(chain.config.eip158.forked(header.number)) },
            transactions = body.transactions,
            uncles = body.uncles,
            receipts = receipts
        )
    }

    override fun seal(chain: ChainHeaderReader, block: Block) {
        TODO("Not yet implemented")
    }

    override fun sealHash(header: BlockHeader): Hash {
        TODO("Not yet implemented")
    }

    override fun calcDifficulty(chain: ChainHeaderReader, time: ULong, parent: BlockHeader): BigInteger {
        val next = parent.number + 1u
        return when {
            chain.config.grayGlacier.forked(next) -> GrayGlacierHardFork.calcDifficulty(time, parent)
            chain.config.arrowGlacier.forked(next) -> ArrowGlacierHardFork.calcDifficulty(time, parent)
            chain.config.london.forked(next) -> LondonHardFork.calcDifficulty(time, parent)
            chain.config.muirGlacier.forked(next) -> MuirGlacierHardFork.calcDifficulty(time, parent)
            chain.config.constantinople.forked(next) -> ConstantinopleHardFork.calcDifficulty(time, parent)
            chain.config.byzantium.forked(next) -> ByzantiumHardFork.calcDifficulty(time, parent)
            chain.config.homestead.forked(next) -> HomesteadHardFork.calcDifficulty(time, parent)
            else -> FrontierHardFork.calcDifficulty(time, parent)
        }
    }

    override fun api(chain: ChainHeaderReader) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    companion object {
        /** Maximum number of uncles allowed in a single block */
        val maxUncles = 2

        /** Max seconds from current time allowed for blocks, before they're considered future blocks */
        val allowedFutureBlockTimeSeconds = 15

        /** The bound divisor of the difficulty, used in the update calculations. */
        val difficultyBoundDivisor = 2048.toBigInteger()

        /** The minimum that the difficulty may ever be. */
        val minimumDifficulty = 131072.toBigInteger()

        val expDiffPeriod = 100000u
    }
}
