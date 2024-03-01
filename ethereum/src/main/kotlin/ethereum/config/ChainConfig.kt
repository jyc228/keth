package ethereum.config

import ethereum.collections.Hash
import java.math.BigInteger

data class ChainConfig(
    /** chainId identifies the current chain and is used for replay protection */
    val chainId: BigInteger,
    /** Homestead switch block (nil = no fork, 0 = already homestead) */
    val homesteadBlock: ULong? = null,
    /** TheDAO hard-fork switch block (nil = no fork) */
    val daoForkBlock: ULong? = null,
    /** Whether the nodes supports or opposes the DAO hard-fork */
    val daoForkSupport: Boolean,

    // EIP150 implements the Gas price changes (https://github.com/ethereum/EIPs/issues/150)
    /** EIP150 HF block (nil = no fork) */
    val eip150Block: ULong? = 0u,
    /** EIP150 HF hash (needed for header only clients as only gas pricing changed) */
    val eip150Hash: Hash = Hash.EMPTY,

    /** EIP155 HF block */
    val eip155Block: ULong? = 0u,
    /** EIP158 HF block */
    val eip158Block: ULong? = 0u,

    /** Byzantium switch block (nil = no fork, 0 = already on byzantium) */
    val byzantiumBlock: ULong? = 0u,
    /** Constantinople switch block (nil = no fork, 0 = already activated) */
    val constantinopleBlock: ULong? = 0u,
    /** Petersburg switch block (nil = same as Constantinople) */
    val petersburgBlock: ULong? = 0u,
    /** Istanbul switch block (nil = no fork, 0 = already on istanbul) */
    val istanbulBlock: ULong? = 0u,
    /** Eip-2384 (bomb delay) switch block (nil = no fork, 0 = already activated) */
    val muirGlacierBlock: ULong? = 0u,
    /** Berlin switch block (nil = no fork, 0 = already on berlin) */
    val berlinBlock: ULong? = 0u,
    /** London switch block (nil = no fork, 0 = already on london) */
    val londonBlock: ULong? = 0u,
    /** Eip-4345 (bomb delay) switch block (nil = no fork, 0 = already activated) */
    val arrowGlacierBlock: ULong? = 0u,
    /** Eip-5133 (bomb delay) switch block (nil = no fork, 0 = already activated) */
    val grayGlacierBlock: ULong? = 0u,
    /** Virtual fork after The Merge to use as a network splitter */
    val mergeNetsplitBlock: ULong? = 0u,

    // Fork scheduling was switched from blocks to timestamps here
    /** Shanghai switch time (nil = no fork, 0 = already on shanghai) */
    val shanghaiTime: ULong? = 0u,
    /** Cancun switch time (nil = no fork, 0 = already on cancun) */
    val cancunTime: ULong? = 0u,
    /** Prague switch time (nil = no fork, 0 = already on prague) */
    val pragueTime: ULong? = 0u,

    // TerminalTotalDifficulty is the amount of total difficulty reached by the network that triggers the consensus upgrade.
    val terminalTotalDifficulty: BigInteger? = null,

// TerminalTotalDifficultyPassed is a flag specifying that the network already
// passed the terminal total difficulty. Its purpose is to disable legacy sync
// even without having seen the TTD locally (safer long term).
    val terminalTotalDifficultyPassed: Boolean = true,

// Various consensus engines
    val Ethash: Any? = null,
    val Clique: Any? = null,
) {
    companion object {
        fun allEthashProtocolChanges() = ChainConfig(
            chainId = BigInteger.valueOf(1337),
            daoForkSupport = false,
            mergeNetsplitBlock = null,
            shanghaiTime = null,
            cancunTime = null,
            pragueTime = null,
            terminalTotalDifficulty = null,
            terminalTotalDifficultyPassed = false
        )
    }
}