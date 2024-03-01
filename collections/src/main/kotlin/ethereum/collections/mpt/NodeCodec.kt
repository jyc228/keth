package ethereum.collections.mpt

import ethereum.rlp.RLPBuilder
import ethereum.rlp.RLPDecoder
import ethereum.rlp.RLPEncoder
import ethereum.rlp.RLPItem

fun MerklePatriciaTrieNode.encodeToRLP(collapse: Boolean): ByteArray =
    RLPEncoder.encode { encodeNode(this@encodeToRLP, collapse) }

private fun RLPBuilder.encodeNode(node: MerklePatriciaTrieNode?, collapse: Boolean): RLPBuilder = when (node) {
    null -> addEmptyString()
    is HashNode -> addBytes(node.hash)
    is ValueNode -> addBytes(node.bytes)
    is ExtensionNode -> when (collapse && node.hash.isNotEmpty()) {
        true -> addBytes(node.hash)
        false -> addArray { addBytes(node.compactKey).encodeNode(node.value, collapse) }
    }

    is BranchNode -> when (collapse && node.hash.isNotEmpty()) {
        true -> addBytes(node.hash)
        false -> addArray { node.children.forEach { encodeNode(it, collapse) } }
    }
}

fun MerklePatriciaTrieNode.Companion.decodeFromRlp(
    hash: ByteArray?,
    rlp: ByteArray
): MerklePatriciaTrieNode = decodeNode(hash, RLPDecoder.decode(rlp))

private fun decodeNode(hash: ByteArray?, rlp: RLPItem): MerklePatriciaTrieNode = when (rlp) {
    is RLPItem.Byte -> TODO()
    is RLPItem.Str -> HashNode(rlp.value.map { it.code.toByte() }.toByteArray())
    is RLPItem.Arr -> when (rlp.size) {
        2 -> {
            val key = HexKey.fromCompact(rlp[0].castStr().value.map { it.code.toByte() }.toByteArray())
            ExtensionNode(
                key = key,
                value = when (key.hasTerminator) {
                    true -> ValueNode(rlp[1].castStr().value.map { it.code.toByte() }.toByteArray())
                    false -> decodeNode(null, rlp[1])
                },
                hash = hash ?: byteArrayOf()
            ).apply { dirty = false }
        }

        17 -> BranchNode(hash) { i ->
            if (i <= 15) decodeNode(null, rlp[i])
            else if (i == 16) rlp[i].castStr().value.takeIf { it.isNotEmpty() }?.let { ValueNode(it.toByteArray()) }
            else error("")
        }.apply { dirty = false }

        else -> error("invalid number of list elements ${rlp.size}")
    }
}

private fun RLPItem.string(): String = when (this) {
    is RLPItem.Arr -> TODO()
    is RLPItem.Byte -> value.toInt().toChar().toString()
    is RLPItem.Str -> value
}
