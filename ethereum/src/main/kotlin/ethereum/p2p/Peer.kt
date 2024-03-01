package ethereum.p2p

class Peer(
    val conn: Connection,
    val running: Map<String, ProtocolChannel>
) {

    companion object {
        fun newForTest(name: String, remote: List<ProtocolId>): Peer {
            return Peer(
                Connection(),
                matchProtocols(remote.map { NoOpProtocol(it) }, remote)
            )
        }
    }
}

