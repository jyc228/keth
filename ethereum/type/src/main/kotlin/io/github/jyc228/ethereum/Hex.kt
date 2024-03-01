package io.github.jyc228.ethereum

import java.math.BigInteger
import kotlin.reflect.full.companionObjectInstance
import kotlinx.serialization.Serializable

interface HexString {
    val hex: String
}

@Serializable(HashSerializer::class)
data class Hash(override val hex: String) : HexString {
    override fun toString(): String = hex

    companion object : StrictHexStringFactory<Hash>(::Hash, true, 66)
}

@Serializable(AddressSerializer::class)
data class Address(override val hex: String) : HexString {
    override fun toString(): String = hex

    companion object : StrictHexStringFactory<Address>(::Address, true, 42)
}

sealed class HexNumber<T, SELF : HexNumber<T, SELF>>(
    private val lazyHex: Lazy<String>,
    private val lazyNumber: Lazy<T>
) : HexString {
    constructor(toHex: () -> String, number: T) : this(lazy(LazyThreadSafetyMode.NONE, toHex), lazyOf(number))
    constructor(hex: String, toNumber: () -> T) : this(lazyOf(hex), lazy(LazyThreadSafetyMode.NONE, toNumber))

    override val hex: String get() = lazyHex.value
    val number: T get() = lazyNumber.value

    abstract operator fun compareTo(other: SELF): Int
    abstract operator fun plus(other: SELF): SELF
    abstract operator fun minus(other: SELF): SELF
    override fun toString(): String = "$number | $hex"
}

@Serializable(HexIntSerializer::class)
class HexInt : HexNumber<Int, HexInt> {
    constructor(number: Int) : super({ "0x${number.toString(16)}" }, number)
    constructor(hex: String) : super(hex, { hex.removePrefix("0x").toInt(16) })

    override operator fun compareTo(other: HexInt): Int = number.compareTo(other.number)
    override operator fun plus(other: HexInt): HexInt = HexInt(number + other.number)
    override operator fun minus(other: HexInt): HexInt = HexInt(number - other.number)

    companion object {
        val ZERO = HexInt("0x0")
    }
}

@Serializable(HexULongSerializer::class)
class HexULong : HexNumber<ULong, HexULong> {
    constructor(number: ULong) : super({ "0x${number.toString(16)}" }, number)
    constructor(hex: String) : super(hex, { hex.removePrefix("0x").toULong(16) })

    override operator fun compareTo(other: HexULong): Int = number.compareTo(other.number)
    override operator fun plus(other: HexULong): HexULong = HexULong(number + other.number)
    override operator fun minus(other: HexULong): HexULong = HexULong(number - other.number)

    companion object {
        val ZERO = HexULong("0x0")
    }
}

@Serializable(HexBigIntSerializer::class)
class HexBigInt : HexNumber<BigInteger, HexBigInt> {
    constructor(number: BigInteger) : super({ "0x${number.toString(16)}" }, number)
    constructor(hex: String) : super(hex, { hex.removePrefix("0x").toBigInteger(16) })

    override operator fun compareTo(other: HexBigInt): Int = number.compareTo(other.number)
    override operator fun plus(other: HexBigInt): HexBigInt = HexBigInt(number + other.number)
    override operator fun minus(other: HexBigInt): HexBigInt = HexBigInt(number - other.number)

    companion object {
        val ZERO = HexBigInt("0x0")
    }
}

@Serializable(HexDataSerializer::class)
data class HexData(override val hex: String) : HexString {
    fun toInt(): Int = hex.removePrefix("0x").toInt(16)
    fun toLong(): Long = hex.removePrefix("0x").toLong(16)
    fun toULong(): ULong = hex.removePrefix("0x").toULong(16)
    fun toBigInt(): BigInteger = hex.removePrefix("0x").toBigInteger(16)

    fun toText(): String = buildString {
        val value = hex.removePrefix("0x")
        for (i in value.indices step 2) {
            val char = value.substring(i, i + 2).toInt(16).toChar()
            if (!char.isISOControl()) append(char)
        }
    }.trim()

    override fun toString(): String = hex

    companion object : HexStringFactory<HexData>(::HexData)
}

sealed class HexStringFactory<T : HexString>(protected val newInstance: (String) -> T) {
    val empty: T by lazy(LazyThreadSafetyMode.NONE) { create("") }
    open fun create(input: String): T = newInstance(input.lowercase())
}

sealed class StrictHexStringFactory<T : HexString>(
    newInstance: (String) -> T,
    private val requirePrefix: Boolean,
    val valueLength: Int
) : HexStringFactory<T>(newInstance) {
    override fun create(input: String): T {
        if (valueLength == input.length) {
            if (requirePrefix xor input.startsWith("0x")) {
                error("invalid input. require 0x prefix? $requirePrefix, input : $input")
            }
            return newInstance(input.lowercase())
        }
        val refinedInput = buildString(input.length) {
            val refinedInputLength = valueLength - if (requirePrefix) 2 else 0
            input.forEach { append(it.lowercaseChar()) }
            if (startsWith("0x")) delete(0, 2)
            if (length % 2 != 0) insert(0, '0')
            while (length < refinedInputLength) insert(0, "00")
            while (length > refinedInputLength && this[0] == '0' && this[1] == '0') delete(0, 2)
            if (requirePrefix) insert(0, "0x")
        }
        if (valueLength != refinedInput.length) error("invalid length")
        return newInstance(refinedInput)
    }
}

inline fun <reified T : HexString> String.toHex(): T {
    return (T::class.companionObjectInstance as HexStringFactory<*>).create(this) as T
}

inline fun <reified T : HexString> emptyHex(): T {
    return (T::class.companionObjectInstance as HexStringFactory<*>).empty as T
}