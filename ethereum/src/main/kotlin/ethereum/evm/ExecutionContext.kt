package ethereum.evm

import ethereum.collections.Hash

class ExecutionContext(
    private var gasPool: ULong,
    private var txHash: Hash,
    private var txIndex: Int
) {
}