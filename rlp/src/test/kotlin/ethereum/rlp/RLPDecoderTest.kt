package ethereum.rlp

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class RLPDecoderTest {

    @Test
    fun `단일 문자열 디코딩`() {
        val input = byteArrayOf(129.toByte(), 100)

        val result = RLPDecoder.decode(input)

        (result as RLPItem.Str).value shouldBe "d"
    }

    @Test
    fun `길이 0 ~ 55 사이의 문자열 디코딩`() {
        val input = byteArrayOf((128 + 3).toByte(), 97, 98, 99)

        val result = RLPDecoder.decode(input)

        (result as RLPItem.Str).value shouldBe "abc"
    }

    @Test
    fun `길이 55 초과 문자열 디코딩`() {
        val input = byteArrayOf(185.toByte(), 4, 0, *(1..1024).map { 97.toByte() }.toByteArray())

        val result = RLPDecoder.decode(input)

        (result as RLPItem.Str).value shouldBe "a".repeat(1024)
    }

    @Test
    fun `배열 아이템 길이 55 이하 디코딩2`() {
        val input = byteArrayOf(198.toByte(), 130.toByte(), 97, 98, 130.toByte(), 99, 100)

        val result = RLPDecoder.decode(input)

        result.castArr().map { it.castStr().value } shouldContainAll listOf("ab", "cd")
    }

    @Test
    fun `배열 아이템 길이 55 초과 디코딩`() {
        val input = byteArrayOf(
            248.toByte(),
            102,
            178.toByte(),
            *(1..50).map { 97.toByte() }.toByteArray(),
            178.toByte(),
            *(1..50).map { 97.toByte() }.toByteArray()
        )

        val result = RLPDecoder.decode(input)

        result.castArr().map { it.castStr().value } shouldContainAll listOf("a".repeat(50), "a".repeat(50))
    }
}
