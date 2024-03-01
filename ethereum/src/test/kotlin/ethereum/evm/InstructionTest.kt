package ethereum.evm

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class InstructionTest {
    @Test
    fun `AddMod`() {
        val stack = Stack()
        val interpreter = EVMInterpreter()
        val scopeContext = ScopeContext(stack)
        val x = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff".toBigInteger(16)
        val y = "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffe".toBigInteger(16)
        val z = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff".toBigInteger(16)
        stack.add(z)
        stack.add(y)
        stack.add(x)
//        InstructionSet.AddMod.execute(0, interpreter, scopeContext)
        stack.poll() shouldBe "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffe".toBigInteger(16)
    }
}