package ethereum.p2p.node.record

import java.nio.ByteBuffer
import org.bouncycastle.jcajce.provider.digest.Keccak

class V4IdentityScheme : IdentityScheme {
    fun sign() {

    }

    override fun verify(r: Record, sig: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun nodeAddr(r: Record): ByteArray? {
        val pubKey = r[Secp256k1ENREntry] ?: return null
        return ByteBuffer.allocate(64)
            .put(pubKey.x.toByteArray())
            .put(pubKey.y.toByteArray())
            .array()
            .let { Keccak.Digest256().digest(it) }
    }
}