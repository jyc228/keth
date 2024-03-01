package ethereum.core.header

import ethereum.collections.Hash
import ethereum.core.repository.ChainRepository
import ethereum.type.BlockHeader
import java.math.BigInteger
import kotlin.random.Random

/**
 * The basic block header chain logic that is shared by core.BlockChain and light.LightChain.
 * It is not usable in itself, only as a part of either structure.
 *
 * [HeaderChain] is responsible for maintaining the header chain including the header query and updating.
 * The components maintained by headerchain includes:
 * 1. total difficulty
 * 2. header
 * 3. block hash -> number mapping
 * 4. canonical number -> hash mapping
 * 5. head header flag.
 *
 * It is not thread safe either, the encapsulating chain structures should do the necessary mutex locking/unlocking.
 */
class DefaultHeaderChain(
    private val repository: ChainRepository,

    private val terminalTotalDifficulty: BigInteger? = null,
    /**
     * helper function used in td fork choice.
     * Miners will prefer to choose the local mined block if the local td is equal to the extern one.
     * It can be nil for light client
     */
    private val preserve: ((BlockHeader) -> Boolean)? = null
) : HeaderChain {
    private var currentHeader: BlockHeader =
        repository.readHeadBlockHash()?.let { getHeaderByHash(it) } ?: getHeaderByNumber(0u) ?: error("...")

    override fun getBlockNumber(hash: Hash) = repository.readHeaderNumber(hash)
    override fun getTotalDifficulty(hash: Hash, number: ULong) = repository.readTotalDifficulty(hash, number)
    override fun getHeaderByHash(hash: Hash) = getBlockNumber(hash)?.let { getHeader(hash, it) }
    override fun getHeaderByNumber(number: ULong) = repository.readCanonicalHash(number)?.let { getHeader(it, number) }
    override fun getHeader(hash: Hash, number: ULong) = repository.readBlockHeader(hash, number)

    /**
     * Writes a batch of block [headers] and applies the last header as the chain head if the fork choicer says it's ok to update the chain.
     *
     * Note: This method is not concurrent-safe with inserting blocks simultaneously into the chain,
     * as side effects caused by reorganisations cannot be emulated without the real blocks.
     *
     * Hence, writing headers directly should only be done in two scenarios:
     * - pure-header mode of operation (light clients)
     * - properly separated header/block phases (non-archive clients).
     */
    fun writeHeadersAndSetHead(headers: List<BlockHeader>): WriteResult {
        val writeCount = writeHeaders(headers)
        val lastHeader = headers.last()
        var status = when (writeCount == 0) {
            true -> WriteStatus.NONE
            false -> WriteStatus.SIDE
        }
        if (reorgNeeded(currentHeader, lastHeader)) {
            if (lastHeader.hash == repository.readCanonicalHash(lastHeader.number) && lastHeader.number <= currentHeader.number) {
                // Special case, all the inserted headers are already on the canonical header chain, skip the reorg operation.
            } else {
                reorg(headers)
                status = WriteStatus.CANONICAL
            }
        }
        return WriteResult(
            status = status,
            ignored = headers.size - writeCount,
            imported = writeCount,
            lastHeader = headers.last()
        )
    }

    /**
     * Writes a chain of [headers] into the local chain, given that the parents are already known.
     *
     * The chain head header won't be updated in this function, the additional SetCanonical is expected in order to finish the entire procedure.
     */
    private fun writeHeaders(headers: List<BlockHeader>): Int {
        var totalDifficulty =
            getTotalDifficulty(headers[0].parentHash, headers[0].number - 1u) ?: error("ErrUnknownAncestor")
        var parentKnown = true
        var writeCount = 0
        for ((index, header) in headers.withIndex()) {
            // The headers have already been validated at this point, so we already know that it's a contiguous chain
            // headers[i].Hash() == headers[i+1].ParentHash
            val hash = when (index == headers.lastIndex) {
                true -> header.hash
                false -> headers[index + 1].parentHash
            }
            totalDifficulty += header.difficulty ?: BigInteger.ZERO
            val alreadyKnown = parentKnown && getHeader(hash, header.number) != null
            if (!alreadyKnown) {
                writeCount++
                repository.writeTotalDifficulty(hash, header.number, totalDifficulty)
                repository.writeBlockHeader(header)
            }
            parentKnown = alreadyKnown
        }
        return writeCount
    }


    /**
     * - td mode : the new head is chosen if the corresponding total difficulty is higher.
     * - extern mode : the trusted header is always selected as the head.
     * @return The reorg should be applied based on the given external header and local canonical chain
     */
    fun reorgNeeded(local: BlockHeader, external: BlockHeader): Boolean {
        val localTD = getTotalDifficulty(local.hash, local.number) ?: error("missing localTD")
        val externalTD = getTotalDifficulty(external.hash, external.number) ?: error("missing externalTD")

        // Accept the new header as the chain head if the transition is already triggered.
        // We assume all the headers after the transition come from the trusted consensus layer.
        if (terminalTotalDifficulty != null && terminalTotalDifficulty <= externalTD) {
            return true
        }

        // If the total difficulty is higher than our known, add it to the canonical chain
        return when (externalTD.compareTo(localTD)) {
            1 -> true
            -1 -> false
            // Local and external difficulty is identical.
            // Second clause in the if statement reduces the vulnerability to selfish mining.
            // Please refer to http://www.cs.cornell.edu/~ie53/publications/btcProcFC.pdf
            else -> when (external.number.compareTo(local.number)) {
                1 -> false
                -1 -> true
                else -> {
                    val currentPreserve = preserve?.invoke(local) ?: false
                    val externalPreserve = preserve?.invoke(external) ?: false
                    !currentPreserve && (externalPreserve || Random.nextFloat() < 0.5)
                }
            }
        }
    }

    /**
     * reorgs the local canonical chain into the specified chain.
     *
     * The reorg can be classified into two cases
     * - extend the local chain
     * - switch the head to the given header.
     */
    fun reorg(headers: List<BlockHeader>) {
        if (headers.isEmpty()) return
        if (currentHeader.hash != headers.first().parentHash) {
            // Delete any canonical number assignments above the new head
            generateSequence(headers.last().number) { it + 1u }
                .takeWhile { repository.readCanonicalHash(it) != null }
                .forEach { repository.deleteCanonicalHash(it) }

            // Overwrite any stale canonical number assignments,
            // going backwards from the first header in this import until the cross-link between two chains.
            // todo
        }
        for (i in 0 until headers.lastIndex) {
            repository.writeCanonicalHash(headers[i + 1].parentHash, headers[i].number)
            repository.writeHeadHeaderHash(headers[i + 1].parentHash)
        }
        // Write the last header
        repository.writeCanonicalHash(headers.last().parentHash, headers.last().number)
        repository.writeHeadHeaderHash(headers.last().parentHash)

        // Last step update all in-memory head header markers
        currentHeader = headers.last()
    }

    data class WriteResult(
        val status: WriteStatus,
        val ignored: Int,
        val imported: Int,
        val lastHeader: BlockHeader,
    )

    enum class WriteStatus { NONE, CANONICAL, SIDE }
}
