package com.github.jyc228.keth.vm

import com.github.jyc228.keth.fork.HardFork
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class EVMConfigTest : DescribeSpec({
    it("fromHardFork") {
        val fork = HardFork("test", HardFork.Activate(0u), setOf("eip2", "eip3", "eip145"))
        val config = EVMConfig.fromHardFork(fork)

        config.eip2 shouldBe true
        config.eip145 shouldBe true
        config.eip1014 shouldBe false
    }
})