package ethereum.core.state.snapshot

import ethereum.collections.Hash

interface DiskLayer {
    //    val diskDB: KeyValueDB
//    val trieDB: TrieDB
    val root: Hash
}
