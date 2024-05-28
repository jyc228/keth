package io.github.jyc228.keth.fork

class HardForkManager(private val list: List<HardFork>) {

    fun findFork(blockNumber: ULong): HardFork {
        val activated = list.filter { it.activate.blockNumber <= blockNumber }
        val last = activated.last()
        return HardFork(
            name = last.name,
            activate = last.activate,
            eips = activated.flatMap { it.eips }.toSet()
        )
    }

    companion object {
        fun fromNetworkName(networkName: String): HardForkManager {
            val stream = requireNotNull(HardForkManager::class.java.getResourceAsStream("/${networkName}.json")) {
                "$networkName.json not found"
            }
            return HardForkManager(HardFork.listOfFromJson(stream.bufferedReader().readText()))
        }
    }
}
