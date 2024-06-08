package ethereum.core

import io.github.jyc228.ethereum.Hash
import io.github.jyc228.keth.collections.MerkleTree
import io.github.jyc228.keth.collections.mpt.MerklePatriciaTrie
import ethereum.core.database.TreeDatabase
import ethereum.core.repository.ContractCodeRepository
import ethereum.rlp.RLPEncoder
import ethereum.type.Block
import ethereum.type.BlockBody
import ethereum.type.BlockHeader
import ethereum.type.EMPTY_MPT_ROOT
import ethereum.type.EMPTY_RECEIPT_HASH
import ethereum.type.EMPTY_TX_HASH
import ethereum.type.EMPTY_UNCLE_HASH
import ethereum.type.EMPTY_WITHDRAWAL_HASH
import ethereum.type.builder.BlockHeaderBuilder
import ethereum.type.fromStateRoot
import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.Transaction
import io.github.jyc228.ethereum.TransactionReceipt
import io.github.jyc228.ethereum.TransactionRlp
import io.github.jyc228.ethereum.state.OnchainStateDatabase
import java.math.BigInteger
import kotlin.math.min
import kotlinx.coroutines.runBlocking

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


    fun Block.Companion.fromGenesis(genesis: Genesis): Block {
        return Block(
            header = BlockHeader(
                number = genesis.number,
                nonce = ByteArray(8),
                time = genesis.timestamp,
                parentHash = genesis.parentHash ?: Hash.unsafe(""),
                extra = genesis.extraData,
                gasLimit = genesis.gasLimit.takeIf { it != BigInteger.ZERO } ?: BigInteger.valueOf(4712388),
                gasUsed = genesis.gasUsed,
                baseFee = when {
                    genesis.baseFee != null -> genesis.baseFee
                    genesis.isBlockForked(genesis.config?.londonBlock, 0u) -> BigInteger.valueOf(1000000000)
                    else -> null
                },
                difficulty = genesis.difficulty
                    ?: BigInteger.valueOf(131072).takeIf { genesis.mixHash == null || genesis.mixHash == Hash.unsafe("") }
                    ?: BigInteger.ZERO,
                mixDigest = genesis.mixHash ?: Hash.unsafe(""),
                coinbase = genesis.coinbase ?: Address.build {  },
                root = runBlocking {
                    val db = TreeDatabase.memory()
                    Hash.fromStateRoot(
                        genesis.commitAlloc(
                            OnchainStateDatabase.empty(
                                db,
                                ContractCodeRepository(db.db)
                            )
                        )
                    )
                },
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