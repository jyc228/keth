package ethereum.type

import ethereum.collections.Hash
import ethereum.hexToByteArray
import ethereum.history.EIP2930
import ethereum.history.fork.HomesteadHardFork
import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.ECDSASignature
import io.github.jyc228.ethereum.HexBigInt
import io.github.jyc228.ethereum.HexULong
import io.github.jyc228.ethereum.TransactionType
import io.github.jyc228.ethereum.buildAccessListTransaction
import io.github.jyc228.ethereum.buildLegacyTransaction
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.math.BigInteger

class TransactionTest : StringSpec({
    "sig hash" {
        buildLegacyTransaction {
            to = Address.create("0x095e7baea6a6c7c4c2dfeb977efac326af552d87")
        }.let { HomesteadHardFork.hash(it) shouldBe Hash.fromHexString("c775b99e7ad12f50d819fcd602390467e28141316969f4b57f0626f74fe3b386") }
        buildLegacyTransaction {
            nonce = HexULong(3u)
            gasPrice = HexBigInt(1.toBigInteger())
            gas = HexBigInt(2000.toBigInteger())
            to = Address.create("0xb94f5374fce5edbc8e2a8697c15331677e6ebf0b")
            value = HexBigInt(10.toBigInteger())
            input = "0x5544"
            withSignature(
                HomesteadHardFork.signatureValues(
                    txType = TransactionType.Legacy,
                    sig = "98ff921201554726367d2be8c804a7ff89ccf285ebc57dff8ae4c44b9c19ac4a8887321be575c8095f789dd4c743dfe42c1820f9231f98a962b210e3ac2452a301".hexToByteArray()
                )
            )
        }.let { HomesteadHardFork.hash(it) shouldBe Hash.fromHexString("fe7a79529ed5f7c3375d06b26b186a8644e0e16c373d7a12be41c62d6042b77a") }
    }

    "eip2718 sig hash" {
        val buildAccessTx = { sig: ECDSASignature? ->
            buildAccessListTransaction {
                chainId = HexULong(1u)
                nonce = HexULong(3u)
                gasPrice = HexBigInt(BigInteger.ONE)
                gas = HexBigInt(25000.toBigInteger())
                to = Address.create("0xb94f5374fce5edbc8e2a8697c15331677e6ebf0b")
                value = HexBigInt(10.toBigInteger())
                input = "0x5544"
                if (sig != null) withSignature(sig)
            }
        }
        val signer = EIP2930.Signer(1u)
        val eip2930sig = signer.signatureValues(
            TransactionType.AccessList,
            "c9519f4f2b30335884581971573fadf60c6204f59a911df35ee8a540456b266032f1e8e2c5dd761f9e4f88f41c8310aeaba26a8bfcdacfedfa12ec3862d3752101".hexToByteArray()
        )
        signer.hash(buildAccessTx(null)) shouldBe Hash.fromHexString("49b486f0ec0a60dfbbca2d30cb07c9e8ffb2a2ff41f29a1ab6737475f6ff69f3")
        signer.hash(buildAccessTx(eip2930sig)) shouldBe Hash.fromHexString("49b486f0ec0a60dfbbca2d30cb07c9e8ffb2a2ff41f29a1ab6737475f6ff69f3")
    }
})
