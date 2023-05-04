package com.soywiz.kproject.util

import com.soywiz.kproject.newmodel.*
import com.soywiz.kproject.version.*
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
                
                ## deps/.gitignore
                /.idea
                /.gradle
                /build
                /build.gradle
                
                ## deps/build.gradle
                buildscript { repositories { mavenLocal(); mavenCentral(); google(); gradlePluginPortal() } }
                plugins {
                  id("com.soywiz.kproject") version "0.0.1-SNAPSHOT"
                  id("org.jetbrains.kotlin.plugin.serialization")
                }
                dependencies {
                  add("commonMainApi", project(":mymodule"))
                  add("commonMainApi", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
                }
                File extraGradle = file("build.extra.gradle")
                if (extraGradle.exists()) {
                    apply from: extraGradle
                }
                
                
                ## mymodule/.gitignore
                /.idea
                /.gradle
                /build
                /build.gradle
                
                ## mymodule/build.gradle
                buildscript { repositories { mavenLocal(); mavenCentral(); google(); gradlePluginPortal() } }
                plugins {
                  id("com.soywiz.kproject") version "0.0.1-SNAPSHOT"
                }
                dependencies {
                  add("commonMainApi", project(":Ademo2"))
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
                
                
                ## modules/Ademo2/.gitignore
                /.idea
                /.gradle
                /build
                /build.gradle
                
                ## modules/Ademo2/build.gradle
                buildscript { repositories { mavenLocal(); mavenCentral(); google(); gradlePluginPortal() } }
                plugins {
                  id("com.soywiz.kproject") version "0.0.1-SNAPSHOT"
                }
                dependencies {
                  add("commonMainApi", project(":Ademo3"))
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
                
                
                ## modules/Ademo3/demo3/.gitignore
                /.idea
                /.gradle
                /build
                /build.gradle
                
                ## modules/Ademo3/demo3/build.gradle
                buildscript { repositories { mavenLocal(); mavenCentral(); google(); gradlePluginPortal() } }
                plugins {
                  id("com.soywiz.kproject") version "0.0.1-SNAPSHOT"
                }
                dependencies {
                  add("commonMainApi", "org.jetbrains.compose.runtime:runtime:1.4.1")
                }
                File extraGradle = file("build.extra.gradle")
                if (extraGradle.exists()) {
                    apply from: extraGradle
                }
                

                ## kproject.lock
                https://github.com/korlibs/kproject.git/samples/demo2#95696dd942ebc8db4ee9d9f4835ce12d853ff16f ::: 95696dd942ebc8db4ee9d9f4835ce12d853ff16f:f10dfcf9f2f409050d060a0f00f00a0f0cf80b09fbfefa02fe05f70bf104f105
                https://github.com/korlibs/kproject.git/samples/demo3.kproject.yml#95696dd942ebc8db4ee9d9f4835ce12d853ff16f ::: 95696dd942ebc8db4ee9d9f4835ce12d853ff16f:0e040e0a0ffaf602050d080501fd05f5f102f80afcfff60500ffff08f606fff6
            """.trimIndent(),
            out.joinToString("\n").trim()
                .replace(KProjectVersion.VERSION, "0.0.1-SNAPSHOT")
        )
    }
}
