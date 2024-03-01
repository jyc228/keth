package ethereum.crypto

import ethereum.evm.Address
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CryptoTest : StringSpec({
    "test" {
        val private = ECDSAPrivateKey.fromHexString("289c2857d4598e37fb9647507e47a309d6133539bf21a8b9cb6df88fd5232032")
        private.s shouldBe "18368420523168362001751416900979172313964560713233930779034394675028290641970".toBigInteger()
        private.public.x shouldBe "56853879815914347742926672250398113661147572454539521302376574937137774599159".toBigInteger()
        private.public.y shouldBe "67659942069668491962054117788582837361011567284189205141936810330712159236093".toBigInteger()
    }

    "public key to address" {
        val private = ECDSAPrivateKey.fromHexString("289c2857d4598e37fb9647507e47a309d6133539bf21a8b9cb6df88fd5232032")
        val address = Address.fromPublicKey(private.public)
        address shouldBe Address.fromHexString("970e8128ab834e8eac17ab8e3812f010678cf791")
    }
})