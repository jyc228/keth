package ethereum.core.header

import ethereum.collections.Hash
import ethereum.type.BlockHeader
import java.math.BigInteger

interface HeaderChain {
    fun getBlockNumber(hash: Hash): ULong?
    fun getTotalDifficulty(hash: Hash, number: ULong): BigInteger?
    fun getHeaderByHash(hash: Hash): BlockHeader?
    fun getHeaderByNumber(number: ULong): BlockHeader?
    fun getHeader(hash: Hash, number: ULong): BlockHeader?
}