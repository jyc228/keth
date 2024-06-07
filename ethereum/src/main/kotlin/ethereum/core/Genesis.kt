package ethereum.core

import io.github.jyc228.ethereum.Hash
import ethereum.config.ChainConfig
import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.state.StateDatabase
import io.github.jyc228.ethereum.state.account.StateRoot
import java.math.BigInteger

class Genesis(
    val config: ChainConfig? = null,
    val nonce: ULong = 0u,
    val timestamp: ULong = 0u,
    val extraData: ByteArray = byteArrayOf(),
    val gasLimit: BigInteger = BigInteger.ZERO,
    val difficulty: BigInteger? = null,
    val mixHash: Hash? = null,
    val coinbase: Address? = null,
    val alloc: Map<Address, Account> = emptyMap(),

    val number: ULong = 0u,
    val gasUsed: BigInteger = BigInteger.ZERO,
    val parentHash: Hash? = null,
    val baseFee: BigInteger? = null
) {
    fun isBlockForked(s: ULong?, head: ULong): Boolean {
        if (s == null) return false
        return s <= head
    }

    fun isTimestampForked(s: ULong?, head: ULong): Boolean {
        if (s == null) return false
        return s <= head
    }

    suspend fun commitAlloc(db: StateDatabase): StateRoot? {
        alloc.forEach { (addr, account) ->
            db.withAccountOrCreate(addr) {
                it.balance += account.balance
                it.nonce = account.nonce
                it.setCode(account.code)
                account.storage.forEach { (k, v) -> it.storage.set(k.bytes, v.bytes) }
            }
        }
        return db.commit(false)
    }

    class Account(
        val code: ByteArray? = null,
        val storage: Map<Hash, Hash> = emptyMap(),
        val balance: BigInteger,
        val nonce: ULong = 0u,
        val privateKey: ByteArray? = null
    )

    companion object {
        fun default() = Genesis(
            config = null,
            nonce = 66u,
            timestamp = 0u,
            extraData = byteArrayOf(),
            gasLimit = BigInteger.valueOf(5000),
            difficulty = BigInteger.valueOf(17179869184)
        )

        @OptIn(ExperimentalStdlibApi::class)
        fun dev(gasLimit: BigInteger, faucet: Address? = null) = Genesis(
            config = null,
            extraData = ByteArray(32) + (faucet?.hex?.hexToByteArray() ?: byteArrayOf()) + ByteArray(65),
            gasLimit = gasLimit,
            difficulty = BigInteger.ONE,
            baseFee = BigInteger.valueOf(1000000000),
            alloc = buildMap {
                // @formatter:off
                this[Address.fromHexString("0000000000000000000000000000000000000001")] = Account(balance = BigInteger.ONE) // ECRecover
                this[Address.fromHexString("0000000000000000000000000000000000000002")] = Account(balance = BigInteger.ONE) // SHA256
                this[Address.fromHexString("0000000000000000000000000000000000000003")] = Account(balance = BigInteger.ONE) // RIPEMD
                this[Address.fromHexString("0000000000000000000000000000000000000004")] = Account(balance = BigInteger.ONE) // Identity
                this[Address.fromHexString("0000000000000000000000000000000000000005")] = Account(balance = BigInteger.ONE) // ModExp
                this[Address.fromHexString("0000000000000000000000000000000000000006")] = Account(balance = BigInteger.ONE) // ECAdd
                this[Address.fromHexString("0000000000000000000000000000000000000007")] = Account(balance = BigInteger.ONE) // ECScalarMul
                this[Address.fromHexString("0000000000000000000000000000000000000008")] = Account(balance = BigInteger.ONE) // ECPairing
                this[Address.fromHexString("0000000000000000000000000000000000000009")] = Account(balance = BigInteger.ONE) // BLAKE2b
                // @formatter:on
                faucet?.let { this[it] = Account(balance = BigInteger.ONE.shiftLeft(256) - BigInteger.valueOf(9)) }
            }
        )
    }
}
