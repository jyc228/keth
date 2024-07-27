package com.github.jyc228.keth.state

import com.github.jyc228.keth.collections.MerkleTreeDirtyNodes
import com.github.jyc228.keth.state.account.AddressHash
import com.github.jyc228.keth.state.account.CodeHash
import com.github.jyc228.keth.state.account.StateRoot

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