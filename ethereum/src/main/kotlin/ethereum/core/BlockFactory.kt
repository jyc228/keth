package ethereum.core

import ethereum.collections.Hash
import ethereum.collections.MerkleTree
import ethereum.collections.mpt.MerklePatriciaTrie
import ethereum.core.database.TreeDatabase
import ethereum.core.state.StateDatabaseImpl
import ethereum.evm.Address
import ethereum.rlp.RLPEncoder
import ethereum.type.Block
import ethereum.type.BlockBody
import ethereum.type.BlockHeader
import ethereum.type.builder.BlockHeaderBuilder
import io.github.jyc228.ethereum.Transaction
import io.github.jyc228.ethereum.TransactionReceipt
import io.github.jyc228.ethereum.TransactionRlp
import java.math.BigInteger
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
        return tree.rootHash()?.let(::Hash) ?: Hash.EMPTY_MPT_ROOT
    }


    fun Block.Companion.fromGenesis(genesis: Genesis): Block {
        return Block(
            header = BlockHeader(
                number = genesis.number,
                nonce = ByteArray(8),
                time = genesis.timestamp,
                parentHash = genesis.parentHash ?: Hash.EMPTY,
                extra = genesis.extraData,
                gasLimit = genesis.gasLimit.takeIf { it != BigInteger.ZERO } ?: BigInteger.valueOf(4712388),
                gasUsed = genesis.gasUsed,
                baseFee = when {
                    genesis.baseFee != null -> genesis.baseFee
                    genesis.isBlockForked(genesis.config?.londonBlock, 0u) -> BigInteger.valueOf(1000000000)
                    else -> null
                },
                difficulty = genesis.difficulty
                    ?: BigInteger.valueOf(131072).takeIf { genesis.mixHash == null || genesis.mixHash == Hash.EMPTY }
                    ?: BigInteger.ZERO,
                mixDigest = genesis.mixHash ?: Hash.EMPTY,
                coinbase = genesis.coinbase ?: Address.EMPTY,
                root = genesis.commitAlloc(StateDatabaseImpl.empty(TreeDatabase.memory())),
                uncleHash = Hash.EMPTY_UNCLE_HASH,
                txHash = Hash.EMPTY_TX_HASH,
                receiptHash = Hash.EMPTY_RECEIPT_HASH,
                bloom = ByteArray(256),
                withdrawalsHash = Hash.EMPTY_WITHDRAWAL_HASH
                    .takeIf { genesis.isTimestampForked(genesis.config?.shanghaiTime, genesis.timestamp) },
                excessDataGas = null
            ),
            body = BlockBody(emptyList(), emptyList(), emptyList())
        )
    }
}