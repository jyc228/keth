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
    implementation(project(":ethereum:state"))
    implementation(project(":ethereum:hardfork"))
    compileOnly("com.github.jyc228.keth:client:1.0-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.4.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    testImplementation(project(":ethereum"))
    testImplementation("com.github.jyc228.keth:client:1.0-SNAPSHOT")
}

publishing(createGPRPublisher { artifactId = "vm" })
