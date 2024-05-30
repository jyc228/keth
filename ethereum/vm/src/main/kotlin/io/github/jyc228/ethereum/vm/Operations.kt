package io.github.jyc228.ethereum.vm

import io.github.jyc228.ethereum.state.account.Address
import io.github.jyc228.ethereum.state.account.keccak256
import java.math.BigInteger
import java.nio.ByteBuffer
import kotlin.math.max

// @formatter:off
fun OperationBuilder.withOpCode(opCode: OpCode): OperationBuilder { when (opCode) { // https://ethervm.io/
// @formatter:on
    OpCode.STOP -> execute { result = EVMReturn.success(byteArrayOf()) }
    OpCode.ADD -> poppush { (a, b) -> a + b }.gas3()
    OpCode.MUL -> poppush { (a, b) -> a * b }.gas5()
    OpCode.SUB -> poppush { (a, b) -> a - b }.gas3()
    OpCode.DIV -> poppush { (a, b) -> a / b }.gas5()
    OpCode.SDIV -> poppush { (a, b) -> a.signed() / b.signed() }.gas5()
    OpCode.MOD -> poppush { (a, b) -> a % b }.gas5()
    OpCode.SMOD -> poppush { (a, b) -> a.signed() % b.signed() }.gas5()
    OpCode.ADDMOD -> poppush { (a, b, n) -> (a + b) % n }.gas8()
    OpCode.MULMOD -> poppush { (a, b, n) -> (a * b) % n }.gas8()
    OpCode.EXP -> poppush { (base, exp) -> base pow exp }.extraGas { (_, exp) -> ((exp.big.bitLength() + 7) / 8 * if (vmConfig.eip160) 50 else 10) + 10 }

    OpCode.SIGNEXTEND -> poppush { (b, x) ->
        if (b.big >= 32.toBigInteger() || b.big < 0.toBigInteger()) return@poppush x
        val result = when (x.bytes.size > b.int && x.bytes[x.bytes.lastIndex - b.int] < 0) {
            true -> ByteArray(32).apply { fill(0xFF.toByte(), 0, lastIndex - b.int) }
            false -> ByteArray((b.int + 1) * 8)
        }
        result.write(result.lastIndex - b.int, x.bytes.sliceArrayLast(b.int + 1))
        result.toElement()
    }.gas5()

    OpCode.LT -> poppush { (a, b) -> if (b > a) EVMStackElement.ONE else EVMStackElement.ZERO }.gas3()
    OpCode.GT -> poppush { (a, b) -> if (b < a) EVMStackElement.ONE else EVMStackElement.ZERO }.gas3()
    OpCode.SLT -> poppush { (a, b) -> if (b.signed() > a.signed()) EVMStackElement.ONE else EVMStackElement.ZERO }.gas3()
    OpCode.SGT -> poppush { (a, b) -> if (b.signed() < a.signed()) EVMStackElement.ONE else EVMStackElement.ZERO }.gas3()
    OpCode.EQ -> poppush { (a, b) -> if (b == a) EVMStackElement.ONE else EVMStackElement.ZERO }.gas3()
    OpCode.ISZERO -> poppush { (a) -> if (a == EVMStackElement.ZERO) EVMStackElement.ONE else EVMStackElement.ZERO }.gas3()
    OpCode.AND -> poppush { (a, b) -> b and a }.gas3()
    OpCode.OR -> poppush { (a, b) -> b or a }.gas3()
    OpCode.XOR -> poppush { (a, b) -> b xor a }.gas3()
    OpCode.NOT -> poppush { (a) -> a.not() }.gas3()
    OpCode.BYTE -> poppush { (a, b) ->
        val index = a.big
        if (32.toBigInteger() <= index) return@poppush EVMStackElement.ZERO
        val pos = index.toInt() - 32 + b.bytes.size
        if (pos < 0 || 32 <= pos) return@poppush EVMStackElement.ZERO
        byteArrayOf(b.bytes[pos]).toElement()
    }.gas3()

    OpCode.SHL -> if (vmConfig.eip145) poppush { (shift, value) -> runIf(shift.big <= big256) { (value.big shl shift.int).toElement() } }.gas3()
    OpCode.SHR -> if (vmConfig.eip145) poppush { (shift, value) -> runIf(shift.big <= big256) { (value.big shr shift.int).toElement() } }.gas3()
    OpCode.SAR -> if (vmConfig.eip145) poppush { (shift, value) ->
        when (shift.big <= big256) {
            true -> when (value.bytes[0] < 0) {
                true -> (value.big shr shift.int or EVMStackElement.MAX.big.shl(256 - shift.int)).toElement()
                false -> (value.big shr shift.int).toElement()
            }

            false -> runIf(value.bytes[0] < 0) { EVMStackElement.MAX }
        }
    }.gas3()

    OpCode.KECCAK256 -> poppush { (offset, size) -> memory.read(offset.int, size.int).keccak256().toElement() }
        .memorySize { (offset, size) -> offset.int + size.int }
        .gas(30).extraGas { (_, size) -> memoryGasCost() + (size.int.wordSize * keccak256Gas) }

    OpCode.ADDRESS -> push { contract.address.toElement() }.gas2()

    OpCode.BALANCE -> poppush { (address) ->
        db.findAccount(address.toAddress())?.balance?.toElement() ?: EVMStackElement.ZERO
    }.gas(
        when {
            vmConfig.eip1884 -> 700
            vmConfig.eip150 -> 400
            else -> 20
        }
    )

    OpCode.ORIGIN -> push { transaction.from.toElement() }.gas2()
    OpCode.CALLER -> push { caller.toElement() }.gas2()
    OpCode.CALLVALUE -> push { callValue.toElement() }.gas2()
    OpCode.CALLDATALOAD -> poppush { (i) -> callData.read(i.int, 32).toElement() }.gas3()
    OpCode.CALLDATASIZE -> push { callData.size.toElement() }.gas2()

    OpCode.CALLDATACOPY -> pop { (memOffset, offset, length) ->
        memory.write(memOffset.int, callData.read(offset.int, length.int))
    }.memorySize { (memOffset, _, length) -> length.int + memOffset.int }
        .gas3().extraGas { (_, _, length) -> memoryGasCost() + (length.int.wordSize * memoryCopyGas) }


    OpCode.CODESIZE -> push { contract.code.size.toElement() }.gas2()

    OpCode.CODECOPY -> pop { (memOffset, offset, length) ->
        memory.write(memOffset.int, contract.code.read(offset.int, length.int))
    }.memorySize { (memOffset, _, length) -> length.int + memOffset.int }
        .gas3().extraGas { (_, _, length) -> memoryGasCost() + (length.int.wordSize * memoryCopyGas) }

    OpCode.GASPRICE -> push { transaction.gasPrice.toElement() }.gas2()

    OpCode.EXTCODESIZE -> {
        poppush { (addr) -> db.withAccountOrNull(addr.toAddress()) { it?.getCode()?.size ?: 0 }.toElement() }
        gas(
            when {
                vmConfig.eip2929 -> 100
                vmConfig.eip150 -> 700
                else -> 20
            }
        )
        if (vmConfig.eip2929) extraGas { (addr) -> computeAccessAccountGas(addr.toAddress()) }
    }

    OpCode.EXTCODECOPY -> execute { TODO("EXTCODECOPY") }.gas(if (vmConfig.eip150) 700 else 20)
    OpCode.RETURNDATASIZE -> if (vmConfig.eip211) push {
        nextFrame?.result?.data?.size?.toElement() ?: EVMStackElement.ZERO
    }.gas2()

    OpCode.RETURNDATACOPY -> if (vmConfig.eip211) pop { (memOffset, offset, length) ->
        val returnValue = nextFrame?.result?.data?.read(offset.int, length.int)
        memory.write(memOffset.int, returnValue ?: ByteArray(length.int))
    }.memorySize { (memOffset, _, length) -> length.int + memOffset.int }
        .gas3().extraGas { (_, _, length) -> memoryGasCost() + (length.int.wordSize * memoryCopyGas) }

    OpCode.EXTCODEHASH -> if (vmConfig.eip1052) poppush { (address) ->
        db.findAccount(address.toAddress())?.codeHash?.bytes?.toElement() ?: EVMStackElement.ZERO
    }.gas(if (vmConfig.eip1884) 700 else 400)

    OpCode.BLOCKHASH -> pop { TODO("BLOCKHASH") }.gas(20)
    OpCode.COINBASE -> push { block.coinbase.toElement() }.gas2()
    OpCode.TIMESTAMP -> push { block.time.toElement() }.gas2()
    OpCode.NUMBER -> push { block.number.toElement() }.gas2()
    OpCode.DIFFICULTY -> push { block.difficulty.toElement() }.gas2()
    OpCode.PREVRANDAO -> if (vmConfig.eip4399) push { block.random.toElement() }.gas2()
    OpCode.GASLIMIT -> push { block.gasLimit.toElement() }.gas2()
    OpCode.CHAINID -> if (vmConfig.eip1344) push { chainId.toElement() }.gas2()
    OpCode.SELFBALANCE -> if (vmConfig.eip1884) push {
        db.findAccount(contract.address)?.balance?.toElement() ?: EVMStackElement.ZERO
    }.gas5()

    OpCode.BASEFEE -> if (vmConfig.eip3198) push { block.baseFee.toElement() }.gas2()
    OpCode.BLOBHASH -> poppush { TODO("BLOBHASH") }.gas3()
    OpCode.BLOBBASEFEE -> push { TODO("BLOBBASEFEE") }.gas2()
    OpCode.POP -> pop { (n) -> }.gas2()

    OpCode.MLOAD -> poppush { (offset) -> memory.read(offset.int, 32).toElement() }
        .memorySize { (offset) -> offset.int + 32 }
        .gas3().extraGas { memoryGasCost() }

    OpCode.MSTORE -> pop { (offset, value) -> memory.write(offset.int, value.bytes.sliceArrayLast(32)) }
        .memorySize { (offset) -> offset.int + 32 }
        .gas3().extraGas { memoryGasCost() }

    OpCode.MSTORE8 -> pop { (a, b) -> TODO("MSTORE8") }
    OpCode.SLOAD -> {
        poppush { (key) ->
            db.withAccount(contract.address) { it.storage.get(key.bytes) }?.toElement() ?: EVMStackElement.ZERO
        }
        gas(
            when {
                vmConfig.eip2929 -> 0
                vmConfig.eip2200 -> 800
                vmConfig.eip1884 -> 800
                vmConfig.eip150 -> 200
                else -> 50
            }
        )
        if (vmConfig.eip2929) extraGas { (key) -> computeAccessSlotGas(contract, key.bytes, alreadyExistGas = 100) }
    }

    OpCode.SSTORE -> pop { (key, value) ->
        db.withAccountOrCreate(contract.address) { it.storage.set(key.bytes, value.bytes.takeIfNotAllZero()) }
    }.extraGas { (key, value) ->
        val ssStoreGas = when {
            vmConfig.eip3529 -> SStoreGas.eip3529(computeAccessSlotGas(contract, key.bytes))
            vmConfig.eip2929 -> SStoreGas.eip2929(computeAccessSlotGas(contract, key.bytes))
            vmConfig.eip2200 -> SStoreGas.eip2200()
            vmConfig.eip1716 -> null
            vmConfig.eip1283 -> SStoreGas.eip1283()
            else -> null
        }
        val new = value.bytes.takeIfNotAllZero()
        val dirty = db.withAccount(contract.address) { it.storage.get(key.bytes) }
        if (ssStoreGas != null) {
            if (ssStoreGas.reentrancy != null && remainGas <= ssStoreGas.reentrancy) {
                result = EVMReturn.outOfGas()
                return@extraGas 0
            }
            if (dirty.contentEquals(new)) return@extraGas ssStoreGas.doNothing
            val origin = db.withAccount(contract.address) { it.storage.getCommittedState(key.bytes) }
            if (dirty.contentEquals(origin)) {
                if (origin == null) return@extraGas ssStoreGas.createSlot
                if (new == null) remainGas += ssStoreGas.deleteSlot
                return@extraGas ssStoreGas.updateSlot
            }
            if (origin != null) {
                if (dirty == null) remainGas -= ssStoreGas.recreateSlot
                else if (new == null) remainGas += ssStoreGas.deleteSlot
            }
            if (origin.contentEquals(new)) {
                if (origin == null) remainGas += ssStoreGas.resetDeleteSlot
                else remainGas -= ssStoreGas.resetOriginSlot
            }
            return@extraGas ssStoreGas.updateDirtySlot
        }
        when {
            dirty == null && new != null -> 20000
            dirty != null && new == null -> 5000
            else -> 5000
        }
    }

    OpCode.JUMP -> pop { (pos) -> pc = pos.int - 1 }.gas8()
    OpCode.JUMPI -> pop { (pos, condition) ->
        if (condition == EVMStackElement.ZERO) return@pop
        pc = pos.int - 1 // pc will be increased by the interpreter loop
    }.gas(10)

    OpCode.PC -> push { pc.toElement() }
    OpCode.MSIZE -> pop { TODO("MSIZE") }.gas2()
    OpCode.GAS -> push { remainGas.toElement() }.gas2()
    OpCode.JUMPDEST -> pop { }.gas(1)
    OpCode.TLOAD -> pop { TODO("TLOAD") }
    OpCode.TSTORE -> pop { (a, b) -> TODO("TSTORE") }
    OpCode.MCOPY -> pop { (a, b, c) -> TODO("MCOPY") }.gas3()

    OpCode.PUSH0 -> if (vmConfig.eip3855) push { EVMStackElement.ZERO }.gas2()
    OpCode.PUSH1 -> push { byteArrayOf(contract.code[++pc]).toElement() }.gas3()
    OpCode.PUSH2, OpCode.PUSH3, OpCode.PUSH4, OpCode.PUSH5, OpCode.PUSH6, OpCode.PUSH7, OpCode.PUSH8, OpCode.PUSH9,
    OpCode.PUSH10, OpCode.PUSH11, OpCode.PUSH12, OpCode.PUSH13, OpCode.PUSH14, OpCode.PUSH15, OpCode.PUSH16, OpCode.PUSH17, OpCode.PUSH18, OpCode.PUSH19,
    OpCode.PUSH20, OpCode.PUSH21, OpCode.PUSH22, OpCode.PUSH23, OpCode.PUSH24, OpCode.PUSH25, OpCode.PUSH26, OpCode.PUSH27, OpCode.PUSH28, OpCode.PUSH29,
    OpCode.PUSH30, OpCode.PUSH31, OpCode.PUSH32 -> push {
        val size = operation.opCode.name.drop(4).toInt()
        contract.code.read(pc + 1, size).toElement().apply { pc += size }
    }.gas3()

    OpCode.DUP1, OpCode.DUP2, OpCode.DUP3, OpCode.DUP4, OpCode.DUP5, OpCode.DUP6, OpCode.DUP7, OpCode.DUP8, OpCode.DUP9,
    OpCode.DUP10, OpCode.DUP11, OpCode.DUP12, OpCode.DUP13, OpCode.DUP14, OpCode.DUP15, OpCode.DUP16 -> execute {
        val size = operation.opCode.name.drop(3).toInt()
        stack.push(stack.back(size - 1).copy())
    }.gas3()

    OpCode.SWAP1, OpCode.SWAP2, OpCode.SWAP3, OpCode.SWAP4, OpCode.SWAP5, OpCode.SWAP6, OpCode.SWAP7, OpCode.SWAP8, OpCode.SWAP9,
    OpCode.SWAP10, OpCode.SWAP11, OpCode.SWAP12, OpCode.SWAP13, OpCode.SWAP14, OpCode.SWAP15, OpCode.SWAP16 -> execute {
        val size = operation.opCode.name.drop(4).toInt()
        val a = stack.back(0)
        val b = stack.back(size)
        stack[stack.lastIndex] = b
        stack[stack.lastIndex - size] = a
    }.gas3()

    OpCode.LOG0, OpCode.LOG1, OpCode.LOG2, OpCode.LOG3, OpCode.LOG4 -> execute {
        val offset = stack.pop().int
        val size = stack.pop().int
        val topics = (1..operation.opCode.name.drop(3).toInt()).map { _ -> stack.pop().bytes }
        val data = memory.read(offset, size)
        transaction.logs += createLog(topics, data)
    }.memorySize { (offset, size) -> offset.int + size.int }.extraGas { (_, size) ->
        var gas = memoryGasCost()
        gas += 375
        gas += operation.opCode.name.drop(3).toInt() * 375
        gas + size.int * 8
    }

    OpCode.CALL -> poppush { (gas, addr, value, argsOffset, argsLength, retOffset, retLength) ->
        if ((db.findAccount(contract.address)?.balance ?: BigInteger.ZERO) < value.big) {
            result = EVMReturn.insufficientBalance()
            return@poppush EVMStackElement.ZERO
        }

        nextFrameGas += if (value.big != BigInteger.ZERO) 2300 else 0
        db.applyAccountOrThrow(contract.address) { it.balance -= value.big }
        val account = db.applyAccountOrCreate(addr.toAddress()) { it.balance += value.big }
        if (account.codeHash == null) {
            this.remainGas = nextFrameGas
            return@poppush EVMStackElement.ONE
        }

        val result = nextFrame {
            val calldata = memory.read(argsOffset.int, argsLength.int)
            val nextContract = db.withAccountOrThrow(addr.toAddress(), EVMContract::of)
            EVMFrame(contract.address, value.big, calldata, nextContract, nextFrameGas)
        }

        memory.write(retOffset.int, retLength.int, result.data)
        if (result.err == null) EVMStackElement.ONE else EVMStackElement.ZERO
    }.memorySize { (_, _, _, argsOffset, argsLength, retOffset, retLength) ->
        max(retLength.int + retOffset.int, argsLength.int + argsOffset.int)
    }.gas(
        when {
            vmConfig.eip2929 -> 100
            vmConfig.eip150 -> 700
            else -> 40
        }
    ).extraGas { (gas, addr, value) ->
        if (vmConfig.eip2929) computeAccessContractCallGas(addr.toAddress()) {
            transferValueGas(addr.toAddress(), value.big) + callGas(gas.int)
        } else transferValueGas(addr.toAddress(), value.big) + callGas(gas.int)
    }

    OpCode.CALLCODE -> execute { TODO() }.gas(if (vmConfig.eip150) 700 else 40)
    OpCode.RETURN -> pop { (offset, size) -> result = EVMReturn.success(memory.read(offset.int, size.int)) }

    OpCode.DELEGATECALL -> if (vmConfig.eip7) poppush { (gas, addr, argsOffset, argsLength, retOffset, retLength) ->
        val result = nextFrame {
            val calldata = memory.read(argsOffset.int, argsLength.int)
            val contract = db.withAccountOrThrow(addr.toAddress()) {
                EVMContract(contract.address, requireNotNull(it.getCode()), requireNotNull(it.codeHash))
            }
            EVMFrame(caller, callValue, calldata, contract, nextFrameGas)
        }
        memory.write(retOffset.int, retLength.int, result.data)
        if (result.err == null) EVMStackElement.ONE else EVMStackElement.ZERO
    }.memorySize { (_, _, argsOffset, argsLength, retOffset, retLength) ->
        max(retLength.int + retOffset.int, argsLength.int + argsOffset.int)
    }.gas(
        when {
            vmConfig.eip2929 -> 100
            vmConfig.eip150 -> 700
            else -> 20
        }
    ).extraGas { (gas, addr) ->
        if (vmConfig.eip2929) computeAccessContractCallGas(addr.toAddress()) { callGas(gas.int) } else callGas(gas.int)
    }

    OpCode.CREATE -> poppush { (value, offset, size) ->
        val newContract = db.withAccountOrThrow(contract.address) {
            EVMContract(Address.new(it.address, it.nonce), memory.read(offset.int, size.int))
        }
        deployContract(contract.address, newContract, value.big)
    }.gas(32000).extraGas { (_, _, size) ->
        if (vmConfig.eip3860 && maxInitCodeSize < size.int) 0.also { result = EVMReturn.initMaxCodeSizeExceeded() }
        else memoryGasCost() + when (vmConfig.eip3860) {
            true -> size.int.wordSize * 2
            false -> 0
        }
    }.memorySize { (_, offset, size) -> offset.int + size.int }

    OpCode.CREATE2 -> if (vmConfig.eip1014) poppush { (value, offset, size, salt) ->
        val code = memory.read(offset.int, size.int)
        val newContract = EVMContract(Address.new(contract.address, salt.bytes, code.keccak256()), code)
        deployContract(contract.address, newContract, value.big)
    }.gas(32000).extraGas { (_, _, size) ->
        if (vmConfig.eip3860 && maxInitCodeSize < size.int) 0.also { result = EVMReturn.initMaxCodeSizeExceeded() }
        else memoryGasCost() + when (vmConfig.eip3860) {
            true -> size.int.wordSize * (2 + keccak256Gas)
            false -> size.int.wordSize * keccak256Gas
        }
    }.memorySize { (_, offset, size) -> offset.int + size.int }

    OpCode.STATICCALL -> if (vmConfig.eip214) poppush { (gas, addr, argsOffset, argsLength, retOffset, retLength) ->
        // We do an AddBalance of zero here, just in order to trigger a touch.
        // This doesn't matter on Mainnet, where all empties are gone at the time of Byzantium,
        // but is the correct thing to do and matters on other networks, in tests, and potential
        // future scenarios
        db.withAccountOrThrow(addr.toAddress()) { it.balance += BigInteger.ZERO }

        val result = nextFrame {
            val calldata = memory.read(argsOffset.int, argsLength.int)
            val contract = db.withAccountOrThrow(addr.toAddress(), EVMContract::of)
            EVMFrame(this.contract.address, BigInteger.ZERO, calldata, contract, nextFrameGas)
        }
        memory.write(retOffset.int, retLength.int, result.data)
        if (result.err == null) EVMStackElement.ONE else EVMStackElement.ZERO
    }.memorySize { (_, _, argsOffset, argsLength, retOffset, retLength) ->
        max(retLength.int + retOffset.int, argsLength.int + argsOffset.int)
    }.gas(
        when {
            vmConfig.eip2929 -> 100
            else -> 700
        }
    ).extraGas { (gas, addr) ->
        if (vmConfig.eip2929) computeAccessContractCallGas(addr.toAddress()) { callGas(gas.int) } else callGas(gas.int)
    }

    OpCode.REVERT -> if (vmConfig.eip140) pop { (offset, length) ->
        result = EVMReturn.executionReverted(memory.read(offset.int, length.int))
    }

    OpCode.INVALID -> pop { }
    OpCode.SELFDESTRUCT -> pop { (_) -> TODO("SELFDESTRUCT") }
// @formatter:off
}; return this }
// @formatter:on

private suspend fun EVMFrame.deployContract(
    caller: Address,
    newContract: EVMContract,
    value: BigInteger
): EVMStackElement {
    result = db.withAccount(caller) {
        if (it.balance < BigInteger.ZERO) EVMReturn.insufficientBalance()
        else if (it.nonce == ULong.MAX_VALUE) EVMReturn.nonceOverflow()
        else null
    } ?: db.withAccount(newContract.address) {
        if (it.nonce > 0u || it.codeHash != null) EVMReturn.contractAddressCollision()
        else null
    }
    if (result != null) return EVMStackElement.ZERO

    db.withAccountOrThrow(caller) { it.balance -= value }
    db.createAccount(newContract.address) {
        it.balance += value
        if (vmConfig.eip158) it.nonce = 1u
    }

    val deploymentResult = nextFrame {
        val gas = if (vmConfig.eip150) remainGas - (remainGas / 64) else remainGas
        EVMFrame(caller, value, byteArrayOf(), newContract, gas)
    }

    result = when {
        deploymentResult.err != null -> deploymentResult
        deploymentResult.data!!.size > maxCodeSize -> EVMReturn.maxCodeSizeExceeded()
        deploymentResult.data.getOrNull(0) == 0xEF.toByte() && vmConfig.eip3541 -> EVMReturn.invalidCode()
        else -> null
    }
    if (result != null) {
        // todo execution reverted
        // todo rollback state
        return EVMStackElement.ZERO
    }

    this.remainGas -= deploymentResult.data!!.size * 200
    if (this.remainGas >= 0) {
        db.applyAccount(newContract.address) { it.setCode(deploymentResult.data) }
        return newContract.address.toElement()
    }
    result = EVMReturn.codeStoreOutOfGas()
    if (vmConfig.eip2) {
        // todo execution reverted
        // todo rollback state
    }
    return EVMStackElement.ZERO
}

private const val memoryCopyGas = 3
private const val keccak256Gas = 6
private const val maxCodeSize = 24576
private const val maxInitCodeSize = maxCodeSize * 2

private data class SStoreGas(
    val reentrancy: Int? = null,
    val doNothing: Int,
    val createSlot: Int,
    val recreateSlot: Int,
    val deleteSlot: Int,
    val updateSlot: Int,
    val updateDirtySlot: Int,
    val resetDeleteSlot: Int,
    val resetOriginSlot: Int,
) {
    companion object {
        fun eip3529(accessSlot: Int) = SStoreGas(
            reentrancy = 2300,
            doNothing = 100 + accessSlot,
            createSlot = 20000 + accessSlot,
            recreateSlot = 4800,
            deleteSlot = 4800,
            updateSlot = 2900 + accessSlot,
            updateDirtySlot = 100 + accessSlot,
            resetDeleteSlot = 19900,
            resetOriginSlot = 2800
        )

        fun eip2929(accessSlot: Int) = SStoreGas(
            reentrancy = 2300,
            doNothing = 100 + accessSlot,
            createSlot = 20000 + accessSlot,
            recreateSlot = 15000,
            deleteSlot = 15000,
            updateSlot = 2900 + accessSlot,
            updateDirtySlot = 100 + accessSlot,
            resetDeleteSlot = 19900,
            resetOriginSlot = 2800
        )

        fun eip2200() = SStoreGas(
            reentrancy = 2300,
            doNothing = 800,
            createSlot = 20000,
            recreateSlot = 15000,
            deleteSlot = 15000,
            updateSlot = 5000,
            updateDirtySlot = 800,
            resetDeleteSlot = 19200,
            resetOriginSlot = 4200
        )

        fun eip1283() = SStoreGas(
            doNothing = 200,
            createSlot = 20000,
            recreateSlot = 15000,
            deleteSlot = 15000,
            updateSlot = 5000,
            updateDirtySlot = 200,
            resetDeleteSlot = 19800,
            resetOriginSlot = 4800
        )
    }
}

private fun ByteArray.read(offset: Int, size: Int): ByteArray = copyOfRange(offset, offset + size)
private fun ByteArray.write(offset: Int, bytes: ByteArray): ByteArray =
    bytes.copyInto(destination = this, destinationOffset = offset)

private fun ByteArray.write(offset: Int, length: Int, bytes: ByteArray?): ByteArray =
    (bytes ?: ByteArray(length)).copyInto(destination = this, destinationOffset = offset)

private fun ByteArray.sliceArrayLast(n: Int): ByteArray = when (size.compareTo(n)) {
    -1 -> ByteBuffer.allocate(n).position(n - size).put(this).array()
    0 -> this
    1 -> copyOfRange(size - n, size)
    else -> error("")
}

private fun ByteArray.toElement() = EVMStackElement(_bytes = this)
private fun BigInteger.toElement() = EVMStackElement(_big = this)
private fun Int.toElement() = EVMStackElement(_int = this)
private fun ULong.toElement() = EVMStackElement(_big = toLong().toBigInteger())
private fun Address.toElement() = EVMStackElement(bytes)

private fun EVMStackElement.toAddress() = Address(bytes.sliceArrayLast(20))

private inline fun runIf(condition: Boolean, crossinline execute: () -> EVMStackElement): EVMStackElement {
    if (condition) return execute()
    return EVMStackElement.ZERO
}

private val big256 = 256.toBigInteger()

private fun ByteArray.takeIfNotAllZero() = takeIf { it.any { b -> b != 0.toByte() } }
