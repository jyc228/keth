plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.21"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation(project(":rlp"))
    implementation(project(":collections"))
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    // https://mvnrepository.com/artifact/org.rocksdb/rocksdbjni
    implementation("org.rocksdb:rocksdbjni:8.1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.6.2")
    testImplementation("io.kotest:kotest-assertions-core:5.6.2")
}
