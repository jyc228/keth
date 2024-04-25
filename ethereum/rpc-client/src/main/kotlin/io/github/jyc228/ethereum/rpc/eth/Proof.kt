package io.github.jyc228.ethereum.rpc.eth

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.HexBigInt
import io.github.jyc228.ethereum.HexData
import io.github.jyc228.ethereum.HexULong
import kotlinx.serialization.Serializable

@Serializable
data class AccountProof(
    val address: Address,
    val accountProof: List<HexData>,
    val balance: HexBigInt,
    val codeHash: Hash,
    val nonce: HexULong,
    val storageHash: Hash,
    val storageProof: List<StorageProof>,
)

@Serializable
data class StorageProof(
    val key: HexData,
    val value: HexBigInt,
    val proof: List<HexData>
)