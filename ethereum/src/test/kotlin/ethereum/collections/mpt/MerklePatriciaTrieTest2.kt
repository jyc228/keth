package ethereum.collections.mpt

import ethereum.collections.Hash
import ethereum.core.database.TreeDatabase
import ethereum.db.InMemoryKeyValueDatabase
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MerklePatriciaTrieTest2 {

    @Test
    fun `updating empty trie`() {
        val key = ""
        val value = "test"

        val trie = MerklePatriciaTrie.empty { null }
        trie[key] = value

        trie[key] shouldBe value.toByteArray()
    }

    @Test
    fun `root missing`() {
        val root = Hash.fromString("0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33")

//        shouldThrowAny { MerklePatriciaTrie.fromHash(root, initResolveHashNodeFunc()) }
    }

    @Test
    fun `insert 1`() {
        val trie = MerklePatriciaTrie.empty { null }

        trie["doe"] = "reindeer"
        trie["dog"] = "puppy"
        trie["dogglesworth"] = "cat"

        trie.rootHash() shouldBe Hash.fromHexString("8aad789dff2f538bca5d8ea56e8abe10f4c7ba3a5dea95fea4cd6e7c3a1168d3")
    }

    @Test
    fun `insert 2`() {
        val trie = MerklePatriciaTrie.empty { null }

        trie["A"] = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

        trie.rootHash() shouldBe Hash.fromHexString("d23786fb4a010da3ce639d66d5e904a11dbc02746d1ce25029e53290cabf28ab")

        trie["A"] = "bb"
        trie["A"] shouldBe "bb".toByteArray()
    }

    @Test
    fun `insert empty value`() {
        val trie = MerklePatriciaTrie.empty { null }

        trie["do"] = "verb"
        trie["ether"] = "wookiedoo"
        trie["horse"] = "stallion"
        trie["shaman"] = "horse"
        trie["doge"] = "coin"
        trie["ether"] = ""
        trie["dog"] = "puppy"
        trie["shaman"] = ""

        trie.rootHash() shouldBe Hash.fromHexString("5991bb8c6514148a29db676a14ac506cd2cd5775ace63c30a4fe457715e9ac84")
    }

    @Test
    fun `get`() {
//        val db = TreeDatabase(InMemoryKeyValueDatabase())
//        var trie = MerklePatriciaTrie.empty(initResolveHashNodeFunc(db))
//
//        trie["doe"] = "reindeer"
//        trie["dog"] = "puppy"
//        trie["dogglesworth"] = "cat"
//
//        trie["dog"] shouldBe "puppy".toByteArray()
//        trie["unknown"] shouldBe null
//
//        val collector = MerkleTreeNodeCollector()
//        val nodeSet = collector.commitNode(Hash.EMTPY, trie, false)
//        db.update(collector)
////        trie = MerklePatriciaTrie.fromHash(nodeSet?.root!!, initResolveHashNodeFunc(db))
//
//        trie["dog"] shouldBe "puppy".toByteArray()
//
//        println(trie.toString())
    }


    @Test
    fun `ss`() {
//        val db = TreeDatabase(InMemoryKeyValueDatabase())
//        var trie = MerklePatriciaTrie.empty(initResolveHashNodeFunc(db))
//        var chars = ('a'..'z') + ('A'..'Z')
//        repeat(300) {
//            val key = (1..Random.nextInt(5, 20)).map { chars[Random.nextInt(0, chars.size)] }.joinToString()
//            val value = (1..Random.nextInt(5, 20)).map { chars[Random.nextInt(0, chars.size)] }.joinToString()
//            trie[key] = value
//        }
//
//        println(trie.toString())
////        trie.commit(false)
    }

    private fun initResolveHashNodeFunc(db: TreeDatabase = TreeDatabase(InMemoryKeyValueDatabase())): (Hash) -> ByteArray? {
        return { hash -> db.node(hash) }
    }
}