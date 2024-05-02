package ethereum.core.state.account

import ethereum.collections.Hash
import ethereum.evm.Address
import ethereum.rlp.RLPEncoder
import ethereum.rlp.rlpToObject
import java.math.BigInteger

fun StateAccount(
    nonce: ULong = 0u,
    balance: BigInteger = BigInteger.ZERO,
    root: Hash = Hash.EMPTY_MPT_ROOT,
    codeHash: Hash = Hash.EMPTY_CODE
): StateAccount = ImmutableStateAccount(nonce, balance, root, codeHash)

interface StateAccount {
    val nonce: ULong
    val balance: BigInteger
    val root: Hash
    val codeHash: Hash

    fun encodeToRlp() = RLPEncoder.encodeArray {
        addULong(nonce)
        addBigInt(balance)
        addBytes(root.bytes)
        addBytes(codeHash.bytes)
    }

    companion object {
        fun fromRlp(rlp: ByteArray): StateAccount = rlp.rlpToObject<ImmutableStateAccount>()
    }
}

interface ManagedStateAccount : StateAccount {
    override var nonce: ULong
    override var balance: BigInteger
    val address: Address
    val storage: Storage

    suspend fun getCode(): ByteArray?
    suspend fun setCode(code: ByteArray?)

    interface Storage {
        suspend fun get(key: Hash): Hash
        suspend fun getCommittedState(key: Hash): Hash
        suspend fun set(key: Hash, value: Hash)
    }
}

data class ImmutableStateAccount(
    override val nonce: ULong,
    override val balance: BigInteger,
    override val root: Hash,
    override val codeHash: Hash
) : StateAccount
