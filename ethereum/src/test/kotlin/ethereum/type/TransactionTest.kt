package ethereum.type

import ethereum.collections.Hash
import ethereum.evm.Address
import ethereum.hexToByteArray
import ethereum.history.EIP2930
import ethereum.history.fork.HomesteadHardFork
import ethereum.type.builder.AccessListTransactionBuilder
import ethereum.type.builder.LegacyTransactionBuilder
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.math.BigInteger
import java.util.HexFormat

class TransactionTest : StringSpec({
    val emptyTx = LegacyTransactionBuilder().build {
        to = Address.fromHexString("095e7baea6a6c7c4c2dfeb977efac326af552d87")
    }

    val rightvrsTx = LegacyTransactionBuilder().build {
        nonce = 3u
        gasPrice = 1.toBigInteger()
        gas = 2000u
        to = Address.fromHexString("b94f5374fce5edbc8e2a8697c15331677e6ebf0b")
        value = 10.toBigInteger()
        data = HexFormat.of().parseHex("5544")
        sig = HomesteadHardFork.signatureValues(
            txType = LegacyTransaction.TYPE,
            sig = "98ff921201554726367d2be8c804a7ff89ccf285ebc57dff8ae4c44b9c19ac4a8887321be575c8095f789dd4c743dfe42c1820f9231f98a962b210e3ac2452a301".hexToByteArray()
        )
    }

    val accessListTxBuilder = AccessListTransactionBuilder().mutate {
        chainId = 1u
        nonce = 3u
        gasPrice = BigInteger.ONE
        gas = 25000u
        to = Address.fromHexString("b94f5374fce5edbc8e2a8697c15331677e6ebf0b")
        value = 10.toBigInteger()
        data = HexFormat.of().parseHex("5544")
    }

    "decode empty tx type" {

    }

    "sig hash" {
        HomesteadHardFork.hash(emptyTx) shouldBe Hash.fromHexString("c775b99e7ad12f50d819fcd602390467e28141316969f4b57f0626f74fe3b386")
        HomesteadHardFork.hash(rightvrsTx) shouldBe Hash.fromHexString("fe7a79529ed5f7c3375d06b26b186a8644e0e16c373d7a12be41c62d6042b77a")
    }

    "encode" {
        val result = rightvrsTx.toRlp()

        result shouldBe "f86103018207d094b94f5374fce5edbc8e2a8697c15331677e6ebf0b0a8255441ca098ff921201554726367d2be8c804a7ff89ccf285ebc57dff8ae4c44b9c19ac4aa08887321be575c8095f789dd4c743dfe42c1820f9231f98a962b210e3ac2452a3".hexToByteArray()
    }

    "eip 2718 encode" {
        val tx = accessListTxBuilder.buildAccessListTx {
            val signer = EIP2930.Signer(1u)
            sig = signer.signatureValues(
                AccessListTransaction.TYPE,
                "c9519f4f2b30335884581971573fadf60c6204f59a911df35ee8a540456b266032f1e8e2c5dd761f9e4f88f41c8310aeaba26a8bfcdacfedfa12ec3862d3752101".hexToByteArray()
            )
        }

        val result = tx.toRlp()

        result shouldBe "b86601f8630103018261a894b94f5374fce5edbc8e2a8697c15331677e6ebf0b0a825544c001a0c9519f4f2b30335884581971573fadf60c6204f59a911df35ee8a540456b2660a032f1e8e2c5dd761f9e4f88f41c8310aeaba26a8bfcdacfedfa12ec3862d37521".hexToByteArray()
    }

    "eip2718 sig hash" {
        val signer = EIP2930.Signer(1u)
        val eip2930sig = signer.signatureValues(
            AccessListTransaction.TYPE,
            "c9519f4f2b30335884581971573fadf60c6204f59a911df35ee8a540456b266032f1e8e2c5dd761f9e4f88f41c8310aeaba26a8bfcdacfedfa12ec3862d3752101".hexToByteArray()
        )
        signer.hash(accessListTxBuilder.buildAccessListTx()) shouldBe Hash.fromHexString("49b486f0ec0a60dfbbca2d30cb07c9e8ffb2a2ff41f29a1ab6737475f6ff69f3")
        signer.hash(accessListTxBuilder.buildAccessListTx {
            sig = eip2930sig
        }) shouldBe Hash.fromHexString("49b486f0ec0a60dfbbca2d30cb07c9e8ffb2a2ff41f29a1ab6737475f6ff69f3")
    }
})
