package io.github.jyc228.ethereum.vm

import ethereum.rlp.RLPEncoder
import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.state.account.CodeHash
import io.github.jyc228.ethereum.state.account.ManagedStateAccount
import io.github.jyc228.ethereum.state.account.keccak256
import java.nio.ByteBuffer

fun Address.Companion.generate(address: Address, nonce: ULong): Address {
    val data = RLPEncoder.encodeArray { addBytes(address.bytes).addULong(nonce) }
    return Address.fromByteArray(data.keccak256().copyOfRange(12, 32))
}

fun Address.Companion.generate(address: Address, salt: ByteArray, initHash: ByteArray): Address {
    val data = ByteBuffer
        .allocate(1 + address.bytes.size + salt.size + initHash.size)
        .put(0xFF.toByte()).put(address.bytes).put(salt).put(initHash)
        .array()
    return Address.fromByteArray(data.keccak256().copyOfRange(12, 32))
}

class EVMContract(
    val address: Address,
    val code: ByteArray,
    val codeHash: CodeHash
) {
    constructor(address: Address, code: ByteArray) : this(address, code, CodeHash.keccak256FromBytes(code))

//    val caller: AccountReference,
//    val self: AccountReference,
//    val jumpdests: Map<Hash, ByteArray>,
//    val analysis: ByteArray,
//    val code: ContractCode,
//    val codeAddress: Address,
//    val input: ByteArray,
//    var gas: ULong,
//    val value: BigInteger

    companion object {
        suspend fun of(account: ManagedStateAccount) = EVMContract(
            address = account.address,
            code = requireNotNull(account.getCode()),
            codeHash = requireNotNull(account.codeHash)
        )
    }
}
