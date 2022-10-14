package com.soywiz.kproject

import com.soywiz.kproject.util.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

@Suppress("unused")
class KProjectRootPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.allprojects {
            this.defineStandardRepositories()
        }
    }
}
