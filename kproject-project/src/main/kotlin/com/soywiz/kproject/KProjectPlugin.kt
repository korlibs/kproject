package com.soywiz.kproject

import com.soywiz.kproject.model.*
import com.soywiz.kproject.util.*
import org.gradle.api.*
import org.gradle.api.plugins.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*

@Suppress("unused")
class KProjectPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.defineStandardRepositories()

        project.plugins.applyOnce("kotlin-multiplatform")
        //project.repositories()
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

        //val kprojectYml = File(project.projectDir, "kproject.yml")
        //val kproject = if (kprojectYml.exists()) KProject.load(kprojectYml, KSet(File("modules")), true) else null

        // @TODO: Configure
        fun hasTarget(name: KProjectTarget): Boolean {
            if (name.isKotlinNative && isWindowsOrLinuxArm) return false
            //return kproject?.hasTarget(name) ?: true
            return true
        }

        kotlin.apply {
            metadata()
            if (hasTarget(KProjectTarget.JVM)) {
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
            if (hasTarget(KProjectTarget.JS)) {
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
            //println(isWindows)
            //println(isLinux)
            //println(isArm)
            //println(isWindowsOrLinuxArm)
            //for (target in KProjectTarget.values()) {
            //    println("target=$target, has=${hasTarget(target)}")
            //}
            if (hasTarget(KProjectTarget.DESKTOP)) {
                macosArm64()
                macosX64()
                mingwX64()
                linuxX64()
                linuxArm64()
            }
            if (hasTarget(KProjectTarget.MOBILE)) {
                iosX64()
                iosArm64()
                iosSimulatorArm64()
            }
            sourceSets.apply {
                val common = createPair("common")
                common.test.dependencies { implementation(kotlin("test")) }
                val concurrent = createPair("concurrent")
                val jvmAndroid = createPair("jvmAndroid").dependsOn(concurrent)
                if (hasTarget(KProjectTarget.JVM)) {
                    val jvm = createPair("jvm").dependsOn(jvmAndroid)
                }
                if (hasTarget(KProjectTarget.JS)) {
                    val js = createPair("js")
                }
                if (hasTarget(KProjectTarget.DESKTOP) || hasTarget(KProjectTarget.MOBILE)) {
                    val native = createPair("native").dependsOn(concurrent)
                    val posix = createPair("posix").dependsOn(native)
                    val apple = createPair("apple").dependsOn(posix)
                    val macos = createPair("macos").dependsOn(apple)
                    if (hasTarget(KProjectTarget.DESKTOP)) {
                        createPair("macosX64").dependsOn(macos)
                        createPair("macosArm64").dependsOn(macos)
                        val linux = createPair("linux").dependsOn(posix)
                        createPair("linuxX64").dependsOn(linux)
                        createPair("linuxArm64").dependsOn(linux)
                    }
                    if (hasTarget(KProjectTarget.MOBILE)) {
                        val ios = createPair("ios").dependsOn(apple)
                        createPair("iosArm64").dependsOn(ios)
                        createPair("iosSimulatorArm64").dependsOn(ios)
                    }
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
