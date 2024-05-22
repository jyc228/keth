package io.github.jyc228.ethereum.vm

import io.github.jyc228.ethereum.state.account.Address
import java.math.BigInteger
import java.nio.ByteBuffer
import org.bouncycastle.jcajce.provider.digest.Keccak

// @formatter:off
fun OperationBuilder.withOpCode(opCode: OpCode): OperationBuilder { when (opCode) { // https://ethervm.io/
// @formatter:on
    OpCode.STOP -> pop0 { result = EVMReturn.success(byteArrayOf()) }
    OpCode.ADD -> pop2push { t1, t0 -> t0 + t1 }.gas3()
    OpCode.MUL -> pop2push { t1, t0 -> t0 * t1 }.gas5()
    OpCode.SUB -> pop2push { t1, t0 -> t0 - t1 }.gas3()

    OpCode.DIV -> pop2push { t1, t0 -> t0 / t1 }.gas5()
    OpCode.SDIV -> pop2push { t1, t0 -> t0.signed() / t1.signed() }.gas5()

    OpCode.MOD -> pop2push { t1, t0 -> t0 % t1 }.gas5()
    OpCode.SMOD -> pop2push { t1, t0 -> t0.signed() % t1.signed() }.gas5()

    OpCode.ADDMOD -> pop3push { t2, t1, t0 -> (t0 + t1) % t2 }.gas8()
    OpCode.MULMOD -> pop3push { t2, t1, t0 -> (t0 * t1) % t2 }.gas8()
    OpCode.EXP -> pop2push { exp, base -> base pow exp }.additionalGas {
        ((stack.back(1).big.bitLength() + 7) / 8 * if (vmConfig.eip158) 50 else 10) + 10
    }

    OpCode.SIGNEXTEND -> pop2push { x, b ->
        if (b.big >= 32.toBigInteger() || b.big < 0.toBigInteger()) return@pop2push x
        val result = when (x.bytes.size > b.int && x.bytes[x.bytes.lastIndex - b.int] < 0) {
            true -> ByteArray(32).apply { fill(0xFF.toByte(), 0, lastIndex - b.int) }
            false -> ByteArray((b.int + 1) * 8)
        }
        result.write(result.lastIndex - b.int, x.bytes.sliceArrayLast(b.int + 1))
        result.toElement()
    }.gas5()

    OpCode.LT -> pop2push { b, a -> if (b > a) EVMStackElement.ONE else EVMStackElement.ZERO }.gas3()
    OpCode.GT -> pop2push { b, a -> if (b < a) EVMStackElement.ONE else EVMStackElement.ZERO }.gas3()
    OpCode.SLT -> pop2push { b, a -> if (b.signed() > a.signed()) EVMStackElement.ONE else EVMStackElement.ZERO }.gas3()
    OpCode.SGT -> pop2push { b, a -> if (b.signed() < a.signed()) EVMStackElement.ONE else EVMStackElement.ZERO }.gas3()
    OpCode.EQ -> pop2push { b, a -> if (b == a) EVMStackElement.ONE else EVMStackElement.ZERO }.gas3()
    OpCode.ISZERO -> pop1push { a -> if (a == EVMStackElement.ZERO) EVMStackElement.ONE else EVMStackElement.ZERO }.gas3()
    OpCode.AND -> pop2push { b, a -> b and a }.gas3()
    OpCode.OR -> pop2push { b, a -> b or a }.gas3()
    OpCode.XOR -> pop2push { b, a -> b xor a }.gas3()
    OpCode.NOT -> pop1push { a -> a.not() }.gas3()
    OpCode.BYTE -> pop2push { t1, t0 ->
        val index = t0.big
        if (32.toBigInteger() <= index) return@pop2push EVMStackElement.ZERO
        val pos = index.toInt() - 32 + t1.bytes.size
        if (pos < 0 || 32 <= pos) return@pop2push EVMStackElement.ZERO
        byteArrayOf(t1.bytes[pos]).toElement()
    }.gas3()

    OpCode.SHL -> if (vmConfig.eip145) pop2push { value, shift -> runIf(shift.big <= big256) { (value.big shl shift.int).toElement() } }.gas3()
    OpCode.SHR -> if (vmConfig.eip145) pop2push { value, shift -> runIf(shift.big <= big256) { (value.big shr shift.int).toElement() } }.gas3()
    OpCode.SAR -> if (vmConfig.eip145) pop2push { value, shift ->
        when (shift.big <= big256) {
            true -> when (value.bytes[0] < 0) {
                true -> (value.big shr shift.int or EVMStackElement.MAX.big.shl(256 - shift.int)).toElement()
                false -> (value.big shr shift.int).toElement()
            }

            false -> runIf(value.bytes[0] < 0) { EVMStackElement.MAX }
        }
    }.gas3()

    OpCode.KECCAK256 -> pop2push { size, offset ->
        val input = memory.read(offset.int, size.int)
        Keccak.Digest256().digest(input).toElement()
    }.gas(30).memorySize {
        val offset = stack.back(0)
        val size = stack.back(1)
        offset.int + size.int
    }.additionalGas {
        val gas = memoryGasCost(memorySize)
        val size = stack.back(1).int
        val wordSize = (size + 31) / 32
        gas + (wordSize * 6)
    }

    OpCode.ADDRESS -> push { contract.address.toElement() }.gas2()

    OpCode.BALANCE -> pop1push { address ->
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
    OpCode.CALLDATALOAD -> pop1push { t0 -> callData.read(t0.int, 32).toElement() }.gas3()
    OpCode.CALLDATASIZE -> push { callData.size.toElement() }.gas2()

    OpCode.CALLDATACOPY -> pop3 { length, dataOffset, memOffset ->
        val callData = callData.read(dataOffset.int, length.int)
        memory.write(memOffset.int, callData)
    }.gas3()
        .additionalGas { memoryCopyGas(2, memorySize) }
        .memorySize { stack.back(2).int + stack.back(0).int }

    OpCode.CODESIZE -> push { contract.code.size.toElement() }.gas2()

    OpCode.CODECOPY -> pop3 { length, dataOffset, memOffset ->
        val code = contract.code.read(dataOffset.int, length.int)
        memory.write(memOffset.int, code)
    }.gas3()
        .additionalGas { memoryCopyGas(2, memorySize) }
        .memorySize { stack.back(0).int + stack.back(2).int }

    OpCode.GASPRICE -> push { transaction.gasPrice.toElement() }.gas2()

    OpCode.EXTCODESIZE -> pop1push { address ->
        db.withAccountOrNull(address.toAddress()) { it?.getCode()?.size ?: 0 }.toElement()
    }.gas(if (vmConfig.eip150) 700 else 20)

    OpCode.EXTCODECOPY -> execute { TODO() }.gas(if (vmConfig.eip150) 700 else 20)
    OpCode.RETURNDATASIZE -> if (vmConfig.eip211) push {
        nextFrame?.result?.data?.size?.toElement() ?: EVMStackElement.ZERO
    }.gas2()

    OpCode.RETURNDATACOPY -> if (vmConfig.eip211) pop3 { length, dataOffset, memOffset ->
        val returnValue = nextFrame?.result?.data?.read(dataOffset.int, length.int)
        memory.write(memOffset.int, returnValue ?: ByteArray(length.int))
    }.additionalGas { memoryCopyGas(2, memorySize) }.gas3()

    OpCode.EXTCODEHASH -> if (vmConfig.eip1052) pop1push { address ->
        db.findAccount(address.toAddress())?.codeHash?.bytes?.toElement() ?: EVMStackElement.ZERO
    }.gas(if (vmConfig.eip1884) 700 else 400)

    OpCode.BLOCKHASH -> pop1 { }.gas(20)
    OpCode.COINBASE -> push { block.coinbase.toElement() }.gas2()
    OpCode.TIMESTAMP -> push { block.time.toElement() }.gas2()
    OpCode.NUMBER -> push { block.number.toElement() }.gas2()
    OpCode.DIFFICULTY -> push { block.difficulty.toElement() }.gas2()

    OpCode.RANDOM,
    OpCode.PREVRANDAO -> push { block.random.toElement() }.gas2()

    OpCode.GASLIMIT -> push { block.gasLimit.toElement() }.gas2()
    OpCode.CHAINID -> if (vmConfig.eip1344) push { chainId.toElement() }.gas2()
    OpCode.SELFBALANCE -> if (vmConfig.eip1884) push {
        db.findAccount(contract.address)?.balance?.toElement() ?: EVMStackElement.ZERO
    }.gas5()

    OpCode.BASEFEE -> push { block.baseFee.toElement() }.gas2()
    OpCode.BLOBHASH -> pop1push { TODO() }.gas3()
    OpCode.BLOBBASEFEE -> push { TODO() }.gas2()
    OpCode.POP -> pop1 { _ -> }.gas2()

    OpCode.MLOAD -> pop1push { offset -> memory.read(offset.int, 32).toElement() }
        .memorySize { stack.back(0).int + 32 }
        .gas3()
        .additionalGas { memoryGasCost(stack.back(0).int + 32) }

    OpCode.MSTORE -> gas3().pop2 { value, offset ->
        memory.write(offset.int, value.bytes.sliceArrayLast(32))
    }.memorySize {
        stack.back(0).int + 32
    }.additionalGas {
        memoryGasCost(stack.back(0).int + 32)
    }

    OpCode.MSTORE8 -> pop2 { a, b -> TODO() }
    OpCode.SLOAD -> pop1push { key ->
        db.withAccountOrNull(contract.address) { it?.storage?.get(key.bytes) }?.toElement()
            ?: EVMStackElement.ZERO
    }.gas(
        when {
            vmConfig.eip1884 -> 800
            vmConfig.eip150 -> 200
            else -> 50
        }
    )

    OpCode.SSTORE -> pop2 { value, location ->
        db.withAccountOrCreate(contract.address) { it.storage.set(location.bytes, value.bytes) }
    }.gas(2900)

    OpCode.JUMP -> pop1 { pos -> pc = pos.int - 1 }.gas8()
    OpCode.JUMPI -> pop2 { condition, pos ->
        if (condition == EVMStackElement.ZERO) return@pop2
        pc = pos.int - 1 // pc will be increased by the interpreter loop
    }.gas(10)

    OpCode.PC -> push { pc.toElement() }
    OpCode.MSIZE -> pop0 { }.gas2()
    OpCode.GAS -> push { gas.toElement() }.gas2()
    OpCode.JUMPDEST -> pop0 { }.gas(1)
    OpCode.TLOAD -> pop1 { }
    OpCode.TSTORE -> pop2 { a, b -> }
    OpCode.MCOPY -> pop3 { a, b, c -> }.gas3()

    OpCode.PUSH0 -> push { EVMStackElement.ZERO }.gas2()
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
    }.memorySize {
        val offset = stack.back(0).int
        val size = stack.back(1).int
        offset + size
    }.additionalGas {
        val topicCount = operation.opCode.name.drop(3).toInt()
        val size = stack.back(1).int
        var gas = memoryGasCost(memorySize)
        gas += 375
        gas += topicCount * 375
        gas += size * 8
        gas
    }

    OpCode.CREATE -> pop3 { top2, top1, top0 -> TODO() }
    OpCode.CALL -> pop7push { retLength, retOffset, argsLength, argsOffset, value, addr, gas ->
        if ((db.findAccount(contract.address)?.balance ?: BigInteger.ZERO) < value.big) {
            result = EVMReturn.insufficientBalance()
            return@pop7push EVMStackElement.ZERO
        }

        nextFrameGas += if (value.big != BigInteger.ZERO) 2300 else 0
        db.applyAccountOrThrow(contract.address) { it.balance -= value.big }
        val account = db.applyAccountOrCreate(addr.toAddress()) { it.balance += value.big }
        if (account.codeHash == null) {
            this.gas = nextFrameGas
            return@pop7push EVMStackElement.ONE
        }

        val result = nextFrame {
            val calldata = memory.read(argsOffset.int, argsLength.int)
            val nextContract = db.withAccountOrThrow(addr.toAddress(), EVMContract::of)
            EVMFrame(contract.address, value.big, calldata, nextContract, nextFrameGas)
        }

        memory.write(retOffset.int, retLength.int, result.data)
        if (result.err == null) EVMStackElement.ONE else EVMStackElement.ZERO
    }.memorySize {
        val retSize = stack.back(6).int + stack.back(5).int
        val argSize = stack.back(4).int + stack.back(3).int
        if (retSize > argSize) retSize else argSize
    }.gas(if (vmConfig.eip150) 700 else 40).additionalGas {
        var gas = 0
        if (vmConfig.eip158) {
            if (stack.back(2).big > BigInteger.ZERO && db.findAccount(stack.back(1).toAddress()) == null) {
                gas += 25000
            }
        } else if (db.findAccount(stack.back(1).toAddress()) == null) {
            gas += 25000
        }
        if (stack.back(2).big > BigInteger.ZERO) {
            gas += 9000
        }
        gas + callGas(memorySize)
    }

    OpCode.CALLCODE -> execute { TODO() }.gas(if (vmConfig.eip150) 700 else 40)
    OpCode.RETURN -> pop2 { size, offset -> result = EVMReturn.success(memory.read(offset.int, size.int)) }

    OpCode.DELEGATECALL -> if (vmConfig.eip7) pop6push { retLength, retOffset, argsLength, argsOffset, addr, gas ->
        val result = nextFrame {
            val calldata = memory.read(argsOffset.int, argsLength.int)
            val contract = db.withAccountOrThrow(addr.toAddress()) {
                EVMContract(contract.address, requireNotNull(it.getCode()), requireNotNull(it.codeHash))
            }
            EVMFrame(caller, callValue, calldata, contract, nextFrameGas)
        }
        memory.write(retOffset.int, retLength.int, result.data)
        if (result.err == null) EVMStackElement.ONE else EVMStackElement.ZERO
    }.memorySize {
        val retSize = stack.back(5).int + stack.back(4).int
        val argSize = stack.back(3).int + stack.back(2).int
        if (retSize > argSize) retSize else argSize
    }.gas(if (vmConfig.eip150) 700 else 20).additionalGas { callGas(memorySize) }

    OpCode.CREATE2 -> if (vmConfig.eip1014) execute { TODO("CREATE2") }
    OpCode.STATICCALL -> if (vmConfig.eip214) pop6push { retLength, retOffset, argsLength, argsOffset, addr, gas ->
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
    }.memorySize {
        val retSize = stack.back(5).int + stack.back(4).int
        val argSize = stack.back(3).int + stack.back(2).int
        if (retSize > argSize) retSize else argSize
    }.gas(100).additionalGas { callGas(memorySize) }

    OpCode.REVERT -> if (vmConfig.eip140) pop2 { length, offset ->
        result = EVMReturn.executionReverted(memory.read(offset.int, length.int))
    }

    OpCode.INVALID -> pop0 { }
    OpCode.SELFDESTRUCT -> pop1 { TODO() }
// @formatter:off
}; return this }
// @formatter:on

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