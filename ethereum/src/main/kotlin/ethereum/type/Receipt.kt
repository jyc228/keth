package ethereum.type

import ethereum.collections.Hash
import ethereum.evm.Address
import java.math.BigInteger

class Receipt(
    // Consensus fields: These fields are defined by the Yellow Paper
    val type: Byte,
    val postState: ByteArray,
    val status: ULong,
    val cumulativeGasUsed: ULong,
    val bloom: ByteArray,
    val logs: List<Log>,

// Implementation fields: These fields are added by geth when processing a transaction.
    val txHash: Hash,
    val contractAddress: Address,
    val gasUsed: ULong,
    val effectiveGasPrice: BigInteger,

// Inclusion information: These fields provide information about the inclusion of the
// transaction corresponding to this receipt.
    val blockHash: Hash,
    val blockNumber: BigInteger,
    val transactionIndex: UInt
)
