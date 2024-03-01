package ethereum.evm

import ethereum.collections.Hash
import java.math.BigInteger

interface AccountReference {
    val address: Address
}

class AddressReference(override val address: Address) : AccountReference

class ContractCode(val bytes: ByteArray, val hash: Hash) {
    constructor(bytes: ByteArray) : this(bytes, Hash.keccak256FromBytes(bytes))
}

class ContractReference(
    override val address: Address,
    val caller: AccountReference,
    val self: AccountReference,
    val jumpdests: Map<Hash, ByteArray>,
    val analysis: ByteArray,
    val code: ContractCode,
    val codeAddress: Address,
    val input: ByteArray,
    var gas: ULong,
    val value: BigInteger
) : AccountReference {
    fun getOpCode(pc: Int): Instruction {
        if (pc < code.bytes.size) {
            return Instruction.fromOpCode(code.bytes[pc])
        }
        return Instruction.Stop
    }

    fun useGas(amount: ULong): Boolean {
        if (gas < amount) {
            return false
        }
        gas -= amount
        return true
    }

    companion object {
        fun new(address: Address, caller: AccountReference, value: BigInteger, gas: ULong): ContractReference {
            error("")
        }
    }
}
