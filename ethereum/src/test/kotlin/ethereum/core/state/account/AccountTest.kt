package ethereum.core.state.account

import io.github.jyc228.ethereum.state.account.AccountRlp
import io.github.jyc228.ethereum.state.account.CodeHash
import io.github.jyc228.ethereum.state.account.StateAccount
import io.github.jyc228.ethereum.state.account.StorageRoot
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.math.BigInteger
import org.junit.jupiter.api.Test

class AccountTest {
    @Test
    fun `rlp test`() {
        val account = StateAccount.of(
            1u,
            BigInteger.ZERO,
            StorageRoot.fromHexString("56e81f171bcc55a6ff8345e692c0a86e5b48e01b996cadc001622fb5e363b421"),
            CodeHash.fromHexString("56e81f171bcc55a6ff8345e692c0a86e5b48e01b996cadc001622fb5e363b421")
        )
        val rlp = AccountRlp.encode(account)
        assertSoftly(AccountRlp.decode(rlp)) {
            nonce shouldBe account.nonce
            balance shouldBe account.balance
            root?.bytes shouldBe account.root?.bytes
            codeHash?.bytes shouldBe account.codeHash?.bytes
        }
        rlp.map { it.toUByte() }
            .joinToString(" ") shouldBe "248 68 1 128 160 86 232 31 23 27 204 85 166 255 131 69 230 146 192 168 110 91 72 224 27 153 108 173 192 1 98 47 181 227 99 180 33 160 86 232 31 23 27 204 85 166 255 131 69 230 146 192 168 110 91 72 224 27 153 108 173 192 1 98 47 181 227 99 180 33"
    }

    @Test
    fun `rlp test v2`() {
        val account = StateAccount.of(1u, BigInteger.ZERO, null, null)
        val rlp = AccountRlp.encode(account)
        assertSoftly(AccountRlp.decode(rlp)) {
            nonce shouldBe account.nonce
            balance shouldBe account.balance
            root?.bytes shouldBe account.root?.bytes
            codeHash?.bytes shouldBe account.codeHash?.bytes
        }
    }
}
