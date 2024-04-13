package io.github.jyc228.ethereum

import ethereum.rlp.RLPEncoder
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import java.math.BigInteger
import kotlinx.serialization.Serializable

@OptIn(ExperimentalStdlibApi::class)
class TransactionRlpTest : DescribeSpec({
    val rightvrsTx = buildLegacyTransaction {
        nonce = HexULong(3u)
        gasPrice = HexBigInt(1.toBigInteger())
        gas = HexBigInt(2000.toBigInteger())
        to = Address.create("b94f5374fce5edbc8e2a8697c15331677e6ebf0b")
        value = HexBigInt(10.toBigInteger())
        input = "0x5544"
        v = HexBigInt("28".toBigInteger())
        r = HexBigInt("69203107126561044063792201335198645470894861817391589087942477974338948148298".toBigInteger())
        s = HexBigInt("61753417600470177491367431579682165468528826526989350017953370930365939864227".toBigInteger())
    }

    val accessListTx = buildAccessListTransaction {
        chainId = HexULong(1u)
        nonce = HexULong(3u)
        gasPrice = HexBigInt(BigInteger.ONE)
        gas = HexBigInt(25000.toBigInteger())
        to = Address.create("b94f5374fce5edbc8e2a8697c15331677e6ebf0b")
        value = HexBigInt(10.toBigInteger())
        input = "0x5544"
        v = HexBigInt("1".toBigInteger())
        r = HexBigInt("91059096689536776704183241450754166041908571671439170983238491105279368439392".toBigInteger())
        s = HexBigInt("23043059890712937631597966600904561206765736472665286406396949327530054350113".toBigInteger())
    }

    it("legacy tx to rlp") {
        val result = TransactionRlp.encode(rightvrsTx)
        result shouldBe "f86103018207d094b94f5374fce5edbc8e2a8697c15331677e6ebf0b0a8255441ca098ff921201554726367d2be8c804a7ff89ccf285ebc57dff8ae4c44b9c19ac4aa08887321be575c8095f789dd4c743dfe42c1820f9231f98a962b210e3ac2452a3".hexToByteArray()
    }

    it("access list tx to rlp") {
        val result = TransactionRlp.encode(accessListTx)
        result shouldBe "01f8630103018261a894b94f5374fce5edbc8e2a8697c15331677e6ebf0b0a825544c001a0c9519f4f2b30335884581971573fadf60c6204f59a911df35ee8a540456b2660a032f1e8e2c5dd761f9e4f88f41c8310aeaba26a8bfcdacfedfa12ec3862d37521".hexToByteArray()
        RLPEncoder.encode(result) shouldBe "b86601f8630103018261a894b94f5374fce5edbc8e2a8697c15331677e6ebf0b0a825544c001a0c9519f4f2b30335884581971573fadf60c6204f59a911df35ee8a540456b2660a032f1e8e2c5dd761f9e4f88f41c8310aeaba26a8bfcdacfedfa12ec3862d37521".hexToByteArray()
    }

    context("test sepolia block 0x56edea tx") {
        val test = decodeJsonResource<RawTxTest>("/transaction/raw_tx.json")
        withData(nameFn = { "${it.transactionIndex.number} : ${it.type}" }, test.input) { tx ->
            val rlp = test.expected[tx.transactionIndex.number]
            TransactionRlp.encode(tx) shouldBe rlp.removePrefix("0x").hexToByteArray()
        }
    }
})

@Serializable
private data class RawTxTest(val blockNumber: String, val input: List<RpcTransaction>, val expected: List<String>)