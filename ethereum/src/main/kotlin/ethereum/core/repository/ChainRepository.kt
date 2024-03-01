package ethereum.core.repository

import ethereum.collections.Hash
import ethereum.config.ChainConfig
import ethereum.db.KeyValueDatabase
import ethereum.rlp.RLPDecoder
import ethereum.rlp.RLPEncoder
import ethereum.rlp.rlpToObject
import ethereum.rlp.toRlp
import ethereum.type.Block
import ethereum.type.BlockBody
import ethereum.type.BlockHeader
import ethereum.type.Receipt
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ChainRepository(
    private val db: KeyValueDatabase
) {
    fun readHeadHeaderHash(): Hash? {
        return db["LastHeader".map { it.code.toByte() }.toByteArray()]?.let { Hash.fromByteArray(it) }
    }

    fun readHeadBlockHash(): Hash? {
        return db["LastBlock".map { it.code.toByte() }.toByteArray()]?.let { Hash.fromByteArray(it) }
    }

    fun writeHeadBlockHash(hash: Hash) {
        db["LastBlock".map { it.code.toByte() }.toByteArray()] = hash.bytes
    }

    fun readHeadFastBlockHash(): Hash? {
        return db["LastFast".map { it.code.toByte() }.toByteArray()]?.let { Hash.fromByteArray(it) }
    }

    fun readFinalizedBlockHash(): Hash? {
        return db["LastFinalized".map { it.code.toByte() }.toByteArray()]?.let { Hash.fromByteArray(it) }
    }

    fun readHeaderNumber(hash: Hash): ULong? {
        return db[key('H', hash.bytes)]?.let { ByteBuffer.wrap(it).getLong().toULong() }
    }

    fun readTotalDifficulty(hash: Hash, number: ULong): BigInteger? {
        return db[key('h', number.toBigEndian() + hash.bytes, 't')]?.let { RLPDecoder.decode(it).toBigInt() }
    }

    fun writeTotalDifficulty(hash: Hash, number: ULong, totalDifficulty: BigInteger) {
        db[key('h', number.toBigEndian() + hash.bytes, 't')] = RLPEncoder.encode { addBigInt(totalDifficulty) }
    }

    fun writeBlock(block: Block) {
        writeBlockHeader(block.header)
        writeBlockBody(block.header.hash, block.header.number, block.body)
    }

    fun writeBlockHeader(header: BlockHeader) {
        val rlp = header.toRlp()
        val hash = Hash.keccak256FromBytes(rlp).bytes
        val t = rlp.rlpToObject<BlockHeader>()
        db[key('H', hash)] = header.number.toBigEndian()
        db[key('h', header.number.toBigEndian() + hash)] = rlp
    }

    private fun writeBlockBody(hash: Hash, number: ULong, body: BlockBody) {
        db[key('b', number.toBigEndian() + hash.bytes)] = body.toRlp()
    }

    fun readBlock(hash: Hash, number: ULong): Block? {
        val header = readBlockHeader(hash, number) ?: return null
        val body = readBlockBody(hash, number) ?: return null
        return Block(header, body)
    }

    fun readBlockHeader(hash: Hash, number: ULong): BlockHeader? {
        return db[key('h', number.toBigEndian() + hash.bytes)]?.rlpToObject()
    }

    private fun readBlockBody(hash: Hash, number: ULong): BlockBody? {
        return BlockBody(emptyList(), emptyList(), emptyList())
    }

    fun writeCanonicalHash(hash: Hash, number: ULong) {
        db[key('h', number.toBigEndian(), 'n')] = hash.bytes
    }

    fun readCanonicalHash(number: ULong): Hash? {
        return db[key('h', number.toBigEndian(), 'n')]?.let { Hash.fromByteArray(it) }
    }

    fun deleteCanonicalHash(number: ULong) {
        db -= key('h', number.toBigEndian(), 'n')
    }

    private fun key(prefix: Char, middle: ByteArray, postfix: Char? = null): ByteArray {
        return ByteBuffer.allocate((if (postfix == null) 1 else 2) + middle.size)
            .put(prefix.code.toByte())
            .put(middle)
            .apply { if (postfix != null) put(postfix.code.toByte()) }
            .array()
    }

    private fun ULong.toBigEndian() = ByteBuffer
        .allocate(ULong.SIZE_BYTES)
        .order(ByteOrder.BIG_ENDIAN)
        .putLong(toLong())
        .array()

    private fun BigInteger.toBigEndian() = toString().toULong().toBigEndian()
    fun writeReceipts(hash: Hash, number: ULong, receipts: List<Receipt>) {

    }

    fun writeHeadFastBlockHash(hash: Hash) {

    }

    fun writeHeadHeaderHash(hash: Hash) {
    }

    fun writeChainConfig(hash: Hash, config: ChainConfig) {

    }
}

