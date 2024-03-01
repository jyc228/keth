package ethereum.evm

import ethereum.collections.Hash
import ethereum.config.ForkConfig
import ethereum.core.state.StateDatabaseImpl
import java.math.BigInteger

class EVM(
    var depth: Int,
    val blockNumber: ULong,
    val db: StateDatabaseImpl,
    val interpreter: EVMInterpreter,
    val forkConfig: ForkConfig
) {
    fun call(
        caller: AccountReference,
        address: Address,
        input: ByteArray,
        gas: ULong,
        value: BigInteger,
    ) {
        if (value != BigInteger.ZERO && db.withAccountOrThrow(caller.address) { it.balance } < value) {
            error("ErrInsufficientBalance")
        }
        val snapshot = db.snapshot()
        if (db.accountTree[address] == null) {
            db.createAccount(address)
        }
        transfer(caller.address, address, value)
        db.withAccountOrThrow(address) {
            it.code
        }
    }

    fun callCode(
        caller: AccountReference,
        address: Address,
        input: ByteArray,
        gas: ULong,
        value: BigInteger,
    ) {
        if (value != BigInteger.ZERO && db.withAccountOrThrow(caller.address) { it.balance } < value) {
            error("ErrInsufficientBalance")
        }
        val snapshot = db.snapshot()
        if (db.accountTree[address] == null) {
            db.createAccount(address)
        }
        transfer(caller.address, address, value)
        val code = db.withAccountOrThrow(address) { it.code?.let { c -> ContractCode(c, it.codeHash) } }
    }

    fun delegateCall(
        caller: AccountReference,
        address: Address,
        input: ByteArray,
        gas: ULong
    ) {
    }

    fun staticCall(
        caller: AccountReference,
        address: Address,
        input: ByteArray,
        gas: ULong
    ) {
    }


    fun createContract(
        caller: AccountReference,
        code: ByteArray,
        gas: ULong,
        value: BigInteger,
    ) {
        createContract(
            caller = caller,
            address = Address.new(caller.address, db.withAccountOrNull(caller.address) { it?.nonce ?: 0u }),
            code = ContractCode(code),
            gas = gas,
            value = value,
            op = Instruction.Create
        )
    }

    fun createContract(
        caller: AccountReference,
        address: Address,
        code: ContractCode,
        gas: ULong,
        value: BigInteger,
        op: Instruction
    ) {
        if (depth > CallCreateDepth) {
            error("ErrDepth")
        }
        db.withAccountOrThrow(caller.address) {
            if (it.balance < value) error("ErrInsufficientBalance")
            if (it.nonce + 1u < it.nonce) error("ErrNonceUintOverflow")
            it.nonce += 1u
        }
        if (forkConfig.berlin.forked(blockNumber)) {
            // We add this to the access list _before_ taking a snapshot.
            // Even if the creation fails, the access-list change should not be rolled back
            db.accessList += address
        }
        db.withAccountOrNull(address) {
            if (it?.nonce != 0.toULong() && (it?.codeHash != Hash.EMPTY || it.codeHash != Hash.EMPTY_CODE)) {
                error("ErrContractAddressCollision")
            }
        }
        val snapshotId = db.snapshot()
        db.createAccount(address) {
            if (forkConfig.eip158.forked(blockNumber)) it.nonce = 1u
        }

        transfer(caller.address, address, value)
        val contract = ContractReference.new(address, caller, value, gas)
        interpreter.run(contract, null, false)
            .onSuccess { }
            .onFailure {
                if (forkConfig.homestead.forked(blockNumber) || it.message != "ErrCodeStoreOutOfGas") {
                    db.revertSnapshot(snapshotId)
                }
                if (it.message != "ErrExecutionReverted") {

                }
            }
    }

    private fun transfer(from: Address, to: Address, value: BigInteger) {
        db.withAccountOrThrow(from) { it.balance -= value }
        db.withAccountOrThrow(to) { it.balance += value }
    }

    companion object {
        val CallCreateDepth = 1024
        val MaxCodeSize = 24576
    }
}
