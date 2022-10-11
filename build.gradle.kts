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
            //sourceSet.kotlin.srcDir(folder)
            println("sourceSets=${sourceSets.names}")
        }
        js {
            browser()
        }
        for (target in listOf("common", "jvm", "js")) {
            sourceSets["${target}Main"].kotlin.srcDir(file("src/$target/src"))
            sourceSets["${target}Main"].resources.srcDir(file("src/$target/resources"))
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