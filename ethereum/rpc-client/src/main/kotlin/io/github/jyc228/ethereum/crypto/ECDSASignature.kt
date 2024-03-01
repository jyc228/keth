package io.github.jyc228.ethereum.crypto

import java.math.BigInteger

interface ECDSASignature {
    val v: BigInteger
    val r: BigInteger
    val s: BigInteger

    data class Mutable(
        override var r: BigInteger,
        override var s: BigInteger,
        override var v: BigInteger
    ) : ECDSASignature

    companion object {
        fun fromBytes(bytes: ByteArray): ECDSASignature {
            require(bytes.size == 65) { "wrong size for signature: ${bytes.size}" }
            return Mutable(
                r = BigInteger(1, bytes.sliceArray(0..<32)),
                s = BigInteger(1, bytes.sliceArray(32..<64)),
                v = BigInteger(1, byteArrayOf((bytes[64] + 27).toByte()))
            )
        }
    }
}