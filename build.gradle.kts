import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.0"
}

allprojects {
    apply(plugin = "kotlin")

    group = "io.github.jyc228"

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation(kotlin("test"))
        testImplementation("io.kotest:kotest-runner-junit5:5.8.1")
        testImplementation("io.kotest:kotest-runner-junit5:5.8.1")
        testImplementation("io.kotest:kotest-assertions-core:5.8.1")
        testImplementation("io.kotest:kotest-framework-datatest:5.8.1")
    }

    kotlin {
        jvmToolchain(17)
    }

    java {
        withSourcesJar()
        withJavadocJar()
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
}
