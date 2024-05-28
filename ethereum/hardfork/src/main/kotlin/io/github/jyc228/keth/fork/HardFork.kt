package io.github.jyc228.keth.fork

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class HardFork(
    val name: String,
    val activate: Activate,
    val eips: Set<String>
) {
    @Serializable
    data class Activate(
        val blockNumber: ULong,
        val epochNumber: ULong = 0u,
        val slotNumber: ULong = 0u,
    )

    companion object {
        fun listOfFromJson(json: String): List<HardFork> = Json.decodeFromString<List<HardFork>>(json)
    }
}
