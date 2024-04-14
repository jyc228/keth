package io.github.jyc228.ethereum

import kotlinx.serialization.Serializable

@Serializable
data class RpcTransaction(
    @Serializable(NullBlockHash::class)
    override val blockHash: Hash = Transaction.pendingBlockHash,

    @Serializable(NullBlockNumber::class)
    override val blockNumber: HexULong = Transaction.pendingBlockNumber,

    override val hash: Hash = Hash.empty,
    override val from: Address = Address.empty,
    override val to: Address? = null,
    override val input: String = "",
    override val value: HexBigInt = HexBigInt.ZERO,
    override val nonce: HexULong = HexULong.ZERO,
    override val gas: HexBigInt = HexBigInt.ZERO,
    override val gasPrice: HexBigInt? = null,

    @Serializable(NullTxIndex::class)
    override val transactionIndex: HexInt = HexInt.ZERO,
    override val type: TransactionType,
    override val v: HexBigInt? = null,
    override val r: HexBigInt? = null,
    override val s: HexBigInt? = null,
    override val chainId: HexULong? = null,
    override val yParity: HexULong? = null,

    @Serializable(NullList::class)
    override val accessList: List<Access> = emptyList(),
    @Serializable(NullGas::class)
    override val maxFeePerGas: HexBigInt = HexBigInt.ZERO,
    @Serializable(NullGas::class)
    override val maxPriorityFeePerGas: HexBigInt = HexBigInt.ZERO,
    @Serializable(NullGas::class)
    override val maxFeePerBlobGas: HexBigInt = HexBigInt.ZERO,
    @Serializable(NullList::class)
    override val blobVersionedHashes: List<Hash> = emptyList(),
) : Transaction
