package ethereum.config

data class ForkConfig(
    val homestead: BlockNumber,
    val dao: BlockNumber,
    val eip150: BlockNumber,
    val eip155: BlockNumber,
    val eip158: BlockNumber,
    val byzantium: BlockNumber,
    val constantinople: BlockNumber,
    val muirGlacier: BlockNumber,
    val petersburg: BlockNumber,
    val istanbul: BlockNumber,
    val berlin: BlockNumber,
    val london: BlockNumber,
    val arrowGlacier: BlockNumber,
    val grayGlacier: BlockNumber,
    val shanghai: Timestamp,
    val cancun: Timestamp,
    val prague: Timestamp,
) {

    sealed class Comparable<T : Any>(val value: kotlin.Comparable<T>?) {
        fun forked(target: T): Boolean {
            if (value == null) return false
            return value <= target
        }
    }

    class BlockNumber(value: ULong?) : Comparable<ULong>(value)
    class Timestamp(value: ULong?) : Comparable<ULong>(value)

    companion object {
        fun from(chainConfig: ChainConfig) = ForkConfig(
            homestead = BlockNumber(chainConfig.homesteadBlock),
            dao = BlockNumber(chainConfig.daoForkBlock),
            eip150 = BlockNumber(chainConfig.eip150Block),
            eip155 = BlockNumber(chainConfig.eip155Block),
            eip158 = BlockNumber(chainConfig.eip158Block),
            byzantium = BlockNumber(chainConfig.byzantiumBlock),
            constantinople = BlockNumber(chainConfig.constantinopleBlock),
            muirGlacier = BlockNumber(chainConfig.muirGlacierBlock),
            petersburg = BlockNumber(chainConfig.petersburgBlock),
            istanbul = BlockNumber(chainConfig.istanbulBlock),
            berlin = BlockNumber(chainConfig.berlinBlock),
            london = BlockNumber(chainConfig.londonBlock),
            arrowGlacier = BlockNumber(chainConfig.arrowGlacierBlock),
            grayGlacier = BlockNumber(chainConfig.grayGlacierBlock),
            shanghai = Timestamp(chainConfig.shanghaiTime),
            cancun = Timestamp(chainConfig.cancunTime),
            prague = Timestamp(chainConfig.pragueTime)

        )

        fun allForked() = ForkConfig(
            homestead = BlockNumber(value = 0u),
            dao = BlockNumber(value = 0u),
            eip150 = BlockNumber(value = 0u),
            eip155 = BlockNumber(value = 0u),
            eip158 = BlockNumber(value = 0u),
            byzantium = BlockNumber(value = 0u),
            constantinople = BlockNumber(value = 0u),
            muirGlacier = BlockNumber(value = 0u),
            petersburg = BlockNumber(value = 0u),
            istanbul = BlockNumber(value = 0u),
            berlin = BlockNumber(value = 0u),
            london = BlockNumber(value = 0u),
            arrowGlacier = BlockNumber(value = 0u),
            grayGlacier = BlockNumber(value = 0u),
            shanghai = Timestamp(value = 0u),
            cancun = Timestamp(value = 0u),
            prague = Timestamp(value = 0u)
        )

        fun allNonForked() = with(BlockNumber(null) to Timestamp(null)) {
            ForkConfig(
                homestead = first,
                dao = first,
                eip150 = first,
                eip155 = first,
                eip158 = first,
                byzantium = first,
                constantinople = first,
                muirGlacier = first,
                petersburg = first,
                istanbul = first,
                berlin = first,
                london = first,
                arrowGlacier = first,
                grayGlacier = first,
                shanghai = second,
                cancun = second,
                prague = second
            )
        }
    }
}
