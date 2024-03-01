package ethereum.p2p.node.record

import ethereum.p2p.node.record.TestIdentityScheme.setTestSig
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.ByteBuffer

class ENREntryTest : StringSpec({
    "udp" {
        val udp = UdpENREntry(30309u)

        Record.fromEntries(udp)[UdpENREntry] shouldBe udp
    }

    "id" {
        val ip = IdENREntry("someid")

        Record.fromEntries(ip)[IdENREntry] shouldBe ip
    }

    "ipv4" {
        val ip = IPv4ENREntry("192.168.0.3")

        Record.fromEntries(ip)[IPv4ENREntry] shouldBe ip
    }

    "seq" {
        val record = Record()
        record.seq shouldBe 0u
        record.withEntry(UdpENREntry(1u)).seq shouldBe 0u
        record.setTestSig(byteArrayOf(5))
        record.seq shouldBe 0u
        record.withEntry(UdpENREntry(2u)).seq shouldBe 1u
    }
})

class TestId(bytes: ByteArray) : BytesENREntry(bytes) {
    companion object : ENREntry.Key<TestId>("testid", TestId::class)
}

private object TestIdentityScheme : IdentityScheme {
    override fun verify(r: Record, sig: ByteArray) {
        val entry = r[TestId] ?: error("")
        if (!sig.contentEquals(makeTestSig(entry.bytes, r.seq))) {
            error("ErrInvalidSig")
        }
    }

    override fun nodeAddr(r: Record): ByteArray? {
        return r[TestId]?.bytes
    }

    fun Record.setTestSig(id: ByteArray) {
        return this
            .withEntry(IdENREntry("test"))
            .withEntry(TestId(id))
            .setSig(this@TestIdentityScheme, makeTestSig(id, this.seq))
    }

    private fun makeTestSig(id: ByteArray, seq: ULong): ByteArray {
        val sig = ByteArray(id.size + 8)
        ByteBuffer.wrap(sig).putLong(seq.toLong()).put(id)
        return sig
    }
}