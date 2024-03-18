plugins {
    kotlin("jvm") version "1.9.21"
}

allprojects {
    apply(plugin = "kotlin")

    group = "io.github.jyc228"

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation(kotlin("test"))
        testImplementation("io.kotest:kotest-runner-junit5:5.6.2")
        testImplementation("io.kotest:kotest-runner-junit5:5.6")
        testImplementation("io.kotest:kotest-assertions-core:5.6")
    }

    kotlin {
        jvmToolchain(17)
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
}
