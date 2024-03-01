package ethereum.history.fork

import ethereum.type.BlockHeader
import java.math.BigInteger

object ByzantiumHardFork {
    val blockReward: BigInteger = 3e+18.toBigDecimal().toBigIntegerExact()

    fun calcDifficulty(time: ULong, parent: BlockHeader): BigInteger {
        TODO("Not yet implemented")
    }
}