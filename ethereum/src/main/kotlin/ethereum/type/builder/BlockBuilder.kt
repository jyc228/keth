package ethereum.type.builder

import com.github.jyc228.keth.type.Address
import com.github.jyc228.keth.type.Transaction
import ethereum.type.Block
import ethereum.type.BlockHeader
import java.math.BigInteger

class BlockBuilder(val parent: Block) {
    val header = BlockHeaderBuilder(parent.header)
    var coinbase: Address? = null
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