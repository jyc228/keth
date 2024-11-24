package com.github.jyc228.keth.state.account

import java.math.BigInteger

interface StateAccount {
    val nonce: ULong
    val balance: BigInteger
    val root: StorageRoot?
    val codeHash: CodeHash?

    companion object {
        fun of(nonce: ULong, balance: BigInteger, root: StorageRoot?, codeHash: CodeHash?) = ImmutableStateAccount(
            nonce = nonce,
            balance = balance,
            root = root,
            codeHash = codeHash
        )
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
        suspend fun get(key: ByteArray): ByteArray?
        suspend fun getCommittedState(key: ByteArray): ByteArray?
        suspend fun set(key: ByteArray, value: ByteArray?)
    }
}
