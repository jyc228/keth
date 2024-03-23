plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.21"
    `maven-publish`
}

dependencies {
    api(project(":codegen"))
    api(project(":contract:abi"))
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.4.0")
}

publishing(createGPRPublisher { artifactId = "contract-generator" })
