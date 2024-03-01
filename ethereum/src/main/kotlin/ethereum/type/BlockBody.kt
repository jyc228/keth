package ethereum.type

class BlockBody(
    val uncles: List<BlockHeader>,
    val transactions: List<Transaction>,
    val withdrawals: List<Withdrawal>,
) {
}