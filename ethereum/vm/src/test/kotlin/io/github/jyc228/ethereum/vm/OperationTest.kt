package io.github.jyc228.ethereum.vm

import io.github.jyc228.ethereum.state.account.Address
import io.github.jyc228.ethereum.state.account.CodeHash
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.resource.resourceAsString
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import java.math.BigInteger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.fail

@OptIn(ExperimentalStdlibApi::class)
class OperationTest : DescribeSpec({
    suspend fun ContainerScope.runTestUsingTestCaseFile(addTc: (MutableList<TestCase>.() -> Unit)? = null) {
        val opcode = OpCode.valueOf(testCase.name.testName)
        val testCases = resourceAsString("/testdata/testcases_${opcode.name.lowercase()}.json")
            .let { Json.decodeFromString<List<TestCase>>(it) }
            .toMutableList()
            .also { addTc?.invoke(it) }
            .onEachIndexed { index, testCase -> testCase.index = index }

        val instructionSet = InstructionSet.fromConfig(EVMConfig())
        val operation = instructionSet[opcode.v] ?: fail("operation not exist. $opcode")
        withData(nameFn = { "${it.index} $it" }, testCases) { tc ->
            val context = EVMFrame(
                contract = EVMContract(Address.fromBytes(), byteArrayOf(), CodeHash(byteArrayOf())),
                callData = byteArrayOf(),
                caller = Address.fromBytes(),
                callValue = BigInteger.ZERO,
                remainGas = 10000000
            )
            context.stack.push(EVMStackElement(_bytes = tc.X.hexToByteArray()))
            context.stack.push(EVMStackElement(_bytes = tc.Y.hexToByteArray()))
            operation.execute(context)
            val result = context.stack.last().bytes.toHexString().trimStart('0')
            result shouldBeEqualIgnoringCase tc.Expected.trimStart('0')
        }
    }

    describe("ADD") {
        runTestUsingTestCaseFile {
            this += TestCase("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffe1", "0240", "0221")
        }
    }
    describe("SUB") {
        runTestUsingTestCaseFile {
            this += TestCase("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "7fffff", "800000")
        }
    }
    describe("MUL") { runTestUsingTestCaseFile() }
    describe("DIV") { runTestUsingTestCaseFile() }
    describe("SDIV") { runTestUsingTestCaseFile() }
    describe("MOD") { runTestUsingTestCaseFile() }
    describe("SMOD") { runTestUsingTestCaseFile() }
    describe("EXP") { runTestUsingTestCaseFile() }
    describe("SIGNEXTEND") { runTestUsingTestCaseFile() }
    describe("LT") { runTestUsingTestCaseFile() }
    describe("GT") { runTestUsingTestCaseFile() }
    describe("SLT") { runTestUsingTestCaseFile() }
    describe("SGT") { runTestUsingTestCaseFile() }
    describe("EQ") { runTestUsingTestCaseFile() }
    describe("AND") { runTestUsingTestCaseFile() }
    describe("OR") { runTestUsingTestCaseFile() }
    describe("XOR") { runTestUsingTestCaseFile() }
    describe("BYTE") {
        runTestUsingTestCaseFile {
            // @formatter:off
            this += TestCase("ABCDEF0908070605040302010000000000000000000000000000000000000000", "00", "AB")
            this += TestCase("ABCDEF0908070605040302010000000000000000000000000000000000000000", "01", "CD")
            this += TestCase("00CDEF090807060504030201ffffffffffffffffffffffffffffffffffffffff", "00", "00")
            this += TestCase("00CDEF090807060504030201ffffffffffffffffffffffffffffffffffffffff", "01", "CD")
            this += TestCase("0000000000000000000000000000000000000000000000000000000000102030", "1F", "30")
            this += TestCase("0000000000000000000000000000000000000000000000000000000000102030", "1E", "20")
            this += TestCase("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "20", "00")
            this += TestCase("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "FFFFFFFFFFFFFFFF", "00")
            // @formatter:on
        }
    }
    describe("SHL") {
        runTestUsingTestCaseFile {
            // @formatter:off
            this += TestCase("0000000000000000000000000000000000000000000000000000000000000001", "01", "0000000000000000000000000000000000000000000000000000000000000002")
            this += TestCase("0000000000000000000000000000000000000000000000000000000000000001", "ff", "8000000000000000000000000000000000000000000000000000000000000000")
            this += TestCase("0000000000000000000000000000000000000000000000000000000000000001", "0100", "0000000000000000000000000000000000000000000000000000000000000000")
            this += TestCase("0000000000000000000000000000000000000000000000000000000000000001", "0101", "0000000000000000000000000000000000000000000000000000000000000000")
            this += TestCase("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "00", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
            this += TestCase("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "01", "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffe")
            this += TestCase("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "ff", "8000000000000000000000000000000000000000000000000000000000000000")
            this += TestCase("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "0100", "0000000000000000000000000000000000000000000000000000000000000000")
            // @formatter:on
        }
    }
    describe("SHR") {
        runTestUsingTestCaseFile {
            // @formatter:off
            this += TestCase("0000000000000000000000000000000000000000000000000000000000000001", "00", "0000000000000000000000000000000000000000000000000000000000000001")
            this += TestCase("0000000000000000000000000000000000000000000000000000000000000001", "01", "0000000000000000000000000000000000000000000000000000000000000000")
            this += TestCase("8000000000000000000000000000000000000000000000000000000000000000", "01", "4000000000000000000000000000000000000000000000000000000000000000")
            this += TestCase("8000000000000000000000000000000000000000000000000000000000000000", "ff", "0000000000000000000000000000000000000000000000000000000000000001")
            this += TestCase("8000000000000000000000000000000000000000000000000000000000000000", "0100", "0000000000000000000000000000000000000000000000000000000000000000")
            this += TestCase("8000000000000000000000000000000000000000000000000000000000000000", "0101", "0000000000000000000000000000000000000000000000000000000000000000")
            this += TestCase("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "00", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
            this += TestCase("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "01", "7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
            // @formatter:on
        }

    }
    describe("SAR") {
        runTestUsingTestCaseFile {
            // @formatter:off
            this += TestCase("0000000000000000000000000000000000000000000000000000000000000001", "00", "0000000000000000000000000000000000000000000000000000000000000001")
            this += TestCase("0000000000000000000000000000000000000000000000000000000000000001", "01", "0000000000000000000000000000000000000000000000000000000000000000")
            this += TestCase("8000000000000000000000000000000000000000000000000000000000000000", "01", "c000000000000000000000000000000000000000000000000000000000000000")
            this += TestCase("8000000000000000000000000000000000000000000000000000000000000000", "ff", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
            this += TestCase("8000000000000000000000000000000000000000000000000000000000000000", "0100", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
            this += TestCase("8000000000000000000000000000000000000000000000000000000000000000", "0101", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
            this += TestCase("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "00", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
            this += TestCase("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "01", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
            this += TestCase("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "ff", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
            this += TestCase("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "0100", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
            this += TestCase("0000000000000000000000000000000000000000000000000000000000000000", "01", "0000000000000000000000000000000000000000000000000000000000000000")
            this += TestCase("4000000000000000000000000000000000000000000000000000000000000000", "fe", "0000000000000000000000000000000000000000000000000000000000000001")
            this += TestCase("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "f8", "000000000000000000000000000000000000000000000000000000000000007f")
            this += TestCase("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "fe", "0000000000000000000000000000000000000000000000000000000000000001")
            this += TestCase("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "ff", "0000000000000000000000000000000000000000000000000000000000000000")
            this += TestCase("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", "0100", "0000000000000000000000000000000000000000000000000000000000000000")
            // @formatter:on
        }
    }
})

@Serializable
private data class TestCase(val X: String, val Y: String, val Expected: String) {
    var index = 0

    override fun toString(): String = "stack[${X}, ${Y}], expected: $Expected"
}