package ethereum.p2p.node.record

interface IdentityScheme {
    fun verify(r: Record, sig: ByteArray)
    fun nodeAddr(r: Record): ByteArray?

    companion object {
        val default: IdentityScheme = V4IdentityScheme()
    }
}