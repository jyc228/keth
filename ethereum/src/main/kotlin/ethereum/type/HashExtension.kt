package ethereum.type

import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.state.account.StateRoot
import io.github.jyc228.ethereum.state.account.keccak256

private val hash_56e81 = Hash.fromHexString("56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421")
private val hash_1dcc4 = Hash.fromHexString("1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347")

fun Hash.Companion.keccak256(bytes: ByteArray) = Hash.fromByteArray(bytes.keccak256())
fun Hash.Companion.fromStateRoot(hash: StateRoot?) = hash?.let { Hash.fromByteArray(it.bytes) } ?: EMPTY_MPT_ROOT

val Hash.Companion.EMPTY_MPT_ROOT get() = hash_56e81
val Hash.Companion.EMPTY_TX_HASH get() = hash_56e81
val Hash.Companion.EMPTY_RECEIPT_HASH get() = hash_56e81
val Hash.Companion.EMPTY_WITHDRAWAL_HASH get() = hash_56e81
val Hash.Companion.EMPTY_UNCLE_HASH get() = hash_1dcc4

