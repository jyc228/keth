plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.0.0"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation(project(":rlp"))
    implementation(project(":collections"))
    implementation(project(":ethereum:type"))
    implementation(project(":ethereum:state"))
    implementation(project(":ethereum:hardfork"))
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    // https://mvnrepository.com/artifact/org.rocksdb/rocksdbjni
    implementation("org.rocksdb:rocksdbjni:8.1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
}
