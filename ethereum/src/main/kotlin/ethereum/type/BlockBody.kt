package ethereum.type

import com.github.jyc228.keth.type.Transaction
import com.github.jyc228.keth.type.Withdrawal

class BlockBody(
    val uncles: List<BlockHeader>,
    val transactions: List<Transaction>,
    val withdrawals: List<Withdrawal>,
)