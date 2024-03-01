package ethereum.type.builder

import ethereum.crypto.ECDSASignature
import ethereum.evm.Address
import ethereum.type.Access
import ethereum.type.AccessListTransaction
import ethereum.type.LegacyTransaction
import ethereum.type.Transaction
import java.math.BigInteger

open class LegacyTransactionBuilder(
    var nonce: ULong = 0u,
    var gasPrice: BigInteger = BigInteger.ZERO,
    var gas: ULong = 0u,
    var to: Address = Address.EMPTY,
    var value: BigInteger = BigInteger.ZERO,
    var data: ByteArray? = null,
    var sig: ECDSASignature = ECDSASignature.Mutable(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO),
) {
    open fun build(mutate: (LegacyTransactionBuilder.() -> Unit)? = null): Transaction {
        mutate?.invoke(this)
        return Transaction(
            inner = LegacyTransaction(
                nonce = nonce,
                gasPrice = gasPrice,
                gas = gas,
                to = to,
                value = value,
                data = data,
                v = sig.v,
                r = sig.r,
                s = sig.s
            )
        )
    }
}

class AccessListTransactionBuilder(
    var chainId: ULong = 0u,
    val accessList: MutableList<Access> = mutableListOf()
) : LegacyTransactionBuilder() {
    fun mutate(callback: AccessListTransactionBuilder.() -> Unit): AccessListTransactionBuilder = apply { callback() }

    fun buildAccessListTx(mutate: (AccessListTransactionBuilder.() -> Unit)? = null): Transaction {
        mutate?.invoke(this)
        return Transaction(
            inner = AccessListTransaction(
                chainID = chainId,
                nonce = nonce,
                gasPrice = gasPrice,
                gas = gas,
                to = to,
                value = value,
                data = data,
                accessList = accessList,
                v = sig.v,
                r = sig.r,
                s = sig.s
            )
        )
    }
}