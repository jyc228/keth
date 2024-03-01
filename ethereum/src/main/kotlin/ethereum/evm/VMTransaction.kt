package ethereum.evm

import ethereum.collections.Hash
import ethereum.config.ForkConfig
import ethereum.core.state.AccessList
import ethereum.core.state.StateDatabase
import ethereum.history.fork.FrontierHardFork
import ethereum.history.fork.IstanbulHardFork
import java.math.BigInteger

class VMTransaction(
    val bn: ULong,
    val forkConfig: ForkConfig,
    val message: Message,
    val db: StateDatabase
) {
    val contractCreation get() = message.to == null
    // TransitionDb will transition the state by applying the current message and
// returning the evm execution result with following fields.
//
//   - used gas: total gas used (including gas being refunded)
//   - returndata: the returned data from evm
//   - concrete execution error: various EVM errors which abort the execution, e.g.
//     ErrOutOfGas, ErrExecutionReverted
//
// However if any consensus issue encountered, return the error directly with
// nil evm execution result.

    fun execute() {
        // First check this message satisfies all consensus rules before
        // applying the message. The rules include these clauses
        //
        // 1. the nonce of the message caller is correct
        // 2. caller has enough balance to cover transaction fee(gaslimit * gasprice)
        // 3. the amount of gas required is available in the block
        // 4. the purchased gas is enough to cover intrinsic usage
        // 5. there is no overflow when calculating intrinsic gas
        // 6. caller has enough balance to cover asset transfer for **topmost** call

        // Check clauses 1-3, buy gas if everything is correct
        preCheck()
        val gas = intrinsicGas()
        if (message.value != BigInteger.ZERO && db.withAccountOrThrow(message.from) { it.balance } < message.value) {
            error("ErrInsufficientFundsForTransfer")
        }

        if (contractCreation && forkConfig.shanghai.forked(bn) && (message.data?.size ?: 0) > maxInitCodeSize) {
            error("ErrMaxInitCodeSizeExceeded")
        }

        db.withAccountOrThrow(message.from) { it.nonce += 1u }

    }

    fun preCheck() {
        throwIfNonceIncorrect()
        throwIfLowBalance()
    }

    fun throwIfNonceIncorrect() {
        val nonce = db.withAccountOrThrow(message.from) { it.nonce }
        when (nonce.compareTo(message.nonce)) {
            -1 -> error("ErrNonceTooLow")
            1 -> error("ErrNonceTooHigh")
            0 -> return
        }
    }

    fun throwIfLowBalance() {
        if (
            db.withAccountOrThrow(message.from) { it.balance } < when (message.gasFeeCap == null) {
                true -> message.gasPrice * message.gasLimit
                false -> message.gasFeeCap * message.gasLimit + message.value
            }
        ) error("ErrInsufficientFunds")
    }

    // computes the 'intrinsic gas' for a message with the given data.
    fun intrinsicGas(): BigInteger {
        var gas = when (contractCreation && forkConfig.homestead.forked(bn)) {
            true -> 53000.toBigInteger()
            false -> 21000.toBigInteger()
        }
        if (message.data?.isNotEmpty() == true) {
            // Bump the required gas by the amount of transactional data
            val nz = message.data.count { it != 0.toByte() }
            val nonZeroGas = when (forkConfig.istanbul.forked(bn)) {
                true -> IstanbulHardFork.txDataNonZeroGas
                false -> FrontierHardFork.txDataNonZeroGas
            }
            gas += nonZeroGas * nz.toBigInteger()
            val z = message.data.size - nz
            gas += txDataZeroGas * z.toBigInteger()
            if (message.to == null && forkConfig.shanghai.forked(bn)) {

            }
        }
        return gas
    }

    //            var nz uint64
//            for _, byt := range data {
//            if byt != 0 {
//                nz++
//            }
//        }
//            // Make sure we don't exceed uint64 for all data combinations
//            nonZeroGas := params.TxDataNonZeroGasFrontier
//            if isEIP2028 {
//                nonZeroGas = params.TxDataNonZeroGasEIP2028
//            }
//            if (math.MaxUint64-gas)/nonZeroGas < nz {
//                return 0, ErrGasUintOverflow
//            }
//            gas += nz * nonZeroGas
//
//            z := dataLen - nz
//            if (math.MaxUint64-gas)/params.TxDataZeroGas < z {
//                return 0, ErrGasUintOverflow
//            }
//            gas += z * params.TxDataZeroGas
//
//            if isContractCreation && isEIP3860 {
//                lenWords := toWordSize(dataLen)
//                if (math.MaxUint64-gas)/params.InitCodeWordGas < lenWords {
//                    return 0, ErrGasUintOverflow
//                }
//                gas += lenWords * params.InitCodeWordGas
//            }
//        }
//        if accessList != nil {
//            gas += uint64(len(accessList)) * params.TxAccessListAddressGas
//            gas += uint64(accessList.StorageKeys()) * params.TxAccessListStorageKeyGas
//        }
//        return gas, nil
//    }
    // A Message contains the data derived from a single transaction that is relevant to state processing.
    class Message(
        val to: Address?,
        val from: Address,
        val nonce: ULong,
        val value: BigInteger,
        val gasLimit: BigInteger,
        val gasPrice: BigInteger,
        val gasFeeCap: BigInteger?,
        val gasTipCap: BigInteger,
        val data: ByteArray?,
        val accessList: AccessList,
        val blobHashes: List<Hash>,

        // When SkipAccountChecks is true, the message nonce is not checked against the
        // account nonce in state. It also disables checking that the sender is an EOA.
        // This field will be set to true for operations like RPC eth_call.
        val skipAccountChecks: Boolean
    )

    companion object {
        // Per byte of data attached to a transaction that equals zero. NOTE: Not payable on data of calls between transactions.
        val txDataZeroGas: BigInteger = 4.toBigInteger()

        // Maximum bytecode to permit for a contract
        val maxCodeSize: Int = 24576

        // Maximum initcode to permit in a creation transaction and create instructions
        val maxInitCodeSize: Int = 2 * maxCodeSize
    }
}