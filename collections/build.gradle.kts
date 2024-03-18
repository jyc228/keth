plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":rlp"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
}
