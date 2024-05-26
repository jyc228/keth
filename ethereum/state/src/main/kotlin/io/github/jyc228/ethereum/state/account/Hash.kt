package io.github.jyc228.ethereum.state.account

import org.bouncycastle.jcajce.provider.digest.Keccak

@JvmInline
value class AddressHash(val bytes: ByteArray) {
    companion object : HashFactory<AddressHash>(::AddressHash)
}

@JvmInline
value class StateRoot(val bytes: ByteArray) {
    companion object : HashFactory<StateRoot>(::StateRoot)
}

@JvmInline
value class StorageRoot(val bytes: ByteArray) {
    companion object : HashFactory<StorageRoot>(::StorageRoot)
}

@JvmInline
value class CodeHash(val bytes: ByteArray) {
    companion object : HashFactory<CodeHash>(
        ::CodeHash,
        "c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470"
    )
}

@OptIn(ExperimentalStdlibApi::class)
abstract class HashFactory<T>(
    val new: (ByteArray) -> T,
    val emptyString: String = "56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"
) {
    val empty: T = new(emptyString.hexToByteArray())
    fun fromHexString(hex: String): T? = hex.removePrefix("0x").takeIf { it != emptyString }?.hexToByteArray()?.let(new)
    fun fromByteArray(bytes: ByteArray): T? = when (bytes contentEquals emptyString.hexToByteArray()) {
        true -> null
        false -> new(bytes)
    }

    fun keccak256FromBytes(bytes: ByteArray) = new(bytes.keccak256())
}

fun ByteArray.keccak256(): ByteArray = Keccak.Digest256().digest(this)