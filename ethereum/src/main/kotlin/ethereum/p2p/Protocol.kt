package ethereum.p2p

import ethereum.p2p.node.record.ENREntry

data class ProtocolId(val name: String, val version: UInt)

interface Protocol {
    val id: ProtocolId
    val length: ULong
    val attributes: List<ENREntry>

    fun run(peer: Peer)
    fun nodeInfo()
    fun peerInfo(id: ByteArray)
    fun candidates()
}

class PeerCapability(val name: String, val version: UInt)

data class NoOpProtocol(
    override val id: ProtocolId,
    override val length: ULong = 0u,
    override val attributes: List<ENREntry> = emptyList()
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
        TODO("Not yet implemented")
    }
}

class ProtocolChannel(
    val metadata: Protocol,
    val offset: ULong
)

fun matchProtocols(local: List<Protocol>, remoteCapabilities: List<ProtocolId>): Map<String, ProtocolChannel> {
    val protocolMap = local.associateBy { it.id }
    var offset: ULong = 16u
    return remoteCapabilities
        .sortedWith(compareBy({ it.name }, { it.version }))
        .fold(mutableMapOf()) { result, remote ->
            protocolMap[remote]?.let { protocol ->
                offset -= result[remote.name]?.metadata?.length ?: 0u
                result[remote.name] = ProtocolChannel(protocol, offset)
                offset += protocol.length
            }
            result
        }
}