package ethereum.core

import ethereum.collections.Hash
import ethereum.config.ChainConfig
import ethereum.core.state.StateDatabase
import ethereum.evm.Address
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

    fun commitAlloc(db: StateDatabase): Hash {
        alloc.forEach { (addr, account) ->
            db.withAccountOrCreate(addr) {
                it.balance += account.balance
                it.code = account.code
                it.nonce = account.nonce
                account.storage.forEach { (k, v) -> it.storage[k] = v }
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

        fun dev(gasLimit: BigInteger, faucet: Address? = null) = Genesis(
            config = null,
            extraData = ByteArray(32) + (faucet?.bytes ?: byteArrayOf()) + ByteArray(65),
            gasLimit = gasLimit,
            difficulty = BigInteger.ONE,
            baseFee = BigInteger.valueOf(1000000000),
            alloc = buildMap {
                this[Address.fromByte(1)] = Account(balance = BigInteger.ONE) // ECRecover
                this[Address.fromByte(2)] = Account(balance = BigInteger.ONE) // SHA256
                this[Address.fromByte(3)] = Account(balance = BigInteger.ONE) // RIPEMD
                this[Address.fromByte(4)] = Account(balance = BigInteger.ONE) // Identity
                this[Address.fromByte(5)] = Account(balance = BigInteger.ONE) // ModExp
                this[Address.fromByte(6)] = Account(balance = BigInteger.ONE) // ECAdd
                this[Address.fromByte(7)] = Account(balance = BigInteger.ONE) // ECScalarMul
                this[Address.fromByte(8)] = Account(balance = BigInteger.ONE) // ECPairing
                this[Address.fromByte(9)] = Account(balance = BigInteger.ONE) // BLAKE2b
                faucet?.let { this[it] = Account(balance = BigInteger.ONE.shiftLeft(256) - BigInteger.valueOf(9)) }
            }
        )
    }
}
