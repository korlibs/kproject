plugins {
    kotlin("multiplatform") version "1.7.20"
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }

    apply(plugin = "kotlin-multiplatform")

    kotlin {
        jvm {
            withJava()
        }
    }
}

subprojects {
    println(this)
}
/*
project(":").dependencies {
    add("commonMainApi", project(":korge-dragonbones"))
}*/