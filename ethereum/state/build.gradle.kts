plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.0.0"
    `maven-publish`
}

dependencies {
    implementation(project(":rlp"))
    implementation(project(":collections"))
    compileOnly(project(":ethereum:rpc-client"))

    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
}

publishing(createGPRPublisher { artifactId = "state" })
