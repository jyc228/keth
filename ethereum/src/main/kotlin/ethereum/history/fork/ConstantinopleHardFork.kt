package ethereum.history.fork

import ethereum.type.BlockHeader
import java.math.BigInteger

object ConstantinopleHardFork {
    /** Block reward in wei for successfully mining a block upward from Constantinople */
    val blockReward: BigInteger = 2e+18.toBigDecimal().toBigIntegerExact()

    fun calcDifficulty(time: ULong, parent: BlockHeader): BigInteger {
        TODO("Not yet implemented")
    }
}