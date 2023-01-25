package com.soywiz.kproject.util

import org.gradle.api.*

fun Project.defineStandardRepositories() {
    repositories.apply {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}
