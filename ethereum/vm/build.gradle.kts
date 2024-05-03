plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.21"
    `maven-publish`
}

dependencies {
    implementation(project(":ethereum:state"))
    compileOnly(project(":ethereum:rpc-client"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.4.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    testImplementation(project(":ethereum"))
    testImplementation(project(":ethereum:rpc-client"))
}

publishing(createGPRPublisher { artifactId = "vm" })
