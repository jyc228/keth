plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.21"
}

dependencies {
    testImplementation("io.kotest:kotest-runner-junit5:5.6.2")
    testImplementation("io.kotest:kotest-runner-junit5:5.6")
    testImplementation("io.kotest:kotest-assertions-core:5.6")
}
