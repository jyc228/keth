package ethereum.type

import ethereum.collections.Hash
import ethereum.evm.Address
import ethereum.rlp.toRlp
import java.math.BigInteger

class BlockHeaders(list: List<BlockHeader>) : ArrayList<BlockHeader>(list)

data class BlockHeader(
    val parentHash: Hash,
    val uncleHash: Hash,
    val coinbase: Address,
    val root: Hash,
    val txHash: Hash,
    val receiptHash: Hash,
    // BloomByteLength : 256
    val bloom: ByteArray,
    val difficulty: BigInteger?,
    val number: ULong,
    val gasLimit: BigInteger,
    val gasUsed: BigInteger,
    val time: ULong,
    val extra: ByteArray?,
    val mixDigest: Hash,
    // size : 8
    val nonce: ByteArray,

    /** [baseFee] was added by EIP-1559 and is ignored in legacy headers. */
    val baseFee: BigInteger? = null,

    /** [withdrawalsHash] was added by EIP-4895 and is ignored in legacy headers. */
    val withdrawalsHash: Hash? = null,

    /** [excessDataGas] was added by EIP-4844 and is ignored in legacy headers. */
    val excessDataGas: BigInteger? = null,

    /** [dataGasUsed] was added by EIP-4844 and is ignored in legacy headers. */
    val dataGasUsed: BigInteger? = null,
) {
    val hash by lazy(LazyThreadSafetyMode.NONE) { Hash.keccak256FromBytes(toRlp()) }

    override fun toString(): String {
        return "$number : ${hash.toHexString()} : ${parentHash.toHexString()}"
    }

    companion object
}