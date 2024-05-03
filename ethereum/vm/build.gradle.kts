plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.21"
    `maven-publish`
}

dependencies {
    implementation(project(":ethereum:state"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    testImplementation(project(":ethereum"))
}

publishing(createGPRPublisher { artifactId = "vm" })
