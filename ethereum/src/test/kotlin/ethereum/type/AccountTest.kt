package ethereum.type

import ethereum.collections.Hash
import ethereum.rlp.rlpToObject
import ethereum.rlp.toRlp
import io.kotest.matchers.shouldBe
import java.math.BigInteger
import org.junit.jupiter.api.Test

class AccountTest {
    @Test
    fun `rlp test`() {
        val account = Account(
            1u,
            BigInteger.ZERO,
            Hash.fromHexString("56e81f171bcc55a6ff8345e692c0a86e5b48e01b996cadc001622fb5e363b421"),
            Hash.fromHexString("56e81f171bcc55a6ff8345e692c0a86e5b48e01b996cadc001622fb5e363b421")
        )
        val rlp = account.toRlp()
        rlp.map { it.toUByte() }
            .joinToString(" ") shouldBe "248 68 1 128 160 86 232 31 23 27 204 85 166 255 131 69 230 146 192 168 110 91 72 224 27 153 108 173 192 1 98 47 181 227 99 180 33 160 86 232 31 23 27 204 85 166 255 131 69 230 146 192 168 110 91 72 224 27 153 108 173 192 1 98 47 181 227 99 180 33"

        val accountFromRlp: Account = rlp.rlpToObject<Account.Default>()

        accountFromRlp shouldBe account
    }
}
