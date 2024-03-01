package ethereum.crypto

import java.math.BigInteger
import java.security.KeyFactory
import java.security.Security
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.util.HexFormat
import kotlin.random.Random
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.bouncycastle.jce.spec.ECPublicKeySpec

class ECDSAPublicKey(val x: BigInteger, val y: BigInteger) {
    fun bytes(): ByteArray {
        return byteArrayOf(4) + x.toByteArray().dropWhile { it == 0.toByte() } + y.toByteArray()
            .dropWhile { it == 0.toByte() }
    }

    companion object {
        fun fromS(s: BigInteger): ECDSAPublicKey {
            val public = secp256k1.generateECDSAPublicKey(s)
            return ECDSAPublicKey(public.w.affineX, public.w.affineY)
        }
    }
}

class ECDSAPrivateKey(val s: BigInteger) {
    val public by lazy(LazyThreadSafetyMode.NONE) { ECDSAPublicKey.fromS(s) }

    companion object {
        fun fromHexString(hex: String): ECDSAPrivateKey {
            return ECDSAPrivateKey(secp256k1.generateECDSAPrivateKey(hex).s)
        }

        fun random(): ECDSAPrivateKey {
            while (true) {
                val s = BigInteger(Random.nextBytes(secp256k1.param.curve.fieldSize / 8))
                if (BigInteger.ZERO < s && s < secp256k1.param.n) {
                    return ECDSAPrivateKey(secp256k1.generateECDSAPrivateKey(s).s)
                }
            }
        }
    }
}

private object secp256k1 {
    init {
        Security.addProvider(org.bouncycastle.jce.provider.BouncyCastleProvider())
    }

    val param get() = ECNamedCurveTable.getParameterSpec("secp256k1")

    fun generateECDSAPrivateKey(hex: String): ECPrivateKey {
        val bytes = HexFormat.of().parseHex(hex)
        require(8 * bytes.size == param.curve.fieldSize) { "Invalid length, need ${param.curve.fieldSize} bits" }
        return generateECDSAPrivateKey(BigInteger(1, bytes))
    }

    fun generateECDSAPrivateKey(s: BigInteger): ECPrivateKey {
        require(s < param.n) { "Invalid private key, >=N" }
        require(s.signum() > 0) { "Invalid private key, zero or negative" }
        val privateKeySpec = ECPrivateKeySpec(s, param)
        return KeyFactory.getInstance("EC").generatePrivate(privateKeySpec) as ECPrivateKey
    }

    fun generateECDSAPublicKey(s: BigInteger): ECPublicKey {
        val publicKeySpec = ECPublicKeySpec(param.g.multiply(s), param)
        return KeyFactory.getInstance("EC").generatePublic(publicKeySpec) as ECPublicKey
    }
}