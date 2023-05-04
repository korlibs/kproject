package com.soywiz.kproject.util

import com.soywiz.kproject.newmodel.*
import kotlin.test.*

class NewKProjectGradleGeneratorTest {
    @Test
    fun test() {
        val files = MemoryFiles().root

        files["/deps.kproject.yml"] = """
            plugins:
            - serialization
            dependencies:
            - ./mymodule
        """.trimIndent()

        files["/mymodule/kproject.yml"] = """
            dependencies:
            - "https://github.com/korlibs/kproject.git/samples/demo2#95696dd942ebc8db4ee9d9f4835ce12d853ff16f"
        """.trimIndent()

        NewKProjectGradleGenerator(files)
            .generate("/deps.kproject.yml")

        files["/modules"]

        val out = arrayListOf<String>()
        for ((fileName, _) in files.files.map) {
            val content = files[fileName].readText()
            out += "## $fileName"
            out += content
            out += ""
        }
        assertEquals(
            """
                ## deps.kproject.yml
                plugins:
                - serialization
                dependencies:
                - ./mymodule

                ## mymodule/kproject.yml
                dependencies:
                - "https://github.com/korlibs/kproject.git/samples/demo2#95696dd942ebc8db4ee9d9f4835ce12d853ff16f"

                ## deps.build.gradle
                buildscript { repositories { mavenLocal(); mavenCentral(); google(); gradlePluginPortal() } }
                plugins {
                  id("com.soywiz.kproject") version "0.0.1-SNAPSHOT"
                  id("org.jetbrains.kotlin.plugin.serialization")
                }
                dependencies {
                  add("commonMain", ":mymodule")
                  add("commonMainApi", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
                }
                File extraGradle = file("build.extra.gradle")
                if (extraGradle.exists()) {
                    apply from: extraGradle
                }


                ## mymodule/build.gradle
                buildscript { repositories { mavenLocal(); mavenCentral(); google(); gradlePluginPortal() } }
                plugins {
                  id("com.soywiz.kproject") version "0.0.1-SNAPSHOT"
                }
                dependencies {
                  add("commonMain", ":Ademo2")
                }
                File extraGradle = file("build.extra.gradle")
                if (extraGradle.exists()) {
                    apply from: extraGradle
                }


                ## modules/Ademo2/hello.txt
                hello1


                ## modules/Ademo2/kproject.yml
                name: Ademo2
                dependencies:
                - ../demo3.kproject.yml


                ## modules/Ademo2/build.gradle
                buildscript { repositories { mavenLocal(); mavenCentral(); google(); gradlePluginPortal() } }
                plugins {
                  id("com.soywiz.kproject") version "0.0.1-SNAPSHOT"
                }
                dependencies {
                  add("commonMain", ":Ademo3")
                }
                File extraGradle = file("build.extra.gradle")
                if (extraGradle.exists()) {
                    apply from: extraGradle
                }


                ## modules/Ademo3/demo1/hello.txt
                hello1


                ## modules/Ademo3/demo1/kproject.yml
                name: demo1
                dependencies:
                - ../demo2


                ## modules/Ademo3/demo2/hello.txt
                hello1


                ## modules/Ademo3/demo2/kproject.yml
                name: Ademo2
                dependencies:
                - ../demo3.kproject.yml


                ## modules/Ademo3/demo3.kproject.yml
                name: Ademo3
                dependencies:
                - maven::common::org.jetbrains.compose.runtime:runtime:1.4.1


                ## modules/Ademo3/demo3.build.gradle
                buildscript { repositories { mavenLocal(); mavenCentral(); google(); gradlePluginPortal() } }
                plugins {
                  id("com.soywiz.kproject") version "0.0.1-SNAPSHOT"
                }
                dependencies {
                  add("commonMain", "org.jetbrains.compose.runtime:runtime:1.4.1")
                }
                File extraGradle = file("build.extra.gradle")
                if (extraGradle.exists()) {
                    apply from: extraGradle
                }
            """.trimIndent(),
            out.joinToString("\n").trim()
        )
    }
}
