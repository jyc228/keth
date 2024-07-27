package com.github.jyc228.keth.state

import com.github.jyc228.keth.rpc.EthereumClient
import com.github.jyc228.keth.rpc.eth.AccountProof
import com.github.jyc228.keth.state.account.CodeHash
import com.github.jyc228.keth.state.account.ManagedStateAccount
import com.github.jyc228.keth.state.account.StateRoot
import com.github.jyc228.keth.state.account.StorageRoot
import com.github.jyc228.keth.type.Address
import com.github.jyc228.keth.type.BlockReference
import com.github.jyc228.keth.type.Hash
import com.github.jyc228.keth.type.HexData
import java.math.BigInteger

class OffchainStateDatabase(
    private val originalRoot: Hash,
    private val client: EthereumClient
) : AbstractStateDatabase<ManagedStateAccount>() {

    private val ref = BlockReference.fromHex(originalRoot)
    private val accountByAddress = mutableMapOf<Address, OffchainManagedStateAccount>()

    override suspend fun createAccount(
        address: Address,
        callback: (suspend (ManagedStateAccount) -> Unit)?
    ): ManagedStateAccount {
        TODO("Not yet implemented")
    }

    override suspend fun findAccount(address: Address): ManagedStateAccount? {
        return accountByAddress.getOrPut(address) {
            val proof = client.eth.getProof(address, emptyList(), ref).awaitOrNull() ?: return null
            OffchainManagedStateAccount.fromProof(address, proof, client, ref)
        }
    }

    override suspend fun commit(): StateRoot? {
        TODO("Not yet implemented")
    }

    override suspend fun intermediateRoot(): StateRoot? {
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

    private class OffchainManagedStateAccount(
        override var nonce: ULong,
        override var balance: BigInteger,
        override val root: StorageRoot?,
        override val codeHash: CodeHash?,
        override val address: Address,
        private val client: EthereumClient,
        private val ref: BlockReference,
    ) : ManagedStateAccount, ManagedStateAccount.Storage {
        override val storage: ManagedStateAccount.Storage get() = this
        private val origin = mutableMapOf<String, ByteArray?>()
        private val dirty = mutableMapOf<String, ByteArray?>()

        var code: ByteArray? = null

        @OptIn(ExperimentalStdlibApi::class)
        override suspend fun getCode(): ByteArray? {
            if (code == null && codeHash != null) {
                code = client.eth.getCode(address, ref).awaitOrNull()?.hex?.removePrefix("0x")?.hexToByteArray()
            }
            return code
        }

        override suspend fun setCode(code: ByteArray?) {
            this.code = code
        }

        @OptIn(ExperimentalStdlibApi::class)
        override suspend fun get(key: ByteArray): ByteArray? {
            if (key.toHexString() in dirty) {
                return dirty[key.toHexString()]
            }
            return getCommittedState(key)
        }

        @OptIn(ExperimentalStdlibApi::class)
        override suspend fun getCommittedState(key: ByteArray): ByteArray? {
            if (key.toHexString() in origin) {
                return origin[key.toHexString()]
            }
            return client.eth.getStorageAt(address, HexData.fromByteArray(key), ref).awaitOrNull()
                ?.hex
                ?.removePrefix("0x")
                ?.hexToByteArray()
                ?.also { origin[key.toHexString()] = if (it.all { c -> c == 0.toByte() }) null else it }
        }

        @OptIn(ExperimentalStdlibApi::class)
        override suspend fun set(key: ByteArray, value: ByteArray?) {
            dirty[key.toHexString()] = value
        }

        companion object {
            fun fromProof(
                address: Address,
                proof: AccountProof,
                client: EthereumClient,
                ref: BlockReference
            ) = OffchainManagedStateAccount(
                nonce = proof.nonce.number,
                balance = proof.balance.number,
                root = StorageRoot.fromHexString(proof.storageHash.hex),
                codeHash = CodeHash.fromHexString(proof.codeHash.hex),
                address = address,
                client = client,
                ref = ref,
            )
        }
    }
}