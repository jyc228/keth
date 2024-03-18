plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.21"
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    api(project(":codegen"))
    api(project(":contract:abi"))
    implementation(gradleKotlinDsl())
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.4.0")

    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("solidity-plugin") {
            id = "io.github.jyc228.keth"
            implementationClass = "io.github.jyc228.solidity.SolidityPlugin"
        }
    }
}

publishing(createGPRPublisher { artifactId = "solidity-plugin" })
