package ethereum.history

import com.github.jyc228.keth.type.Address
import com.github.jyc228.keth.type.Hash
import ethereum.config.ForkConfig
import ethereum.type.BlockHeader
import io.kotest.matchers.shouldBe
import java.math.BigInteger
import org.junit.jupiter.api.Test

class EIP1559Test {

    @Test
    fun `compute bases fee`() {
        computeBaseFee(10000000) shouldBe EIP1559.initialBaseFee // usage == target
        computeBaseFee(9000000) shouldBe 987500000.toBigInteger() // usage below target
        computeBaseFee(11000000) shouldBe 1012500000.toBigInteger() // usage above target
    }

    private fun computeBaseFee(parentGasUsed: Int): BigInteger {
        val header = BlockHeader(
            parentHash = Hash.unsafe(""),
            uncleHash = Hash.unsafe(""),
            coinbase = Address.build { },
            root = Hash.unsafe(""),
            txHash = Hash.unsafe(""),
            receiptHash = Hash.unsafe(""),
            bloom = ByteArray(256),
            difficulty = null,
            number = 32u,
            gasLimit = 20000000.toBigInteger(),
            gasUsed = parentGasUsed.toBigInteger(),
            time = 0u,
            extra = byteArrayOf(),
            mixDigest = Hash.unsafe(""),
            nonce = ByteArray(8),
            baseFee = EIP1559.initialBaseFee,
            withdrawalsHash = null,
            excessDataGas = BigInteger.ZERO
        )
        return EIP1559.computeBaseFee(ForkConfig.allForked(), header)
    }
}