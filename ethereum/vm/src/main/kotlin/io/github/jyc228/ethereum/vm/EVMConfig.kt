package io.github.jyc228.ethereum.vm

import io.github.jyc228.keth.fork.HardFork
import kotlin.reflect.full.primaryConstructor

class EVMConfig(
    val eip2: Boolean = true,
    val eip7: Boolean = true,
    val eip150: Boolean = true,
    val eip158: Boolean = true,
    val eip160: Boolean = true,
    val eip214: Boolean = true,
    val eip140: Boolean = true,
    val eip211: Boolean = true,
    val eip145: Boolean = true,
    val eip1014: Boolean = true,
    val eip1052: Boolean = true,
    val eip1283: Boolean = true,
    val eip1344: Boolean = true,
    val eip1716: Boolean = true,
    val eip1884: Boolean = true,
    val eip2028: Boolean = true,
    val eip2200: Boolean = true,
    val eip2929: Boolean = true,
    val eip3198: Boolean = true,
    val eip3529: Boolean = true,
    val eip3541: Boolean = true,
    val eip3855: Boolean = true,
    val eip3860: Boolean = true,
    val eip4399: Boolean = true,
) {
    companion object {
        fun fromHardFork(fork: HardFork): EVMConfig {
            val const = requireNotNull(EVMConfig::class.primaryConstructor)
            return const.callBy(const.parameters.associateWith { it.name in fork.eips })
        }
    }
}