package ethereum.rlp

import java.math.BigInteger
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

// todo need to process optional field.

fun Any.toRlp(): ByteArray = RLPEncoder.encode { encodeUsingReflection(this@toRlp) }

fun <T : Any> ByteArray.rlpToObject(clazz: KClass<T>): T {
    return RLPDecoder.decode(this).toObject(clazz)
}

inline fun <reified T : Any> ByteArray.rlpToObject(): T {
    return RLPDecoder.decode(this).toObject(T::class)
}

private fun RLPBuilder.encodeUsingReflection(instance: Any): RLPBuilder {
    if (instance is Collection<*>) {
        return addArray {
            instance.forEach {
                when (it == null) {
                    true -> addEmptyString()
                    false -> encodeUsingReflection(it)
                }
            }
        }
    }
    val clazz = instance::class
    val const = clazz.primaryConstructor ?: error("${clazz.simpleName} primaryConstructor not exist.")
    return when (const.parameters.size) {
        0 -> this
        1 -> when (val value = clazz.memberProperties.first().call(instance)) {
            null -> addEmptyString()
            is ByteArray -> addBytes(value)
            is BigInteger -> addBigInt(value)
            is ULong -> addULong(value)
            is Int -> addULong(value.toULong())
            else -> encodeUsingReflection(value)
        }

        else -> addArray {
            val propertyByName = clazz.memberProperties.associateBy { it.name }
            const.parameters.forEach { p ->
                val property = propertyByName[p.name] ?: error("field ${p.name} does not exist")
                when (val value = property.call(instance)) {
                    null -> addNull(p.isOptional && property.returnType.isMarkedNullable)
                    is ByteArray -> addBytes(value)
                    is BigInteger -> addBigInt(value)
                    is ULong -> addULong(value)
                    else -> encodeUsingReflection(value)
                }
            }
        }
    }
}

fun <T : Any> RLPItem.toObject(clazz: KClass<T>, generic: List<KTypeProjection> = emptyList()): T {
    if (List::class.java.isAssignableFrom(clazz.java)) {
        val type = requireNotNull(generic.first().type?.classifier as KClass<*>?)
        return this.castArr().map { it.toObject(type) } as T
    }
    val const = clazz.primaryConstructor ?: error("${clazz.simpleName} primaryConstructor not exist.")
    val args: Map<KParameter, Any?> = when (this) {
        is RLPItem.Arr -> const.parameters.withIndex().associate { (index, p) ->
            if (p.isOptional && index >= this.size) {
                return@associate p to null
            }
            p to when (val pClass = p.type.classifier as KClass<*>) {
                ULong::class -> this[index].toULong()
                BigInteger::class -> this[index].toBigInt()
                ByteArray::class -> this[index].castStr().value.map { it.code.toByte() }.toByteArray()
                else -> this[index].toObject(pClass, p.type.arguments)
            }
        }

        is RLPItem.Str -> const.parameters.first().let { p ->
            val value = when (val pClass = p.type.classifier as KClass<*>) {
                ULong::class -> toULong()
                BigInteger::class -> toBigInt()
                ByteArray::class -> this.value.map { it.code.toByte() }.toByteArray()
                else -> toObject(pClass)
            }
            mapOf(p to value)
        }

        is RLPItem.Byte -> const.parameters.first().let { p ->
            when (p.type.classifier as KClass<*>) {
                ByteArray::class -> mapOf(p to byteArrayOf(value))
                else -> mapOf(p to value)
            }
        }
    }
    return const.callBy(args)
}
