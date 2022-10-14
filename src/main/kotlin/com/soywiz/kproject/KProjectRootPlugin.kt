package com.soywiz.kproject

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

@Suppress("unused")
class KProjectRootPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.allprojects {
            repositories {
                mavenLocal()
                mavenCentral()
                google()
                gradlePluginPortal()
            }
        }
    }
}
