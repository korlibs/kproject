package com.soywiz.kproject

import com.soywiz.kproject.model.*
import com.soywiz.kproject.util.*
import org.gradle.api.*
import org.gradle.api.plugins.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import java.io.*

@Suppress("unused")
class KProjectPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.defineStandardRepositories()

        project.plugins.applyOnce("kotlin-multiplatform")
        //project.repositories()
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

        val kprojectYml = File(project.projectDir, "kproject.yml")
        val kproject = if (kprojectYml.exists()) KProject.load(kprojectYml, KSet(File("modules")), true) else null

        fun hasTarget(name: String): Boolean = kproject?.hasTarget(name) ?: true

        kotlin.apply {
            metadata()
            if (hasTarget("jvm")) {
                jvm {
                    compilations.all {
                        it.kotlinOptions.jvmTarget = "1.8"
                    }
                    //withJava()
                    testRuns.maybeCreate("test").executionTask.configure {
                        it.useJUnitPlatform()
                    }
                }
            }
            if (hasTarget("js")) {
                js(KotlinJsCompilerType.IR) {
                    browser {
                        commonWebpackConfig {
                            cssSupport {
                                it.enabled.set(true)
                            }
                        }
                    }
                }
            }
            if (hasTarget("desktop")) {
                macosArm64()
                macosX64()
                mingwX64()
                linuxX64()
            }
            if (hasTarget("mobile")) {
                iosX64()
                iosArm64()
                iosSimulatorArm64()
            }
            sourceSets.apply {
                val common = createPair("common")
                common.test.dependencies { implementation(kotlin("test")) }
                val concurrent = createPair("concurrent")
                val jvmAndroid = createPair("jvmAndroid").dependsOn(concurrent)
                if (hasTarget("jvm")) {
                    val jvm = createPair("jvm").dependsOn(jvmAndroid)
                }
                if (hasTarget("js")) {
                    val js = createPair("js")
                }
                val native = createPair("native").dependsOn(concurrent)
                val posix = createPair("posix").dependsOn(native)
                val apple = createPair("apple").dependsOn(posix)
                val macos = createPair("macos").dependsOn(apple)
                if (hasTarget("desktop")) {
                    createPair("macosX64").dependsOn(macos)
                    createPair("macosArm64").dependsOn(macos)
                    val linux = createPair("linux").dependsOn(posix)
                    createPair("linuxX64").dependsOn(linux)
                }
                if (hasTarget("mobile")) {
                    val ios = createPair("ios").dependsOn(apple)
                    createPair("iosArm64").dependsOn(ios)
                    createPair("iosSimulatorArm64").dependsOn(ios)
                }
            }
            //println("KProjectPlugin: $project")
        }

    }

    // SourceSet pairs: main + test
    data class KotlinSourceSetPair(val main: KotlinSourceSet, val test: KotlinSourceSet)
    fun NamedDomainObjectContainer<KotlinSourceSet>.createPair(name: String): KotlinSourceSetPair = KotlinSourceSetPair(maybeCreate("${name}Main"), maybeCreate("${name}Test"))
    fun KotlinSourceSetPair.dependsOn(other: KotlinSourceSetPair): KotlinSourceSetPair {
        this.main.dependsOn(other.main)
        this.test.dependsOn(other.test)
        return this
    }
}

//fun <T : Plugin<*>> PluginContainer.applyOnce(clazz: Class<T>): T = findPlugin(clazz) ?: apply(clazz)
fun <T : Plugin<*>> PluginContainer.applyOnce(clazz: Class<T>): T = apply(clazz)
inline fun <reified T : Plugin<*>> PluginContainer.applyOnce(): T = applyOnce(T::class.java)
fun PluginContainer.applyOnce(id: String) {
    if (!hasPlugin(id)) {
        apply(id)
    }
}
