plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

publishing(createGPRPublisher { artifactId = "rlp" })
