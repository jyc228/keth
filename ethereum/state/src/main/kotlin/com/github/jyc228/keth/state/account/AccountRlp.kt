package com.github.jyc228.keth.state.account

import com.github.jyc228.keth.rlp.RLPDecoder
import com.github.jyc228.keth.rlp.RLPEncoder

object AccountRlp {
    fun encode(account: StateAccount) = RLPEncoder.encodeArray {
        addULong(account.nonce)
        addBigInt(account.balance)
        addBytes(account.root?.bytes ?: StorageRoot.empty.bytes)
        addBytes(account.codeHash?.bytes ?: CodeHash.empty.bytes)
    }

    fun decode(rlp: ByteArray) = with(RLPDecoder.decode(rlp).castArr()) {
        ImmutableStateAccount(
            nonce = this[0].toULong(),
            balance = this[1].toBigInt(),
            root = StorageRoot.fromByteArray(this[2].castStr().value.map { it.code.toByte() }.toByteArray()),
            codeHash = CodeHash.fromByteArray(this[3].castStr().value.map { it.code.toByte() }.toByteArray())
        )
    }
}