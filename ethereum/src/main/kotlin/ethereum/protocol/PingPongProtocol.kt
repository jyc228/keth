package ethereum.protocol

import ethereum.p2p.Peer
import ethereum.p2p.Protocol
import ethereum.p2p.ProtocolId
import ethereum.p2p.node.record.ENREntry

class PingPongProtocol(
    override val id: ProtocolId,
    override val length: ULong,
    override val attributes: List<ENREntry>
) : Protocol {
    override fun run(peer: Peer) {
        TODO("Not yet implemented")
    }

    override fun nodeInfo() {
        TODO("Not yet implemented")
    }

    override fun peerInfo(id: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun candidates() {
    }
}