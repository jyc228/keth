package ethereum.rlp

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import java.math.BigInteger
import org.junit.jupiter.api.Test

class RLPExtensionTest {

    @Test
    fun `simple class test`() {
        val input = Account(BigInteger.ZERO, 10u, Address(byteArrayOf(1, 2, 3)))

        val output = input.toRlp().rlpToObject<Account>()

        input shouldBe output
    }

    @Test
    fun `array test`() {
        val input = listOf(
            Account(BigInteger.ZERO, 10u, Address(byteArrayOf(1, 2, 3))),
            Account(BigInteger.TEN, 20u, Address(byteArrayOf(4, 5, 6)))
        ).let { Accounts(it, 10u) }

        val output = input.toRlp().rlpToObject<Accounts>()

        input.accounts shouldContainAll output.accounts
    }

    @Test
    fun `encode with optional test`() {
        val input = Header(1u, null)

        input.toRlp().toList() shouldContainAll listOf(194.toByte(), 1.toByte(), 128.toByte())
    }

    @Test
    fun `decode with optional test`() {
        val input = byteArrayOf(194.toByte(), 1.toByte(), 128.toByte())

        input.rlpToObject<Header>() shouldBe Header(1u, BigInteger.ZERO)
    }

    data class Account(val balance: BigInteger, val nonce: ULong, val address: Address)

    data class Header(
        val nonce: ULong,
        val baseFee: BigInteger?,
        val withdrawalsHash: BigInteger? = null,
        val excessDataGas: BigInteger? = null,
    )

    class Accounts(val accounts: List<Account>, val size: ULong)

    data class Address(val bytes: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return bytes.contentEquals((other as Address).bytes)
        }

        override fun hashCode(): Int = bytes.contentHashCode()
    }
}