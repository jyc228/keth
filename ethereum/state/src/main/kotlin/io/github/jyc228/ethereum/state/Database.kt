package io.github.jyc228.ethereum.state

import ethereum.collections.MerkleTreeDirtyNodes
import io.github.jyc228.ethereum.state.account.AddressHash
import io.github.jyc228.ethereum.state.account.CodeHash
import io.github.jyc228.ethereum.state.account.StateRoot

interface TreeDatabase {
    fun node(hash: ByteArray): ByteArray?

    /**
     * inserts the dirty nodes in provided nodeset into database and link the account trie with multiple storage tries if necessary.
     */
    fun update(accountDirties: MerkleTreeDirtyNodes, storageDirties: Map<AddressHash, MerkleTreeDirtyNodes>)

    fun commit(hash: StateRoot)
}

interface ContractCodeDatabase {
    fun saveCode(codeHash: CodeHash, code: ByteArray)
    fun findCodeByCodeHash(codeHash: CodeHash): ByteArray?
}