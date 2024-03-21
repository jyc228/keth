package io.github.jyc228.ethereum.rpc.eth

import io.github.jyc228.ethereum.Address
import io.github.jyc228.ethereum.Hash
import io.github.jyc228.ethereum.HexBigInt
import io.github.jyc228.ethereum.HexULong
import org.web3j.crypto.AccessListObject
import org.web3j.crypto.RawTransaction

class TransactionBuilder {
    var nonce: HexULong = HexULong(0u)
    var gasPrice: HexBigInt = HexBigInt.ZERO
    var gasLimit: HexBigInt = HexBigInt.ZERO
    var to: Address? = null
    var value: HexBigInt = HexBigInt.ZERO
    var input: String = ""
    var accessList: List<Access> = emptyList()
    var chainId: HexULong? = null
    var maxFeePerGas: HexBigInt = HexBigInt.ZERO
    var maxPriorityFeePerGas: HexBigInt = HexBigInt.ZERO
    var maxFeePerBlobGas: HexBigInt = HexBigInt.ZERO
    var blobVersionedHashes: List<Hash> = emptyList()
}

internal fun TransactionBuilder.toWeb3jTransaction(): RawTransaction {
    val chainId = chainId
    return when {
        chainId == null -> RawTransaction.createTransaction(
            nonce.number.toString().toBigInteger(),
            gasPrice.number,
            gasLimit.number,
            to?.hex,
            value.number,
            input
        )

        accessList.isNotEmpty() -> RawTransaction.createTransaction(
            chainId.number.toLong(),
            nonce.number.toString().toBigInteger(),
            gasPrice.number,
            gasLimit.number,
            to?.hex,
            value.number,
            input,
            accessList.map { AccessListObject(it.address.hex, it.storageKey.map { k -> k.hex }) }
        )

        else -> RawTransaction.createTransaction(
            chainId.number.toLong(),
            nonce.number.toString().toBigInteger(),
            gasLimit.number,
            to?.hex,
            value.number,
            input,
            maxPriorityFeePerGas.number,
            maxFeePerGas.number
        )
    }
}
