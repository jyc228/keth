package io.github.jyc228.solidity

import io.github.jyc228.keth.solidity.Abi
import io.github.jyc228.keth.solidity.ContractGenerator
import io.github.jyc228.keth.solidity.LibraryGenerator
import java.io.File
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

open class GenerateCodeTask : SourceTask() {

    @Internal
    lateinit var solidityRoot: File

    @TaskAction
    fun execute() {
        val genLibraryByFullName = mutableMapOf<String, LibraryGenerator>()
        source.forEach { file -> generateContract(file, genLibraryByFullName) }
        genLibraryByFullName.toList().forEach { (fullName, gen) -> generateLibrary(fullName, gen) }
    }

    private fun generateContract(
        abiFile: File,
        genLibraryByFullName: MutableMap<String, LibraryGenerator>
    ) {
        val generator = ContractGenerator(
            packagePath = abiFile.relativeTo(solidityRoot).parent.replace("/", "."),
            abi = Abi.fromFile(abiFile)
        )
        val parentDirectory = File(outputs.files.singleFile, generator.packagePath.replace(".", "/"))
        generator.generateInterface().write(parentDirectory)
        generator.generateDefaultImplementation().write(parentDirectory)
        generator.abi.topLevelStructures().forEach { io ->
            val struct = io.resolveStruct()
            val gen =
                genLibraryByFullName.getOrPut("${generator.packagePath}._Struct") { LibraryGenerator(generator.packagePath) }
            gen.abiIOByName[struct.name] = io
        }
        generator.abi.externalStructures().forEach { io ->
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
