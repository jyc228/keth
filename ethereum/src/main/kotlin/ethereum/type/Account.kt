package ethereum.type

import ethereum.collections.Hash
import ethereum.rlp.RLPEncoder
import java.math.BigInteger

fun Account(
    nonce: ULong = 0u,
    balance: BigInteger = BigInteger.ZERO,
    root: Hash = Hash.EMPTY_MPT_ROOT,
    codeHash: Hash = Hash.EMPTY_CODE
): Account = Account.Default(nonce, balance, root, codeHash)

interface Account {
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

    data class Default(
        override val nonce: ULong,
        override val balance: BigInteger,
        override val root: Hash,
        override val codeHash: Hash
    ) : Account
}

interface MutableAccount : Account {
    override var nonce: ULong
    override var balance: BigInteger
    override var codeHash: Hash
}
