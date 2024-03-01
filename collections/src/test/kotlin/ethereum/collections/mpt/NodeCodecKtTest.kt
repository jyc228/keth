package ethereum.collections.mpt

import org.junit.jupiter.api.Test

class NodeCodecKtTest {

    @Test
    fun `decode test`() {

        println(ValueNode(byteArrayOf(1, 2)).encode().contentToString())
    }

    fun newBranchNode(): BranchNode {
        return BranchNode { ValueNode(byteArrayOf(it.toByte())) }
    }
}
