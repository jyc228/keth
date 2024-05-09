package io.github.jyc228.ethereum.vm

import java.math.BigInteger

class EVMStack(private val elements: MutableList<EVMStackElement> = mutableListOf()) :
    MutableList<EVMStackElement> by elements {
    fun push(b: EVMStackElement) = apply { elements.add(b) }
    fun pop() = elements.removeLast()
    fun back(index: Int) = elements[elements.lastIndex - index]
}

data class EVMStackElement(
    private var _bytes: ByteArray? = null,
    private var _big: BigInteger? = null,
    private var _int: Int? = null,
    private var signed: Boolean = false,
) {
    init {
        if (_big != null && _big!! >= bigMax) _big = _big!! % bigMax
        while (_big != null && _big!! < BigInteger.ZERO) _big = _big!! + bigMax
    }

    val bytes: ByteArray
        get() {
            _big = _big?.let { _bytes = it.toByteArray(); null }
            _int = _int?.let { _bytes = it.toBigInteger().toByteArray(); null }
            return _bytes!!
        }

    val big: BigInteger
        get() {
            _bytes = _bytes?.let { _big = it.toBigInteger(); null }
            _int = _int?.let { _big = it.toBigInteger(); null }
            return _big!!
        }

    val int: Int
        get() {
            _bytes = _bytes?.let { _int = it.toBigInteger().toInt(); null }
            _big = _big?.let { _int = it.toInt(); null }
            return _int!!
        }


    fun signed() = apply {
        signed = true
        if (_big != null) _big = bytes.toBigInteger()
    }

    override operator fun equals(other: Any?): Boolean = big == (other as? EVMStackElement?)?.big
    operator fun compareTo(other: EVMStackElement): Int = big.compareTo(other.big)

    operator fun plus(other: EVMStackElement) = EVMStackElement(_big = big + other.big)
    operator fun times(other: EVMStackElement) = EVMStackElement(_big = big * other.big)
    operator fun minus(other: EVMStackElement) = EVMStackElement(_big = big - other.big)
    operator fun div(other: EVMStackElement) = when (other.big == BigInteger.ZERO) {
        true -> ZERO
        false -> EVMStackElement(_big = big / other.big)
    }

    operator fun rem(other: EVMStackElement) = when (other.big == BigInteger.ZERO) {
        true -> ZERO
        false -> EVMStackElement(_big = big % other.big)
    }

    infix fun and(other: EVMStackElement) = EVMStackElement(_big = big and other.big)
    infix fun or(other: EVMStackElement) = EVMStackElement(_big = big or other.big)
    infix fun xor(other: EVMStackElement) = EVMStackElement(_big = big xor other.big)
    infix fun pow(other: EVMStackElement) = EVMStackElement(_big = big.modPow(other.big, bigMax))
    fun not() = EVMStackElement(_big = big.not())

    private fun ByteArray.toBigInteger(): BigInteger {
        if (signed && size >= 32) return BigInteger(this.takeIf { size == 32 } ?: this.copyOfRange(size - 32, size))
        return BigInteger(1, this)
    }

    override fun hashCode(): Int {
        var result = _bytes?.contentHashCode() ?: 0
        result = 31 * result + (_big?.hashCode() ?: 0)
        result = 31 * result + (_int ?: 0)
        result = 31 * result + signed.hashCode()
        return result
    }

    override fun toString(): String {
        if (_bytes != null) return _bytes.contentToString()
        if (_big != null) return _big.toString()
        if (_int != null) return _int.toString()
        return "invalid"
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun toHexString(): String {
        if (_bytes != null) return "0x${_bytes!!.toHexString().trimStart('0').ifBlank { '0' }}"
        if (_big != null) return "0x${_big!!.toString(16)}"
        if (_int != null) return "0x${_int!!.toString(16)}"
        return "invalid"
    }

    companion object {
        private val bigMax = BigInteger.TWO.pow(256)

        val ZERO = EVMStackElement(_big = BigInteger.ZERO)
        val ONE = EVMStackElement(_big = BigInteger.ONE)
        val MAX = EVMStackElement(_big = bigMax - BigInteger.ONE)
    }
}