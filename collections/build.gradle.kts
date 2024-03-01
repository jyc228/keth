plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":rlp"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-assertions-core:5.5.4")
}
