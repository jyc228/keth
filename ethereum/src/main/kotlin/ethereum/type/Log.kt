package ethereum.type

import ethereum.collections.Hash
import ethereum.evm.Address

class Log(
    // Consensus fields:
    // address of the contract that generated the event
    val address: Address,
// list of topics provided by the contract.
    val topics: List<Hash>,
// supplied by the contract, usually ABI-encoded
    val data: ByteArray,

// Derived fields. These fields are filled in by the node
// but not secured by consensus.
// block in which the transaction was included
    val blockNumber: ULong,
// hash of the transaction
    val txHash: Hash,
// index of the transaction in the block
    val txIndex: UInt,
// hash of the block in which the transaction was included
    val blockHash: Hash,
// index of the log in the block
    val index: UInt,

// The Removed field is true if this log was reverted due to a chain reorganisation.
// You must pay attention to this field if you receive logs through a filter query.
    val removed: Boolean
)
