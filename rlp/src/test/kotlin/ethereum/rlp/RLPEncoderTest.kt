package ethereum.rlp

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RLPEncoderTest {
    @Test
    fun `단일 문자열 인코딩`() {
        val input = "d"

        val rlp = RLPEncoder.encode(input)

        rlp.size shouldBe 2
        rlp[0] shouldBe 129.toByte()
        rlp[1] shouldBe 100.toByte()
    }

    @Test
    fun `길이 0 ~ 55 사이의 문자열 인코딩`() {
        val input = "abc"

        val rlp = RLPEncoder.encode(input)

        rlp shouldBe byteArrayOf(
            (128 + input.length).toByte(),
            *input.toByteArray()
        )
    }

    @Test
    fun `길이 55 초과 문자열 인코딩`() {
        val input = "a".repeat(1024)

        val rlp = RLPEncoder.encode(input)

        rlp shouldBe byteArrayOf(185.toByte(), 4, 0, *input.toByteArray())
    }

    @Test
    fun `배열 아이템 길이 55 이하 인코딩`() {
        val input = listOf("ab", "cd")

        val rlp = RLPEncoder.encodeArray { input.forEach { addString(it) } }

        rlp shouldBe byteArrayOf(198.toByte(), 130.toByte(), 97, 98, 130.toByte(), 99, 100)
    }

    @Test
    fun `배열 아이템 길이 55 초과 인코딩`() {
        val input = listOf("a".repeat(50), "a".repeat(50))

        val rlp = RLPEncoder.encodeArray { input.forEach { addString(it) } }

        rlp shouldBe byteArrayOf(
            248.toByte(),
            102,
            178.toByte(),
            *(1..50).map { 97.toByte() }.toByteArray(),
            178.toByte(),
            *(1..50).map { 97.toByte() }.toByteArray()
        )
    }
}
