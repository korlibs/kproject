package com.soywiz.kproject

import com.soywiz.kproject.util.*
import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

@Suppress("unused")
class KProjectPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.defineStandardRepositories()

        project.plugins.applyOnce("kotlin-multiplatform")
        //project.repositories()
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        kotlin.apply {
            metadata()
            jvm {
                compilations.all {
                    kotlinOptions.jvmTarget = "1.8"
                }
                //withJava()
                testRuns["test"].executionTask.configure {
                    useJUnitPlatform()
                }
            }
            js(KotlinJsCompilerType.IR) {
                browser {
                    commonWebpackConfig {
                        cssSupport.enabled = true
                    }
                }
            }
            macosArm64()
            macosX64()
            mingwX64()
            linuxX64()
            sourceSets {
                val common = createPair("common")
                common.test.dependencies { implementation(kotlin("test")) }
                val concurrent = createPair("concurrent")
                createPair("jvm").dependsOn(concurrent)
                createPair("js")
                val native = createPair("native").dependsOn(concurrent)
                val macos = createPair("macos")
                createPair("macosX64").dependsOn(macos)
                createPair("macosArm64").dependsOn(macos)
            }
            println("KProjectPlugin: $project")
        }

    }

    // SourceSet pairs: main + test
    data class KotlinSourceSetPair(val main: KotlinSourceSet, val test: KotlinSourceSet)
    fun NamedDomainObjectContainer<KotlinSourceSet>.createPair(name: String): KotlinSourceSetPair = KotlinSourceSetPair(maybeCreate("${name}Main"), maybeCreate("${name}Test"))
    fun KotlinSourceSetPair.dependsOn(other: KotlinSourceSetPair) {
        this.main.dependsOn(other.main)
        this.test.dependsOn(other.test)
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
