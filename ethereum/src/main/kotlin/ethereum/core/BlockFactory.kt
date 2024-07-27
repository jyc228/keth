package ethereum.core

import com.github.jyc228.keth.collections.MerkleTree
import com.github.jyc228.keth.collections.mpt.MerklePatriciaTrie
import com.github.jyc228.keth.rlp.RLPEncoder
import com.github.jyc228.keth.type.Hash
import com.github.jyc228.keth.type.Transaction
import com.github.jyc228.keth.type.TransactionReceipt
import com.github.jyc228.keth.type.TransactionRlp
import ethereum.type.Block
import ethereum.type.BlockBody
import ethereum.type.BlockHeader
import ethereum.type.EMPTY_MPT_ROOT
import ethereum.type.builder.BlockHeaderBuilder
import kotlin.math.min

object BlockFactory {
    fun Block.Companion.new(
        header: BlockHeaderBuilder,
        transactions: List<Transaction>,
        uncles: List<BlockHeader>,
        receipts: List<TransactionReceipt>
    ): Block {
        return Block(
            header = header.build {
                txHash = deriveSha(transactions, MerklePatriciaTrie.empty { null }) { transaction ->
                    TransactionRlp.encode(transaction)
                }
                receiptHash = deriveSha(receipts, MerklePatriciaTrie.empty { null }) { receipt ->
                    TODO()
                }
            },
            body = BlockBody(emptyList(), emptyList(), emptyList()),
            receivedAt = null,
            receivedFrom = null
        )
    }

    // DeriveSha creates the tree hashes of transactions, receipts, and withdrawals in a block header.
    fun <E> deriveSha(list: List<E>, tree: MerkleTree, encode: (E) -> ByteArray): Hash {
        // StackTrie requires values to be inserted in increasing hash order, which is not the
        // order that `list` provides hashes in. This insertion sequence ensures that the
        // order is correct.
        //
        // The error returned by hasher is omitted because hasher will produce an incorrect
        // hash in case any error occurs.
        (1..min(127, list.size)).forEach { i ->
            tree[RLPEncoder.encode { addULong(i.toULong()) }] = encode(list[i])
        }
        list.firstOrNull()?.let { tree[RLPEncoder.encode { addULong(0u) }] = encode(it) }
        (128..list.size).forEach { i ->
            tree[RLPEncoder.encode { addULong(i.toULong()) }] = encode(list[i])
        }
        return tree.rootHash()?.let(Hash::fromByteArray) ?: Hash.EMPTY_MPT_ROOT
    }
}