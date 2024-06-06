package io.github.jyc228.ethereum.vm

import io.github.jyc228.ethereum.Address
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class AddressTest : DescribeSpec({
    it("new contract address") {
        val address = Address.fromHexString("970e8128ab834e8eac17ab8e3812f010678cf791")
        Address.generate(address, 0u) shouldBe Address.fromHexString("333c3310824b7c685133f2bedb2ca4b8b4df633d")
        Address.generate(address, 1u) shouldBe Address.fromHexString("8bda78331c916a08481428e4b07c96d3e916d165")
        Address.generate(address, 2u) shouldBe Address.fromHexString("c9ddedf451bc62ce88bf9292afb13df35b670699")
    }
})
