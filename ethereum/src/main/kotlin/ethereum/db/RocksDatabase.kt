package ethereum.db

import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.rocksdb.Options
import org.rocksdb.RocksDB


val inputs = (1..1).associate { Random.nextBytes(1000) to Random.nextBytes(1000000000) }

fun main() {
    RocksDB.loadLibrary()
    val db = RocksDB.open(
        Options().setMaxFileOpeningThreads(100).setMaxOpenFiles(100).setCreateIfMissing(true),
        "/Users/logan/Projects/my-projects/keth/ethereum/src/main/kotlin/ethereum/db/data"
    )
    println("noCoroutinePerformance -----------")
    val noCoroutinePerformance = measureTimedValue {
        inputs.entries.withIndex().sumOf { (i, it) ->
            measureTimeMillis { db.put(it.key, it.value) }.also { println("index $i put ${it.milliseconds}") }
        }.milliseconds
    }
    println()
    runBlocking {
        delay(3000)
        println("coroutinePerformance -----------")
        val coroutinePerformance = measureTimedValue {
            inputs.entries.withIndex().sumOf { (i, it) ->
                measureTimeMillis { db.put(it.key, it.value) }.also { println("index $i put ${it.milliseconds}") }
            }.milliseconds
        }
        println()

        delay(3000)
        println("asyncPerformance -----------")
        val asyncPerformance = async {
            measureTimedValue {
                inputs.entries.withIndex().map { (i, it) ->
                    async {
                        measureTimeMillis {
                            db.put(
                                it.key,
                                it.value
                            )
                        }.also { println("index $i put ${it.milliseconds}") }
                    }
                }.awaitAll().sum().milliseconds
            }
        }.await()
        println()

        println("noCoroutinePerformance $noCoroutinePerformance")
        println("coroutinePerformance $coroutinePerformance")
        println("asyncPerformance $asyncPerformance")
    }
//    val t = Thread {
//        println("start thread")
//
//        println("end thread ${time.milliseconds}")
//        db.close()
//    }.apply { start() }
//    var state = t.state
//    var time = System.currentTimeMillis()
//    var count = 10
//    var ta = System.currentTimeMillis()
//    println("start loop.. with state $state")
//    while (count != 0) {
//        val now = System.currentTimeMillis()
//        if (now - ta >= 500) {
//            println("${t} waiting... ${(now - ta).milliseconds}")
//            ta = now
//        }
//        if (state == t.state) {
////                print(".")
//        } else {
//            println("$state : ${(now - time).milliseconds}")
//            time = now
//            state = t.state
//        }
//        if (!t.isAlive) {
//            println("break ${t.state}")
//            count--
//        }
//    }
}
