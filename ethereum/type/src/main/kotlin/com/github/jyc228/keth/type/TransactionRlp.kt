package com.github.jyc228.keth.type

import com.github.jyc228.keth.rlp.RLPBuilder
import com.github.jyc228.keth.rlp.RLPEncoder

@OptIn(ExperimentalStdlibApi::class)
object TransactionRlp {
    fun encode(tx: Transaction, withSignature: Boolean = false): ByteArray = when (tx.type) {
        is TransactionType.Legacy -> RLPEncoder.encodeArray {
            addULong(tx.nonce.number)
            addBigInt(tx.gasPrice?.number)
            addUInt(tx.gas.number.toInt().toUInt())
            addBytes(tx.to?.hex?.removePrefix("0x")?.hexToByteArray())
            addBigInt(tx.value.number)
            addBytes(tx.input.removePrefix("0x").hexToByteArray())
            if (withSignature) encodeSig(tx)
        }

        is TransactionType.AccessList -> RLPEncoder.encode {
            addByte(tx.type.value.toByte())
            addArray {
                addULong(tx.chainId?.number ?: 0uL)
                addULong(tx.nonce.number)
                addBigInt(tx.gasPrice?.number)
                addUInt(tx.gas.number.toInt().toUInt())
                addBytes(tx.to?.hex?.removePrefix("0x")?.hexToByteArray())
                addBigInt(tx.value.number)
                addBytes(tx.input.removePrefix("0x").hexToByteArray())
                encodeAccessList(tx.accessList)
                if (withSignature) encodeSig(tx)
            }
        }

        is TransactionType.DynamicFee -> RLPEncoder.encode {
            addByte(tx.type.value.toByte())
            addArray {
                addULong(tx.chainId?.number ?: 0uL)
                addULong(tx.nonce.number)
                addBigInt(tx.maxPriorityFeePerGas.number)
                addBigInt(tx.maxFeePerGas.number)
                addUInt(tx.gas.number.toInt().toUInt())
                addBytes(tx.to?.hex?.removePrefix("0x")?.hexToByteArray())
                addBigInt(tx.value.number)
                addBytes(tx.input.removePrefix("0x").hexToByteArray())
                encodeAccessList(tx.accessList)
                if (withSignature) encodeSig(tx)
            }
        }

        is TransactionType.Blob -> RLPEncoder.encode {
            addByte(tx.type.value.toByte())
            addArray {
                addULong(tx.chainId?.number ?: 0uL)
                addULong(tx.nonce.number)
                addBigInt(tx.maxPriorityFeePerGas.number)
                addBigInt(tx.maxFeePerGas.number)
                addUInt(tx.gas.number.toInt().toUInt())
                addBytes(tx.to?.hex?.removePrefix("0x")?.hexToByteArray())
                addBigInt(tx.value.number)
                addBytes(tx.input.removePrefix("0x").hexToByteArray())
                encodeAccessList(tx.accessList)
                addBigInt(tx.maxFeePerBlobGas.number)
                addArray {
                    tx.blobVersionedHashes.forEach { addBytes(it.hex.removePrefix("0x").hexToByteArray()) }
                }
                if (withSignature) encodeSig(tx)
            }
        }

        else -> error("unsupported transaction type ${tx.type}")
    }

    private fun RLPBuilder.encodeAccessList(accessList: List<Access>) = addArray {
        accessList.forEach { access ->
            addBytes(access.address.hex.removePrefix("0x").hexToByteArray())
            addArray {
                access.storageKeys.forEach { key ->
                    addBytes(key.hex.removePrefix("0x").hexToByteArray())
                }
            }
        }
    }

    private fun RLPBuilder.encodeSig(sig: ECDSASignature) {
        addBigInt(sig.v?.number)
        addBigInt(sig.r?.number)
        addBigInt(sig.s?.number)
    }
}