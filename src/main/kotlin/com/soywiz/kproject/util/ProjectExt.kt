package com.soywiz.kproject.util

import org.gradle.api.*
import org.gradle.kotlin.dsl.*

fun Project.defineStandardRepositories() {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}
