package ethereum.collections.mpt

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class HexKeyTest {
    @Test
    fun testx() {
        HexKey.just().decodeToByteArray() shouldBe byteArrayOf()
        HexKey.just(16).decodeToByteArray() shouldBe byteArrayOf()

        HexKey.fromString("").nibbles shouldBe byteArrayOf(16)

        HexKey.fromBytes(0x12, 0x34, 0x56).nibbles shouldBe byteArrayOf(1, 2, 3, 4, 5, 6, 16)
        HexKey.just(1, 2, 3, 4, 5, 6, 16).decodeToByteArray() shouldBe byteArrayOf(0x12, 0x34, 0x56)
        HexKey.just(1, 2, 3, 4, 5, 6).decodeToByteArray() shouldBe byteArrayOf(0x12, 0x34, 0x56)

        HexKey.fromBytes(0x12, 0x34, 0x5).nibbles shouldBe byteArrayOf(1, 2, 3, 4, 0, 5, 16)
        HexKey.just(1, 2, 3, 4, 0, 5, 16).decodeToByteArray() shouldBe byteArrayOf(0x12, 0x34, 0x5)
    }
}
