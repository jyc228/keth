package io.github.jyc228.ethereum.state

import io.github.jyc228.ethereum.BlockReference
import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.HexData
import io.github.jyc228.ethereum.rpc.EthereumClient
import io.github.jyc228.ethereum.rpc.eth.AccountProof
import io.github.jyc228.ethereum.state.account.Address
import io.github.jyc228.ethereum.state.account.CodeHash
import io.github.jyc228.ethereum.state.account.ManagedStateAccount
import io.github.jyc228.ethereum.state.account.StateRoot
import io.github.jyc228.ethereum.state.account.StorageRoot
import java.math.BigInteger

class OffchainStateDatabase(
    private val originalRoot: Hash,
    private val client: EthereumClient
) : AbstractStateDatabase<ManagedStateAccount>() {

    private val ref = BlockReference.fromHex(originalRoot.hex)
    private val accountByAddress = mutableMapOf<Address, OffchainManagedStateAccount>()

    override suspend fun createAccount(
        address: Address,
        callback: (suspend (ManagedStateAccount) -> Unit)?
    ): ManagedStateAccount {
        TODO("Not yet implemented")
    }

    override suspend fun findAccount(address: Address): ManagedStateAccount? {
        return accountByAddress.getOrPut(address) {
            val addr = io.github.jyc228.ethereum.Address("0x${address.hex}")
            val proof = client.eth.getProof(addr, emptyList(), ref).awaitOrNull() ?: return null
            OffchainManagedStateAccount.fromProof(address, proof, client)
        }
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

    private class OffchainManagedStateAccount(
        override var nonce: ULong,
        override var balance: BigInteger,
        override val root: StorageRoot?,
        override val codeHash: CodeHash?,
        override val address: Address,
        private val client: EthereumClient,
    ) : ManagedStateAccount, ManagedStateAccount.Storage {
        override val storage: ManagedStateAccount.Storage get() = this

        val addr = io.github.jyc228.ethereum.Address("0x${address.hex}")
        var code: ByteArray? = null

        @OptIn(ExperimentalStdlibApi::class)
        override suspend fun getCode(): ByteArray? {
            if (code == null && codeHash != null) {
                code = client.eth.getCode(addr).awaitOrNull()?.hex?.removePrefix("0x")?.hexToByteArray()
            }
            return code
        }

        override suspend fun setCode(code: ByteArray?) {
            this.code = code
        }

        @OptIn(ExperimentalStdlibApi::class)
        override suspend fun get(key: ByteArray): ByteArray? {
            return client.eth.getStorageAt(addr, HexData.create(key.toHexString())).awaitOrNull()
                ?.hex
                ?.removePrefix("0x")
                ?.hexToByteArray()
        }

        override suspend fun getCommittedState(key: ByteArray): ByteArray? = get(key)

        override suspend fun set(key: ByteArray, value: ByteArray?) {
            println("set ${key.contentToString()} : ${value.contentToString()}")
        }

        companion object {
            fun fromProof(address: Address, proof: AccountProof, client: EthereumClient) = OffchainManagedStateAccount(
                nonce = proof.nonce.number,
                balance = proof.balance.number,
                root = StorageRoot.fromHexString(proof.storageHash.hex),
                codeHash = CodeHash.fromHexString(proof.codeHash.hex),
                address = address,
                client = client,
            )
        }
    }
}