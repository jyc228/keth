package ethereum.consensus

import ethereum.config.ForkConfig

interface ChainHeaderReader {
    val config: ForkConfig
}