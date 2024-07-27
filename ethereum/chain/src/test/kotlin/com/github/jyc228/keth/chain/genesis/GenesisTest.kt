package com.github.jyc228.keth.chain.genesis

import com.github.jyc228.keth.collections.MerkleTreeDirtyNodes
import com.github.jyc228.keth.fork.HardForkManager
import com.github.jyc228.keth.state.ContractCodeDatabase
import com.github.jyc228.keth.state.OnchainStateDatabase
import com.github.jyc228.keth.state.TreeDatabase
import com.github.jyc228.keth.state.account.AddressHash
import com.github.jyc228.keth.state.account.CodeHash
import com.github.jyc228.keth.state.account.StateRoot
import com.github.jyc228.keth.type.Hash
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GenesisTest : DescribeSpec({
    context("mainnet header hash") {
        val genesis = Genesis.fromNetworkName("mainnet")
        val fork = HardForkManager.fromNetworkName("mainnet").findFork(genesis.number.number)
        val db = TestDB()
        val header = com.github.jyc228.keth.chain.BlockHeader.fromGenesis(
            genesis,
            OnchainStateDatabase.empty(db, db, "eip158" in fork.eips),
            fork
        )
        header.hash shouldBe Hash.fromHexString("0xd4e56740f876aef8c010b86a40d5f56745a118d0906a34e69aec8c0db1cb8fa3")
    }
})

class TestDB : TreeDatabase, ContractCodeDatabase {
    override fun node(hash: ByteArray): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun update(accountDirties: MerkleTreeDirtyNodes, storageDirties: Map<AddressHash, MerkleTreeDirtyNodes>) {

    }

    override fun commit(hash: StateRoot) {
        TODO("Not yet implemented")
    }

    override fun saveCode(codeHash: CodeHash, code: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun findCodeByCodeHash(codeHash: CodeHash): ByteArray? {
        TODO("Not yet implemented")
    }
}