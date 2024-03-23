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
