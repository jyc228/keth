package ethereum.core.header

import ethereum.type.BlockHeader
import io.github.jyc228.ethereum.Hash
import java.math.BigInteger

interface HeaderChain {
    fun getBlockNumber(hash: Hash): ULong?
    fun getTotalDifficulty(hash: Hash, number: ULong): BigInteger?
    fun getHeaderByHash(hash: Hash): BlockHeader?
    fun getHeaderByNumber(number: ULong): BlockHeader?
    fun getHeader(hash: Hash, number: ULong): BlockHeader?
}