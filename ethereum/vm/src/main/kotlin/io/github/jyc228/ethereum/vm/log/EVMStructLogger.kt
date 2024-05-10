package io.github.jyc228.ethereum.vm.log

import io.github.jyc228.ethereum.state.AbstractStateDatabase
import io.github.jyc228.ethereum.state.StateDatabase
import io.github.jyc228.ethereum.state.account.Address
import io.github.jyc228.ethereum.state.account.ManagedStateAccount
import io.github.jyc228.ethereum.state.account.StateRoot
import io.github.jyc228.ethereum.vm.EVMContext
import io.github.jyc228.ethereum.vm.EVMInterpreterDelegate
import io.github.jyc228.ethereum.vm.EVMReturn
import io.github.jyc228.ethereum.vm.FrameContext
import io.github.jyc228.ethereum.vm.OpCode
import io.github.jyc228.ethereum.vm.Operation
import kotlinx.serialization.Serializable

class EVMStructLogger(
    private val enableMemory: Boolean = false,
    private val enableStorage: Boolean = false
) : EVMInterpreterDelegate, AbstractStateDatabase<ManagedStateAccount>() {
    val logs = mutableListOf<StructLog>()
    private val storage = mutableMapOf<String, String>()
    private lateinit var originDB: StateDatabase

    override suspend fun execute(frame: FrameContext, execute: suspend (FrameContext) -> EVMReturn): EVMReturn {
        if (frame.depth == 0) {
            originDB = frame.db
        }
        return execute(frame.with(vm = EVMContext(frame.block, this)))
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun execute(
        frame: FrameContext,
        operation: Operation?,
        execute: suspend (FrameContext, Operation?) -> EVMReturn?
    ): EVMReturn? {
        val storageHash = storage.hashCode()
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
            if (enableStorage && nextFrameHash == frame.nextFrame?.hashCode() && storageHash != storage.hashCode()) {
                logs[index].storage = storage.toMap()
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
            return delegate.storage.get(key).apply {
                logger.storage["0x${key.toHexString()}"] = "0x${this?.toHexString()}"
            }
        }

        override suspend fun set(key: ByteArray, value: ByteArray?) {
            delegate.storage.set(key, value)
            logger.storage["0x${key.toHexString()}"] = "0x${value?.toHexString()}"
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
