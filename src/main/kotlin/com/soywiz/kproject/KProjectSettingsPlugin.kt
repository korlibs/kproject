package com.soywiz.kproject

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

@Suppress("unused")
class KProjectSettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        println("KProjectSettingsPlugin: $settings")
    }
}
