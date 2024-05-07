package io.github.jyc228.ethereum.vm

import java.math.BigInteger
import org.bouncycastle.jcajce.provider.digest.Keccak

fun newOperation(opCode: OpCode) = OperationBuilder.build(opCode) {
    // https://ethervm.io/
    when (opCode) {
        OpCode.STOP -> pop0 { result = EVMReturn.success(byteArrayOf()) }
        OpCode.ADD -> pop2push { t1, t0 -> t0 + t1 }.withGas3()
        OpCode.MUL -> pop2push { t1, t0 -> t0 * t1 }.withGas5()
        OpCode.SUB -> pop2push { t1, t0 -> t0 - t1 }.withGas3()

        OpCode.DIV -> pop2push { t1, t0 -> t0 / t1 }.withGas5()
        OpCode.SDIV -> pop2push { t1, t0 ->
            val result = t0.signed() / t1.signed()
            when (result.bytes.size <= 32) {
                true -> result
                false -> result.bytes.read(result.bytes.size - 32, 32).toElement()
            }
        }.withGas5()

        OpCode.MOD -> pop2push { t1, t0 -> t0 % t1 }.withGas5()
        OpCode.SMOD -> pop2push { t1, t0 ->
            val xn = if (t0.bytes.size < 32) BigInteger(1, t0.bytes) else BigInteger(t0.bytes)
            val yn = if (t1.bytes.size < 32) BigInteger(1, t1.bytes) else BigInteger(t1.bytes)
            if (yn == BigInteger.ZERO) EVMStackElement.ZERO
            else {
                var result = xn.abs() % yn.abs()
                if (xn.signum() < 0) {
                    result = -result
                }
                var resultBytes = result.toByteArray()
                if (resultBytes.size > 32) {
                    resultBytes = resultBytes.copyOfRange(resultBytes.size - 32, resultBytes.size)
                }
                ByteArray(32) { idx ->
                    if (idx < 32 - resultBytes.size) {
                        if (result.signum() < 0) 0xFF.toByte() else 0x00
                    } else {
                        resultBytes[idx - (32 - resultBytes.size)]
                    }
                }.toElement()
            }
        }.withGas5()

        OpCode.ADDMOD -> pop3push { t2, t1, t0 -> (t0 + t1) % t2 }.withGas8()
        OpCode.MULMOD -> pop3push { t2, t1, t0 -> (t0 * t1) % t2 }.withGas8()
        OpCode.EXP -> pop2push { exp, base -> base pow exp }.withDynamicGas {
            ((stack.back(1).big.bitLength() + 7) / 8 * 50) + 10
        }

        OpCode.SIGNEXTEND -> pop2push { t1, t0 ->
            val top0Int = t0.big
            if (top0Int > 31.toBigInteger()) return@pop2push t1

            var byteIndex = 31 - top0Int.toInt()
            val toFill = if (t1.bytes[31 - top0Int.toInt()] < 0) 0xFF.toByte() else 0x00
            ByteArray(32).apply {
                fill(toFill, 0, byteIndex)
                for (byte in t1.bytes.drop(byteIndex)) {
                    this[byteIndex++] = byte
                }
            }.toElement()
        }.withGas5()

        OpCode.LT -> pop2push { t1, t0 -> if (t0 < t1) EVMStackElement.ONE else EVMStackElement.ZERO }.withGas3()
        OpCode.GT -> pop2push { t1, t0 -> if (t0 > t1) EVMStackElement.ONE else EVMStackElement.ZERO }.withGas3()
        OpCode.SLT -> pop2push { t1, t0 -> if (t0.signed() < t1.signed()) EVMStackElement.ONE else EVMStackElement.ZERO }.withGas3()
        OpCode.SGT -> pop2push { t1, t0 -> if (t0.signed() > t1.signed()) EVMStackElement.ONE else EVMStackElement.ZERO }.withGas3()
        OpCode.EQ -> pop2push { t1, t0 -> if (t0 == t1) EVMStackElement.ONE else EVMStackElement.ZERO }.withGas3()
        OpCode.ISZERO -> pop1push { t0 -> if (t0 == EVMStackElement.ZERO) EVMStackElement.ONE else EVMStackElement.ZERO }.withGas3()
        OpCode.AND -> pop2push { t1, t0 -> t0 and t1 }.withGas3()
        OpCode.OR -> pop2push { t1, t0 -> t0 or t1 }.withGas3()
        OpCode.XOR -> pop2push { t1, t0 -> t0 xor t1 }.withGas3()
        OpCode.NOT -> pop1push { t0 -> t0.not() }.withGas3()
        OpCode.BYTE -> pop2push { t1, t0 ->
            val index = t0.big
            if (32.toBigInteger() <= index) return@pop2push EVMStackElement.ZERO
            val pos = index.toInt() - 32 + t1.bytes.size
            if (pos < 0 || 32 <= pos) return@pop2push EVMStackElement.ZERO
            byteArrayOf(t1.bytes[pos]).toElement()
        }.withGas3()

        OpCode.SHL -> pop2push { value, shift ->
            when (shift.big <= 256.toBigInteger()) {
                true -> (value.big shl shift.int).toElement()
                false -> EVMStackElement.ZERO
            }
        }.withGas3()

        OpCode.SHR -> pop2push { value, shift ->
            when (shift.big <= 256.toBigInteger()) {
                true -> (value.big shr shift.int).toElement()
                false -> EVMStackElement.ZERO
            }
        }.withGas3()

        OpCode.SAR -> pop2push { value, shift ->
            if (shift.big > 256.toBigInteger()) {
                if (value.bytes[0] < 0) EVMStackElement.MAX
                else EVMStackElement.ZERO
            } else {
                if (value.bytes[0] < 0) {
                    val significantBits = EVMStackElement.MAX.big.shl(256 - shift.int)
                    (value.big shr shift.int or significantBits).toElement()
                } else {
                    value.big.shr(shift.int).toElement()
                }
            }
        }.withGas3()

        OpCode.KECCAK256 -> pop2push { size, offset ->
            val input = memory.read(offset.int, size.int)
            Keccak.Digest256().digest(input).toElement()
        }.withGas(30).withMemorySize {
            val offset = stack.back(0)
            val size = stack.back(1)
            offset.int + size.int
        }.withDynamicGas { memorySize ->
            val gas = memoryGasCost(memorySize)
            val size = stack.back(1).int
            val wordSize = (size + 31) / 32
            gas + (wordSize * 6)
        }

        OpCode.ADDRESS -> push { contract.address.toElement() }.withGas2()

        OpCode.BALANCE -> pop1push { address ->
            db.findAccount(address.toAddress())?.balance?.toElement() ?: EVMStackElement.ZERO
        }.withGas(20)

        OpCode.ORIGIN -> push { transaction.from.toElement() }.withGas2()
        OpCode.CALLER -> push { caller.toElement() }.withGas2()
        OpCode.CALLVALUE -> push { callValue.toElement() }.withGas2()
        OpCode.CALLDATALOAD -> pop1push { t0 -> callData.read(t0.int, 32).toElement() }.withGas3()
        OpCode.CALLDATASIZE -> push { callData.size.toElement() }.withGas2()

        OpCode.CALLDATACOPY -> pop3 { length, dataOffset, memOffset ->
            val callData = callData.read(dataOffset.int, length.int)
            memory.write(memOffset.int, callData)
        }.withGas3()
            .withDynamicGas { memorySize -> memoryCopyGas(2, memorySize) }
            .withMemorySize { stack.back(2).int + stack.back(0).int }

        OpCode.CODESIZE -> push { contract.code.size.toElement() }.withGas2()

        OpCode.CODECOPY -> pop3 { length, dataOffset, memOffset ->
            val code = contract.code.read(dataOffset.int, length.int)
            memory.write(memOffset.int, code)
        }

        OpCode.GASPRICE -> push { transaction.gasPrice.toElement() }.withGas2()

        OpCode.EXTCODESIZE -> pop1push { address ->
            db.withAccountOrNull(address.toAddress()) { it?.getCode()?.size ?: 0 }.toElement()
        }.withGas(20)

        OpCode.EXTCODECOPY -> withExecute { TODO() }
        OpCode.RETURNDATASIZE -> push { nextFrame?.result?.data?.size?.toElement() ?: EVMStackElement.ZERO }.withGas2()
        OpCode.RETURNDATACOPY -> pop3 { length, dataOffset, memOffset ->
            val returnValue = nextFrame?.result?.data?.read(dataOffset.int, length.int)
            memory.write(memOffset.int, returnValue ?: ByteArray(length.int))
        }.withDynamicGas { memorySize -> memoryCopyGas(2, memorySize) }.withGas3()

        OpCode.EXTCODEHASH -> pop1push { address ->
            db.findAccount(address.toAddress())?.codeHash?.bytes?.toElement() ?: EVMStackElement.ZERO
        }

        OpCode.BLOCKHASH -> pop1 { }.withGas(20)
        OpCode.COINBASE -> push { block.coinbase.toElement() }.withGas2()
        OpCode.TIMESTAMP -> push { block.time.toElement() }.withGas2()
        OpCode.NUMBER -> push { block.number.toElement() }.withGas2()
        OpCode.DIFFICULTY -> push { block.difficulty.toElement() }.withGas2()

        OpCode.RANDOM,
        OpCode.PREVRANDAO -> push { block.random.toElement() }.withGas2()

        OpCode.GASLIMIT -> push { block.gasLimit.toElement() }.withGas2()
        OpCode.CHAINID -> pop0 { }.withGas2()
        OpCode.SELFBALANCE -> push {
            db.findAccount(contract.address)?.balance?.toElement() ?: EVMStackElement.ZERO
        }.withGas5()

        OpCode.BASEFEE -> push { block.baseFee.toElement() }.withGas2()
        OpCode.BLOBHASH -> pop1push { TODO() }.withGas3()
        OpCode.BLOBBASEFEE -> push { TODO() }.withGas2()
        OpCode.POP -> pop1 { _ -> }.withGas2()

        OpCode.MLOAD -> pop1push { offset -> memory.read(offset.int, 32).toElement() }
            .withMemorySize { stack.back(0).int + 32 }
            .withGas3()
            .withDynamicGas { memoryGasCost(stack.back(0).int + 32) }

        OpCode.MSTORE -> withGas3().pop2 { value, offset ->
            memory.write(offset.int, value.bytes.sliceArrayLast(32))
        }.withMemorySize {
            stack.back(0).int + 32
        }.withDynamicGas {
            memoryGasCost(stack.back(0).int + 32)
        }

        OpCode.MSTORE8 -> pop2 { a, b -> TODO() }
        OpCode.SLOAD -> pop1push { key ->
            db.withAccountOrNull(contract.address) { it?.storage?.get(key.bytes) }?.toElement()
                ?: EVMStackElement.ZERO
        }.withGas(2100) // SloadGasFrontier 50

        OpCode.SSTORE -> pop2 { value, location ->
            db.withAccountOrCreate(contract.address) { it.storage.set(location.bytes, value.bytes) }
        }.withGas(2900)

        OpCode.JUMP -> pop1 { pos -> pc = pos.int - 1 }.withGas8()
        OpCode.JUMPI -> pop2 { condition, pos ->
            if (condition == EVMStackElement.ZERO) return@pop2
            pc = pos.int - 1 // pc will be increased by the interpreter loop
        }.withGas(10)

        OpCode.PC -> push { pc.toElement() }
        OpCode.MSIZE -> pop0 { }.withGas2()
        OpCode.GAS -> push { gas.toElement() }.withGas2()
        OpCode.JUMPDEST -> pop0 { }.withGas(1)
        OpCode.TLOAD -> pop1 { }
        OpCode.TSTORE -> pop2 { a, b -> }
        OpCode.MCOPY -> pop3 { a, b, c -> }.withGas3()

        OpCode.PUSH0 -> push { EVMStackElement.ZERO }.withGas2()
        OpCode.PUSH1 -> push { byteArrayOf(contract.code[++pc]).toElement() }.withGas3()
        OpCode.PUSH2,
        OpCode.PUSH3,
        OpCode.PUSH4,
        OpCode.PUSH5,
        OpCode.PUSH6,
        OpCode.PUSH7,
        OpCode.PUSH8,
        OpCode.PUSH9,
        OpCode.PUSH10,
        OpCode.PUSH11,
        OpCode.PUSH12,
        OpCode.PUSH13,
        OpCode.PUSH14,
        OpCode.PUSH15,
        OpCode.PUSH16,
        OpCode.PUSH17,
        OpCode.PUSH18,
        OpCode.PUSH19,
        OpCode.PUSH20,
        OpCode.PUSH21,
        OpCode.PUSH22,
        OpCode.PUSH23,
        OpCode.PUSH24,
        OpCode.PUSH25,
        OpCode.PUSH26,
        OpCode.PUSH27,
        OpCode.PUSH28,
        OpCode.PUSH29,
        OpCode.PUSH30,
        OpCode.PUSH31,
        OpCode.PUSH32 -> push {
            val size = opCode.name.drop(4).toInt()
            contract.code.read(pc + 1, size).toElement().apply { pc += size }
        }.withGas3()

        OpCode.DUP1,
        OpCode.DUP2,
        OpCode.DUP3,
        OpCode.DUP4,
        OpCode.DUP5,
        OpCode.DUP6,
        OpCode.DUP7,
        OpCode.DUP8,
        OpCode.DUP9,
        OpCode.DUP10,
        OpCode.DUP11,
        OpCode.DUP12,
        OpCode.DUP13,
        OpCode.DUP14,
        OpCode.DUP15,
        OpCode.DUP16 -> withExecute {
            val size = opCode.name.drop(3).toInt()
            stack.push(stack.back(size - 1))
        }.withGas3()

        OpCode.SWAP1,
        OpCode.SWAP2,
        OpCode.SWAP3,
        OpCode.SWAP4,
        OpCode.SWAP5,
        OpCode.SWAP6,
        OpCode.SWAP7,
        OpCode.SWAP8,
        OpCode.SWAP9,
        OpCode.SWAP10,
        OpCode.SWAP11,
        OpCode.SWAP12,
        OpCode.SWAP13,
        OpCode.SWAP14,
        OpCode.SWAP15,
        OpCode.SWAP16 -> withExecute {
            val size = opCode.name.drop(4).toInt()
            val a = stack.back(0)
            val b = stack.back(size)
            stack[stack.lastIndex] = b
            stack[stack.lastIndex - size] = a
        }.withGas3()

        OpCode.LOG0,
        OpCode.LOG1,
        OpCode.LOG2,
        OpCode.LOG3,
        OpCode.LOG4 -> withExecute {
            val topicCount = opCode.name.drop(3).toInt()
            val offset = stack.pop().int
            val size = stack.pop().int
            val topics = (1..topicCount).map { _ -> stack.pop().bytes }
            val data = memory.read(offset, size)
            addLog(topics, data)
        }.withMemorySize {
            val offset = stack.back(0).int
            val size = stack.back(1).int
            offset + size
        }.withDynamicGas { memorySize ->
            val topicCount = opCode.name.drop(3).toInt()
            val size = stack.back(1).int
            var gas = memoryGasCost(memorySize)
            gas += 375
            gas += topicCount * 375
            gas += size * 8
            gas
        }

        OpCode.CREATE -> pop3 { top2, top1, top0 -> TODO() }
        OpCode.CALL -> withExecute { TODO() }
        OpCode.CALLCODE -> withExecute { TODO() }
        OpCode.RETURN -> pop2 { size, offset -> result = EVMReturn.success(memory.read(offset.int, size.int)) }

        OpCode.DELEGATECALL -> withExecute {
            val gas = stack.pop()
            val addr = stack.pop()
            val argsOffset = stack.pop().int
            val argsLength = stack.pop().int
            val retOffset = stack.pop()
            val retLength = stack.pop()

            val result = nextFrame {
                val calldata = memory.read(argsOffset, argsLength)
                val contract = db.withAccountOrThrow(addr.toAddress()) {
                    EVMContract(contract.address, requireNotNull(it.getCode()), requireNotNull(it.codeHash))
                }
                FrameContext(caller, callValue, calldata, contract, callGasTemp)
            }
            if (result.err == null) stack.push(EVMStackElement.ONE) else stack.push(EVMStackElement.ZERO)
            memory.write(retOffset.int, retLength.int, result.data)
        }.withMemorySize {
            val retSize = stack.back(5).int + stack.back(4).int
            val argSize = stack.back(3).int + stack.back(2).int
            if (retSize > argSize) retSize else argSize
        }.withGas(100).withDynamicGas { memorySize ->
            val address = stack.back(1)
            val coldAccess = true // todo
//            coldCost := params.ColdAccountAccessCostEIP2929 - params.WarmStorageReadCostEIP2929
            val coldCost = 2600 - 100
            if (coldAccess) {
                gas -= coldCost
            }
            //
            val base = memoryGasCost(memorySize)
            val eip150 = true
            callGasTemp = if (eip150) {
                val availableGas = gas - base
                availableGas - availableGas / 64
            } else {
                stack.back(0).int
            }
            val nextGas = base + callGasTemp
            //
            if (coldAccess) {
                gas += coldCost
                nextGas + coldCost
            } else {
                nextGas
            }
        }

        OpCode.CREATE2 -> withExecute { TODO() }
        OpCode.STATICCALL -> pop6push { retLength, retOffset, argsLength, argsOffset, addr, gas ->
            // We do an AddBalance of zero here, just in order to trigger a touch.
            // This doesn't matter on Mainnet, where all empties are gone at the time of Byzantium,
            // but is the correct thing to do and matters on other networks, in tests, and potential
            // future scenarios
            db.withAccountOrThrow(addr.toAddress()) { it.balance += BigInteger.ZERO }

            val result = nextFrame {
                val calldata = memory.read(argsOffset.int, argsLength.int)
                val contract = db.withAccountOrThrow(addr.toAddress(), EVMContract::of)
                FrameContext(this.contract.address, BigInteger.ZERO, calldata, contract, callGasTemp)
            }
            memory.write(retOffset.int, retLength.int, result.data)
            if (result.err == null) EVMStackElement.ONE else EVMStackElement.ZERO
        }.withMemorySize {
            val retSize = stack.back(5).int + stack.back(4).int
            val argSize = stack.back(3).int + stack.back(2).int
            if (retSize > argSize) retSize else argSize
        }.withGas(100).withDynamicGas { memorySize ->
            val address = stack.back(1)
            val coldAccess = false // todo
//            coldCost := params.ColdAccountAccessCostEIP2929 - params.WarmStorageReadCostEIP2929
            val coldCost = 2600 - 100
            if (coldAccess) {
                gas -= coldCost
            }
            //
            val base = memoryGasCost(memorySize)
            val eip150 = true
            callGasTemp = if (eip150) {
                val availableGas = gas - base
                availableGas - availableGas / 64
            } else {
                stack.back(0).int
            }
            val nextGas = base + callGasTemp
            //
            if (coldAccess) {
                gas += coldCost
                nextGas + coldCost
            } else {
                nextGas
            }
        }

        OpCode.REVERT -> pop2 { a, b -> TODO() }
        OpCode.INVALID -> pop0 { }
        OpCode.SELFDESTRUCT -> pop1 { TODO() }
    }
}
