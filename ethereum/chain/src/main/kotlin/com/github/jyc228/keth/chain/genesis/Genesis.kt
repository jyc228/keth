package com.github.jyc228.keth.chain.genesis

import com.github.jyc228.keth.chain.EMPTY_RECEIPT_HASH
import com.github.jyc228.keth.chain.EMPTY_TX_HASH
import com.github.jyc228.keth.chain.EMPTY_UNCLE_HASH
import com.github.jyc228.keth.chain.EMPTY_WITHDRAWAL_HASH
import com.github.jyc228.keth.chain.fromStateRoot
import com.github.jyc228.keth.fork.HardFork
import com.github.jyc228.keth.state.StateDatabase
import com.github.jyc228.keth.type.Address
import com.github.jyc228.keth.type.Hash
import com.github.jyc228.keth.type.HexBigInt
import com.github.jyc228.keth.type.HexData
import com.github.jyc228.keth.type.HexULong
import java.math.BigInteger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
class Genesis(
    val nonce: HexULong = HexULong.ZERO,
    val timestamp: HexULong = HexULong.ZERO,
    val extraData: String = "",
    val gasLimit: HexBigInt = HexBigInt.ZERO,
    val difficulty: HexBigInt? = null,
    val mixHash: Hash = Hash.ZERO,
    val coinbase: Address? = null,
    val alloc: Map<Address, GenesisAccount> = emptyMap(),
    val number: HexULong = HexULong.ZERO,
    val gasUsed: HexBigInt = HexBigInt.ZERO,
    val parentHash: Hash = Hash.ZERO,
    val baseFee: HexBigInt? = null
) {
    companion object {
        fun fromNetworkName(networkName: String): Genesis {
            val stream = requireNotNull(Genesis::class.java.getResourceAsStream("/genesis/${networkName}.json")) {
                "$networkName.json not found"
            }
            return Json.decodeFromString<Genesis>(stream.bufferedReader().readText())
        }
    }
}

@Serializable
class GenesisAccount(
    val code: HexData? = null,
    val storage: Map<HexData, HexData> = emptyMap(),
    val balance: HexBigInt = HexBigInt.ZERO,
    val nonce: HexULong = HexULong.ZERO
) {
    override fun toString() = buildString {
        if (balance.number != BigInteger.ZERO) append("balance: $balance, ")
        if (nonce.number != 0uL) append("nonce: $nonce, ")
        if (code != null) append("codeSize: ${code.bytes.size}, ")
        if (storage.isNotEmpty()) append("storageSize: ${storage.size}, ")
        delete(lastIndex - 1, lastIndex)
    }
}

suspend fun com.github.jyc228.keth.chain.BlockHeader.Companion.fromGenesis(
    genesis: Genesis,
    db: StateDatabase,
    fork: HardFork
): com.github.jyc228.keth.chain.BlockHeader {
    return com.github.jyc228.keth.chain.BlockHeader(
        number = genesis.number.number,
        nonce = genesis.nonce.number,
        time = genesis.timestamp.number,
        parentHash = genesis.parentHash,
        extra = when (genesis.extraData.startsWith("0x")) {
            true -> HexData.fromHexString(genesis.extraData).bytes
            false -> genesis.extraData.toByteArray()
        },
        gasLimit = genesis.gasLimit.number,
        gasUsed = genesis.gasUsed.number,
        baseFee = when {
            genesis.baseFee != null -> genesis.baseFee.number
            "eip1559" in fork.eips -> BigInteger.valueOf(1000000000)
            else -> null
        },
        difficulty = genesis.difficulty?.number
            ?: BigInteger.valueOf(131072).takeIf { genesis.mixHash == Hash.ZERO }
            ?: BigInteger.ZERO,
        mixDigest = genesis.mixHash,
        coinbase = genesis.coinbase ?: Address.build { },
        root = Hash.fromStateRoot(db.createAccounts(genesis.alloc).commit()),
        uncleHash = Hash.EMPTY_UNCLE_HASH,
        txHash = Hash.EMPTY_TX_HASH,
        receiptHash = Hash.EMPTY_RECEIPT_HASH,
        bloom = ByteArray(256),
        withdrawalsHash = Hash.EMPTY_WITHDRAWAL_HASH.takeIf { "eip4895" in fork.eips },
        excessDataGas = null
    )
}

suspend fun StateDatabase.createAccounts(accounts: Map<Address, GenesisAccount>): StateDatabase {
    for ((address, account) in accounts) createAccount(address) {
        it.balance += account.balance.number
        it.nonce = account.nonce.number
        it.setCode(account.code?.bytes)
        account.storage.forEach { (k, v) -> it.storage.set(k.bytes, v.bytes) }
    }
    return this
}
