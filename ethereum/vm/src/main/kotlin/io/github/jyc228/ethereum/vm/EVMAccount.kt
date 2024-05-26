package io.github.jyc228.ethereum.vm

import io.github.jyc228.ethereum.state.account.Address
import io.github.jyc228.ethereum.state.account.CodeHash
import io.github.jyc228.ethereum.state.account.ManagedStateAccount

interface EVMAccount {
    val address: Address
}

class EVMAddress(override val address: Address) : EVMAccount

class EVMContract(
    override val address: Address,
    val code: ByteArray,
    val codeHash: CodeHash
) : EVMAccount {
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
