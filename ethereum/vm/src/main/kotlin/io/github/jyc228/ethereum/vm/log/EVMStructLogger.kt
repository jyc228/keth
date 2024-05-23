package io.github.jyc228.ethereum.vm.log

import io.github.jyc228.ethereum.state.AbstractStateDatabase
import io.github.jyc228.ethereum.state.StateDatabase
import io.github.jyc228.ethereum.state.account.Address
import io.github.jyc228.ethereum.state.account.ManagedStateAccount
import io.github.jyc228.ethereum.state.account.StateRoot
import io.github.jyc228.ethereum.vm.EVMFrame
import io.github.jyc228.ethereum.vm.EVMInterpreterDelegate
import io.github.jyc228.ethereum.vm.EVMReturn
import io.github.jyc228.ethereum.vm.OpCode
import io.github.jyc228.ethereum.vm.Operation
import java.nio.ByteBuffer
import kotlinx.serialization.Serializable

class EVMStructLogger(
    private val enableMemory: Boolean = false,
    private val enableStorage: Boolean = false
) : EVMInterpreterDelegate, AbstractStateDatabase<ManagedStateAccount>() {
    val logs = mutableListOf<StructLog>()
    private val storage: MutableMap<Address, MutableMap<String, String>> = mutableMapOf()
    private lateinit var originDB: StateDatabase

    override suspend fun execute(frame: EVMFrame, execute: suspend (EVMFrame) -> EVMReturn): EVMReturn {
        if (frame.depth == 0) {
            originDB = frame.db
        }
        return execute(frame.with(this, frame.block, frame.transaction))
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun execute(
        frame: EVMFrame,
        operation: Operation?,
        execute: suspend (EVMFrame, Operation?) -> EVMReturn?
    ): EVMReturn? {
        val nextFrameHash = frame.nextFrame?.hashCode()
        logs += StructLog(
            pc = frame.pc,
            op = operation?.opCode,
            gas = frame.gas,
            gasCost = frame.gas,
            memory = frame.memory.takeIf { enableMemory }?.toHexString(),
            memorySize = frame.memory.size.takeIf { enableMemory },
            stack = frame.stack.map { it.toHexString() },
            returnData = frame.result?.data?.toHexString(),
            storage = mapOf(),
            depth = frame.depth + 1,
            refundCounter = null,
            err = frame.result?.err?.toString()
        )
        val index = logs.lastIndex
        return execute(frame, operation).apply {
            if (nextFrameHash == frame.nextFrame?.hashCode()) {
                logs[index].gasCost -= frame.gas
            } else {
                logs[index].gasCost = logs[index].gas - (frame.gas - (frame.nextFrame?.gas ?: 0))
            }
            if (enableStorage && (logs[index].op == OpCode.SLOAD || logs[index].op == OpCode.SSTORE)) {
                logs[index].storage = storage[frame.contract.address]!!.toMap()
            }
        }
    }

    override suspend fun createAccount(
        address: Address,
        callback: (suspend (ManagedStateAccount) -> Unit)?
    ): ManagedStateAccount {
        TODO("Not yet implemented")
    }

    override suspend fun findAccount(address: Address): ManagedStateAccount? {
        return originDB.withAccount(address) { DelegatedStateAccount(this, it) }
    }

    override suspend fun commit(deleteEmpty: Boolean): StateRoot? {
        TODO("Not yet implemented")
    }

    override suspend fun intermediateRoot(deleteEmpty: Boolean): StateRoot? {
        TODO("Not yet implemented")
    }

    override fun snapshot(): Int {
        TODO("Not yet implemented")
    }

    override fun revertSnapshot(id: Int) {
        TODO("Not yet implemented")
    }

    override fun dump() {
        TODO("Not yet implemented")
    }

    @OptIn(ExperimentalStdlibApi::class)
    private class DelegatedStateAccount(
        private val logger: EVMStructLogger,
        private val delegate: ManagedStateAccount
    ) : ManagedStateAccount by delegate, ManagedStateAccount.Storage by delegate.storage {
        override val storage: ManagedStateAccount.Storage get() = this
        override suspend fun get(key: ByteArray): ByteArray? {
            return delegate.storage.get(key).also {
                logger.storage.getOrPut(address) { mutableMapOf() }[key.to32ByteHexString()] = it.to32ByteHexString()
            }
        }

        override suspend fun set(key: ByteArray, value: ByteArray?) {
            delegate.storage.set(key, value)
            logger.storage.getOrPut(address) { mutableMapOf() }[key.to32ByteHexString()] = value.to32ByteHexString()
        }

        private fun ByteArray?.to32ByteHexString(): String {
            if (this == null) return "0x${"0".repeat(64)}"
            if (size != 32) return "0x${ByteBuffer.allocate(32).position(32 - size).put(this).array().toHexString()}"
            return "0x${toHexString()}"
        }
    }
}

@Serializable
data class StructLog(
    val pc: Int,
    val op: OpCode?,
    val gas: Int,
    var gasCost: Int,
    val memory: String? = null,
    val memorySize: Int? = null,
    val stack: List<String> = emptyList(),
    val returnData: String? = null,
    var storage: Map<String, String> = emptyMap(),
    val depth: Int,
    val refundCounter: ULong? = null,
    val err: String? = null
)
