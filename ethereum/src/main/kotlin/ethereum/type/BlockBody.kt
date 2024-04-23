package ethereum.type

import io.github.jyc228.ethereum.Transaction
import io.github.jyc228.ethereum.Withdrawal

class BlockBody(
    val uncles: List<BlockHeader>,
    val transactions: List<Transaction>,
    val withdrawals: List<Withdrawal>,
)