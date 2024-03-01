plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.21"
    `kotlin-dsl`
    `java-gradle-plugin`
}

dependencies {
    api(project(":codegen"))
    api(project(":contract:abi"))
    implementation(gradleKotlinDsl())
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.4.0")

    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

gradlePlugin {
    plugins {
        create("solidity-plugin") {
            id = "io.github.jyc228.keth"
            implementationClass = "io.github.jyc228.solidity.SolidityPlugin"
        }
    }
}
