plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.21"
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    implementation(project(":contract:generator"))
    implementation(gradleKotlinDsl())
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

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
