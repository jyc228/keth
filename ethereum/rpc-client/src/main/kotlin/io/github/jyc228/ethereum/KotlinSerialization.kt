package io.github.jyc228.ethereum

import io.github.jyc228.ethereum.rpc.eth.Block
import io.github.jyc228.ethereum.rpc.eth.FullBlock
import io.github.jyc228.ethereum.rpc.eth.MutableAccessListTransaction
import io.github.jyc228.ethereum.rpc.eth.MutableBlobTransaction
import io.github.jyc228.ethereum.rpc.eth.MutableDepositTransaction
import io.github.jyc228.ethereum.rpc.eth.MutableDynamicFeeTransaction
import io.github.jyc228.ethereum.rpc.eth.MutableLegacyTransaction
import io.github.jyc228.ethereum.rpc.eth.SimpleBlock
import io.github.jyc228.ethereum.rpc.eth.Transaction
import io.github.jyc228.ethereum.rpc.eth.TransactionStatus
import io.github.jyc228.ethereum.rpc.eth.TransactionType
import kotlinx.datetime.Instant
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal abstract class HexStringSerializer<T : HexString>(val toObject: (String) -> T) : KSerializer<T> {
    override val descriptor = PrimitiveSerialDescriptor(this::class.qualifiedName ?: error(""), PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(value.hex.lowercase())
    override fun deserialize(decoder: Decoder) = toObject(decoder.decodeString().lowercase())
}

internal object HashSerializer : HexStringSerializer<Hash>(::Hash)
internal object AddressSerializer : HexStringSerializer<Address>(::Address)
internal object HexIntSerializer : HexStringSerializer<HexInt>(::HexInt)
internal object HexULongSerializer : HexStringSerializer<HexULong>(::HexULong)
internal object HexBigIntSerializer : HexStringSerializer<HexBigInt>(::HexBigInt)
internal object HexDataSerializer : HexStringSerializer<HexData>(::HexData)
internal object TransactionTypeSerializer : HexStringSerializer<TransactionType>(TransactionType::from)
internal object TransactionStatusSerializer : HexStringSerializer<TransactionStatus>(TransactionStatus::from)

internal object InstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("io.github.jyc228.ethereum.Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.epochSeconds.toString(16))
    override fun deserialize(decoder: Decoder) =
        Instant.fromEpochSeconds(decoder.decodeString().removePrefix("0x").toLong(16))
}

internal abstract class NullSerializer<T>(
    private val serializer: KSerializer<T>,
    private val default: T
) : KSerializer<T> by serializer {
    override fun deserialize(decoder: Decoder): T = decoder.decodeNullableSerializableValue(serializer) ?: default
}

internal object BlockSerializer : JsonContentPolymorphicSerializer<Block>(Block::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Block> {
        if (element.jsonObject["transactions"]?.jsonArray?.get(0) is JsonPrimitive) {
            return SimpleBlock.serializer()
        }
        return FullBlock.serializer()
    }
}

internal object BlockTransactionsSerializer :
    JsonContentPolymorphicSerializer<Block.Transactions>(Block.Transactions::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Block.Transactions> {
        if (element.jsonArray.isEmpty() || element.jsonArray[0] is JsonPrimitive) {
            return TransactionHashesSerializer
        }
        return TransactionsSerializer
    }

    object TransactionHashesSerializer : KSerializer<SimpleBlock.TransactionHashes> {
        private val serializer = ListSerializer(HashSerializer)
        override val descriptor: SerialDescriptor get() = serializer.descriptor

        override fun deserialize(decoder: Decoder): SimpleBlock.TransactionHashes {
            return SimpleBlock.TransactionHashes(serializer.deserialize(decoder))
        }

        override fun serialize(encoder: Encoder, value: SimpleBlock.TransactionHashes) {
            serializer.serialize(encoder, value)
        }
    }

    object TransactionsSerializer : KSerializer<FullBlock.Transactions> {
        private val serializer = ListSerializer(Transaction.serializer())
        override val descriptor: SerialDescriptor get() = serializer.descriptor

        override fun deserialize(decoder: Decoder): FullBlock.Transactions {
            return FullBlock.Transactions(serializer.deserialize(decoder))
        }

        override fun serialize(encoder: Encoder, value: FullBlock.Transactions) {
            serializer.serialize(encoder, value)
        }
    }
}

internal object TransactionsSerializer : JsonContentPolymorphicSerializer<Transaction>(Transaction::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Transaction> {
        val type = element.jsonObject["type"]?.jsonPrimitive?.content ?: error("")
        return when (TransactionType.from(type)) {
            TransactionType.Legacy -> MutableLegacyTransaction.serializer()
            TransactionType.AccessList -> MutableAccessListTransaction.serializer()
            TransactionType.DynamicFee -> MutableDynamicFeeTransaction.serializer()
            TransactionType.Blob -> MutableBlobTransaction.serializer()
            is TransactionType.Custom -> MutableDepositTransaction.serializer()
        }
    }
}
