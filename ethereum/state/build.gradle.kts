plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.0.0"
    `maven-publish`
}

repositories {
    gpr("jyc228/keth-client")
}

dependencies {
    implementation(project(":rlp"))
    implementation(project(":collections"))
    compileOnly("com.github.jyc228:keth-client:0.2")

    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
}

publishing(createGPRPublisher { artifactId = "state" })
