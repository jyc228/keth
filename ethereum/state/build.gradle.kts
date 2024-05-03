plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.21"
    `maven-publish`
}

dependencies {
    implementation(project(":rlp"))
    implementation(project(":collections"))

    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
}

publishing(createGPRPublisher { artifactId = "state" })
