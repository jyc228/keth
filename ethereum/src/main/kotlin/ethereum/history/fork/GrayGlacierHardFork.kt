package ethereum.history.fork

import ethereum.type.BlockHeader
import java.math.BigInteger

object GrayGlacierHardFork {
    fun calcDifficulty(time: ULong, parent: BlockHeader): BigInteger {
        return BigInteger.TEN
    }
}