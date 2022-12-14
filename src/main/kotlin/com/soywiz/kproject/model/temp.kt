package com.soywiz.kproject.model

/*

val projectFile = File(rootDir, "kproject.yml").also {
    if (!it.exists()) it.writeText(
        """
            {
                // Name of the project
                name: "untitled",
                version: "unknown",
                // Dependency list, to other kproject.yml modules or maven
                // Examples:
                // - "./libs/krypto",
                // - "maven::common::org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4"
                dependencies: [
                ],
            }
        """.trimIndent()
    )
}

File(rootDir, "gradle/.gitignore").writeTextIfNew(
"""
    build.gradle*
    deps.gradle*
    *.settings.gradle.kts
""".trimIndent()
)

fun kotlinSource(@Language("kotlin") source: String): String {
    return source
}

val rootBuildGradleKtsFile = File(rootDir, "gradle/build.gradle.kts")
//val rootBuildGradleKtsFile = File(rootDir, "build.gradle.kts")
rootBuildGradleKtsFile.writeTextIfNew(kotlinSource(
//language=kotlin
"""
    // AUTOGENERATED: DO NOT MODIFY
    plugins {
        kotlin("multiplatform") version "1.7.20" apply true
    }

    allprojects {
        repositories {
            mavenLocal()
            mavenCentral()
            google()
        }

        //plugins { kotlin("multiplatform") }

        apply(plugin = "kotlin-multiplatform")

        kotlin {
            metadata {
                compilations.all {
                    kotlinOptions.suppressWarnings = true
                }
            }
            jvm {
                compilations.all {
                    kotlinOptions.jvmTarget = "1.8"
                    kotlinOptions.suppressWarnings = true
                    kotlinOptions.freeCompilerArgs = listOf("-Xno-param-assertions")
                }
            }
            js(org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.IR) {
                browser {
                    compilations.all {
                        //kotlinOptions.sourceMap = true
                        kotlinOptions.suppressWarnings = true
                    }
                }
            }

            sourceSets {
                val commonMain by getting {}
                val commonTest by getting {}
                val concurrentMain by creating { dependsOn(commonMain) }
                val concurrentTest by creating { dependsOn(commonTest) }
                //val nativeMain by getting
                //val nativeTest by getting
                val jvmAndroidMain by creating { dependsOn(concurrentMain) }
                val jvmAndroidTest by creating { dependsOn(concurrentTest) }
                val jvmMain by getting { dependsOn(jvmAndroidMain) }
                val jvmTest by getting {
                    dependsOn(jvmAndroidTest)
                    dependencies {
                        implementation(kotlin("test"))
                    }
                }
                val jsMain by getting
                val jsTest by getting {
                    dependencies {
                        implementation(kotlin("test"))
                    }
                }
            }

            //for (sourceSet in sourceSets) {
            //    val optInAnnotations = listOf(
            //        "kotlin.RequiresOptIn",
            //        "kotlin.experimental.ExperimentalTypeInference",
            //        "kotlin.ExperimentalMultiplatform",
            //        "kotlinx.coroutines.DelicateCoroutinesApi",
            //        "kotlinx.coroutines.ExperimentalCoroutinesApi",
            //        "kotlinx.coroutines.ObsoleteCoroutinesApi",
            //        "kotlinx.coroutines.InternalCoroutinesApi",
            //        "kotlinx.coroutines.FlowPreview"
            //    )
            //    sourceSet.languageSettings {
            //        optInAnnotations.forEach { optIn(it) }
            //        progressiveMode = true
            //    }
            //}
        }
    }

    File(rootDir, "build.extra.gradle").takeIf { it.exists() }?.let { apply(from = it) }
    File(rootDir, "build.extra.gradle.kts").takeIf { it.exists() }?.let { apply(from = it) }
    File(rootDir, "gradle/deps.gradle").takeIf { it.exists() }?.let { apply(from = it) }
    File(rootDir, "gradle/deps.gradle.kts").takeIf { it.exists() }?.let { apply(from = it) }
""".trimIndent()))

File(rootDir, ".editorconfig").takeIf { !it.exists() }?.writeText("""
    [*]
    charset=utf-8
    end_of_line=lf
    insert_final_newline=true
    indent_style=space
    indent_size=4
    ij_kotlin_name_count_to_use_star_import = 1
    ij_kotlin_blank_lines_before_declaration_with_comment_or_annotation_on_separate_line = 0
    ij_kotlin_variable_annotation_wrap = off

    [*.json,*.json5]
    indent_size=2

    [*.yml]
    indent_size = 2
""".trimIndent())

File(rootDir, ".gitignore").takeIf { !it.exists() }?.writeText("""
    /.idea
    /.gradle
    /build
""".trimIndent())

KSet().also { kProj ->
    KProject.load(projectFile, kProj, true).resolve(settings)

    File(settings.rootDir, "gradle/deps.gradle").writeText("// WARNING!! AUTO GENERATED, DO NOT MODIFY BY HAND!\n\n" + kProj.depTexts.joinToString("\n"))
//settings.project(":").buildFileName = "gradle/build.gradle.kts"
    //settings.project(":").projectDir = File(settings.rootDir, "gradle/root")
    settings.project(":").buildFileName = rootBuildGradleKtsFile.relativeTo(rootDir).toString()
}

/*
pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}
/*
buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        google()
    }

    //dependencies {
    //    classpath("com.soywiz.korlibs.korge.plugins:korge-gradle-plugin:2.0.0.999")
    //}
}

 */

//apply(plugin = "com.soywiz.korge.settings")

plugins {
    //alias(libs.plugins.korge)
    id("com.soywiz.korge.settings") version "2.0.0.999"
}

include()
*/

 */
