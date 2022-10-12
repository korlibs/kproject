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
        sourceSets.maybeCreate("concurrentMain").apply {
            kotlin.srcDir(file("src/concurrentMain/kotlin"))
            kotlin.srcDir(file("src/concurrent/src"))
        }
        sourceSets["jvmMain"].apply {
            kotlin.srcDir(file("src/jvmAndroidMain/kotlin"))
            resources.srcDir(file("src/jvmAndroidMain/resources"))
        }

        sourceSets["concurrentMain"].dependsOn(sourceSets["commonMain"])
        sourceSets["jvmMain"].dependsOn(sourceSets["concurrentMain"])

        for (sourceSet in sourceSets) {
            val optInAnnotations = listOf(
                "kotlin.RequiresOptIn",
                "kotlin.experimental.ExperimentalTypeInference",
                "kotlin.ExperimentalMultiplatform",
                "kotlinx.coroutines.DelicateCoroutinesApi",
                "kotlinx.coroutines.ExperimentalCoroutinesApi",
                "kotlinx.coroutines.ObsoleteCoroutinesApi",
                "kotlinx.coroutines.InternalCoroutinesApi",
                "kotlinx.coroutines.FlowPreview"
            )

            sourceSet.languageSettings {
                optInAnnotations.forEach { optIn(it) }
                progressiveMode = true
            }
        }
    }
}
