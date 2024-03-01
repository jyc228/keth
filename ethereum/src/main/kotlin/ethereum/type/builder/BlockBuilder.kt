package ethereum.type.builder

import ethereum.evm.Address
import ethereum.type.Block
import ethereum.type.BlockHeader
import ethereum.type.Transaction
import java.math.BigInteger

class BlockBuilder(val parent: Block) {
    val header = BlockHeaderBuilder(parent.header)
    var coinbase: Address = Address.EMPTY
    var gasPool: ULong = 0u
    var extra: ByteArray = byteArrayOf()
    var nonce: ByteArray = byteArrayOf()
    var difficulty: BigInteger = BigInteger.ZERO
    var pos: Boolean = false
    private val transactions: MutableList<Transaction> = mutableListOf()

    fun addTransaction(tx: Transaction) {

    }

    fun addUncle(header: BlockHeader) {

    }
}