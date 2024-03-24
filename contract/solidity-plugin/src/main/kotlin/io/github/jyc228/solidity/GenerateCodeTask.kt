package io.github.jyc228.solidity

import io.github.jyc228.keth.solidity.ContractGenerator
import io.github.jyc228.keth.solidity.LibraryGenerator
import io.github.jyc228.keth.solidity.compile.CompileResult
import java.io.File
import kotlinx.serialization.json.Json
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

open class GenerateCodeTask : SourceTask() {

    @Internal
    lateinit var solidityRoot: File

    @TaskAction
    fun execute() {
        val genLibraryByFullName = mutableMapOf<String, LibraryGenerator>()
        source.filter { it.extension == "sol" }.forEach { file -> generateContractFromSol(file, genLibraryByFullName) }
        source.filter { it.extension == "abi" }.forEach { file -> generateContractFromAbi(file, genLibraryByFullName) }
        genLibraryByFullName.toList().forEach { (fullName, gen) -> generateLibrary(fullName, gen) }
    }

    private fun generateContractFromSol(
        file: File,
        genLibraryByFullName: MutableMap<String, LibraryGenerator>
    ) {
        val compileOutput = File(outputs.files.singleFile, file.relativeTo(solidityRoot).parent + "/.solc")
        project.exec {
            commandLine("solc", file.absoluteFile, "--bin", "--abi", "-o", compileOutput.absoluteFile)
        }
        val abiFile = File(
            outputs.files.singleFile,
            file.relativeTo(solidityRoot).parent + "/.solc/${file.nameWithoutExtension}.abi"
        )
        generateContractFromAbi(abiFile, genLibraryByFullName)
    }

    private fun generateContractFromAbi(abiFile: File, genLibraryByFullName: MutableMap<String, LibraryGenerator>) {
        val binFile = abiFile.resolveSibling("${abiFile.nameWithoutExtension}.bin")
        val generator = ContractGenerator(
            packagePath = abiFile.relativeTo(solidityRoot).parent.replace("/", "."),
            compileResult = CompileResult(
                abiFile.name.replace(".abi", ""),
                Json.decodeFromString<List<AbiItem>>(abiFile.readText()),
                if (binFile.exists()) binFile.readText() else ""
            )
        )
        val parentDirectory = File(outputs.files.singleFile, generator.packagePath.replace(".", "/"))
        generator.generateInterface().write(parentDirectory)
        generator.generateDefaultImplementation().write(parentDirectory)
        generator.compileResult.topLevelStructures().forEach { io ->
            val struct = io.resolveStruct()
            val gen =
                genLibraryByFullName.getOrPut("${generator.packagePath}._Struct") { LibraryGenerator(generator.packagePath) }
            gen.abiIOByName[struct.name] = io
        }
        generator.compileResult.externalStructures().forEach { io ->
            val struct = io.resolveStruct()
            val gen = genLibraryByFullName.getOrPut("${generator.packagePath}.${struct.ownerName}") {
                LibraryGenerator(generator.packagePath)
            }
            gen.abiIOByName[struct.name] = io
        }
    }

    private fun generateLibrary(fullName: String, gen: LibraryGenerator) {
        val path = fullName.split(".")
        val dir = File(outputs.files.singleFile, path.dropLast(1).joinToString("/"))
        gen.generate(path.last()).write(dir)
    }
}
