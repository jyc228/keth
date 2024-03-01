package ethereum.p2p

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.maps.shouldHaveSize

class ProtocolKtTest : ShouldSpec({
    fun matchProtocols(init: TestMatchProtocol.() -> Unit) = TestMatchProtocol().apply(init).run()

    context("make protocol") {
        should("No remote capabilities") { matchProtocols { local("a") } shouldContainExactly emptyMap() }
        should("No local protocols") { matchProtocols { remote("a") } shouldContainExactly emptyMap() }
        should("No mutual protocols") {
            matchProtocols {
                local("b")
                remote("a")
            } shouldContainExactly emptyMap()
        }
        should("Some matches, some differences") {
            matchProtocols {
                local("match1", "match2", "remote")
                remote("local", "match1", "match2")
            }.keys shouldContainExactly setOf("match1", "match2")
        }
        should("Various alphabetical ordering") {

        }
        should("No mutual versions") {
            matchProtocols {
                local(2)
                remote(1)
            } shouldContainExactly emptyMap()
        }
        should("Multiple versions, single common") {
            val result = matchProtocols {
                local(2, 3)
                remote(1, 2)
            }
            result shouldHaveSize 1
//            result[""]?.metadata?.version shouldBe 2u
        }
        should("Multiple versions, multiple common") {
            val result = matchProtocols {
                local(2, 3)
                remote(1, 2, 3, 4)
            }
            result shouldHaveSize 1
//            result[""]?.metadata?.version shouldBe 3u
        }
        should("Various version orderings") {
            val result = matchProtocols {
                local(2, 3, 1)
                remote(4, 1, 3, 2)
            }
            result shouldHaveSize 1
//            result[""]?.metadata?.version shouldBe 3u
        }
        should("Versions overriding sub-protocol lengths") {

        }
    }
})

private class TestMatchProtocol {
    val local = mutableListOf<Protocol>()
    val remote = mutableListOf<ProtocolId>()
    fun local(vararg names: String) {
//        names.forEach { local += Protocol(name = it, version = 0u, length = 0u) }
    }

    fun local(vararg versions: Int) {
//        versions.forEach { local += Protocol(name = "", version = it.toUInt(), length = 0u) }
    }

    fun remote(vararg names: String) {
        names.forEach { remote += ProtocolId(name = it, version = 0u) }
    }

    fun remote(vararg versions: Int) {
        versions.forEach { remote += ProtocolId(name = "", version = it.toUInt()) }
    }

    fun run() = matchProtocols(local, remote)
}
