package io.github.jyc228.keth.fork

class HardForkManager(private val list: List<HardFork>) {
    companion object {
        fun fromNetworkName(networkName: String): HardForkManager {
            val stream = requireNotNull(HardForkManager::class.java.getResourceAsStream("/${networkName}.json")) {
                "$networkName.json not found"
            }
            return HardForkManager(HardFork.listOfFromJson(stream.bufferedReader().readText()))
        }
    }
}
