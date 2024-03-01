package ethereum.p2p.node

import ethereum.p2p.node.record.IPv4ENREntry
import ethereum.p2p.node.record.IPv6ENREntry
import ethereum.p2p.node.record.IdentityScheme
import ethereum.p2p.node.record.Record
import ethereum.p2p.node.record.TcpENREntry
import ethereum.p2p.node.record.UdpENREntry

class Node(
    val id: ByteArray,
    val record: Record
) {
    val seq get() = record.seq
    val ip: ByteArray? get() = record[IPv4ENREntry]?.bytes ?: record[IPv6ENREntry]?.bytes
    val udp: ULong get() = record[UdpENREntry]?.value ?: 0u
    val tcp: ULong get() = record[TcpENREntry]?.value ?: 0u

    companion object {
        fun fromRawUrl(rawUrl: String) {

        }

        fun of(scheme: IdentityScheme, rawUrl: String): Node {
            if (rawUrl.startsWith("enode://")) {

            }
            if (rawUrl.startsWith("enr:")) {

            }
            error("")
        }
    }
}