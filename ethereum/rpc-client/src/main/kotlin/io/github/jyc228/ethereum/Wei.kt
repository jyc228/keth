package io.github.jyc228.ethereum

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.math.BigInteger

@JvmInline
@JsonSerialize(using = Serializer::class)
value class Wei(val v: BigInteger) {
    operator fun plus(wei: Wei) = Wei(v + wei.v)
    operator fun minus(wei: Wei) = Wei(v - wei.v)
    operator fun times(wei: Wei) = Wei(v * wei.v)
    operator fun times(value: Int) = Wei(v * value.toBigInteger())
    operator fun times(value: Double) = Wei((v.toBigDecimal() * value.toBigDecimal()).toBigInteger())
    operator fun div(wei: Wei) = Wei(v / wei.v)
    operator fun compareTo(other: Wei) = v.compareTo(other.v)
    override fun toString(): String {
        if (v > ONE_ETH.v) {
            return "${v.toBigDecimal() / ONE_ETH.v.toBigDecimal()}.eth"
        }
        return "$v.wei"
    }

    companion object {
        val ONE_GWEI = Wei(1000000000.toBigInteger())
        val ONE_ETH = Wei(1000000000000000000.toBigInteger())

        val Int.eth get() = Wei(this.toBigInteger()) * ONE_ETH
        val Int.wei get() = Wei(this.toBigInteger())
        val Long.wei get() = Wei(this.toBigInteger())
        val BigInteger.wei get() = Wei(this)
    }
}

private class Serializer : JsonSerializer<Wei>() {
    override fun serialize(value: Wei, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(value.v.toString())
    }
}
