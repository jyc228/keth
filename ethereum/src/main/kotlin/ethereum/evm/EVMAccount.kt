package ethereum.evm

import ethereum.core.state.account.Address
import ethereum.core.state.account.CodeHash
import ethereum.core.state.account.ManagedStateAccount
import java.math.BigInteger

interface EVMAccount {
    val address: Address
}

class EVMAddress(override val address: Address) : EVMAccount

class EVMContract(
    override val address: Address,
    val code: ByteArray,
    val codeHash: CodeHash
) : EVMAccount {

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
        fun new(address: Address, caller: EVMAccount, value: BigInteger, gas: ULong): EVMContract {
            error("")
        }

        suspend fun of(account: ManagedStateAccount) = EVMContract(
            address = account.address,
            code = requireNotNull(account.getCode()),
            codeHash = requireNotNull(account.codeHash)
        )
    }
}
