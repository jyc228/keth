plugins {
    kotlin("jvm") version "1.9.21"
}

allprojects {
    apply(plugin = "kotlin")

    group = "io.github.jyc228"

    repositories {
        mavenCentral()
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
