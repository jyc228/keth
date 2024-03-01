package io.github.jyc228.solidity

import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SolidityPluginTest {
    @TempDir
    lateinit var testProjectDir: File

    @Test
    fun testX() {
        File(testProjectDir, "build.gradle.kts").writeText(buildFileContent())
        val testSoliditySrcDir = File(testProjectDir, "src/main/solidity").apply { mkdirs() }
        val testInputAbiFileDir = File(testSoliditySrcDir, "com/test/contract").apply { mkdirs() }
//        val testInputFiles = File("src/test/solidity").listFiles()!!
        val testInputFiles = listOf(File("src/test/solidity/Colosseum.json"))
        testInputFiles.forEach { it.copyTo(File(testInputAbiFileDir, it.name)) }

        val buildResult = GradleRunner.create()
            .withDebug(true)
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
//            .withArguments("GenerateContractWrapper")
            .withArguments("generateKotlinContractWrapper")
            .build()

        println(buildResult.output)

        val result = testProjectDir.walkTopDown()
            .filter { it.name.endsWith(".kt") }
            .toList()
        println(result)
    }

    private fun buildFileContent() = """
       plugins {
            kotlin("jvm") version "1.9.20"
            id("io.github.jyc228.solidity") version "1.0-SNAPSHOT"
        }
    """
}