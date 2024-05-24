plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.0.0"
    `maven-publish`
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.4.0")
}

publishing(createGPRPublisher { artifactId = "contract-abi" })
