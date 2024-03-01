package ethereum.history.fork

import java.math.BigInteger

object IstanbulHardFork {
    // Per byte of non zero data attached to a transaction after EIP 2028 (part in Istanbul)
    val txDataNonZeroGas: BigInteger = 16.toBigInteger()
}