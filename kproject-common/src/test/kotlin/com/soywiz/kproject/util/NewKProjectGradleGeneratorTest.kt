package com.soywiz.kproject.util

import com.soywiz.kproject.model.*
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
            src: "https://github.com/korlibs/kproject.git/samples/demo1/content#08f93c2e49b65d1d8258e4e1408580772558b038"
            dependencies:
            - "https://github.com/korlibs/kproject.git/samples/demo2#95696dd942ebc8db4ee9d9f4835ce12d853ff16f"
            testDependencies:
            - maven::jvm::org.mockito:mockito-core:5.3.1
            - io.mockk:mockk-android:1.13.5::jvm
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
src: "https://github.com/korlibs/kproject.git/samples/demo1/content#08f93c2e49b65d1d8258e4e1408580772558b038"
dependencies:
- "https://github.com/korlibs/kproject.git/samples/demo2#95696dd942ebc8db4ee9d9f4835ce12d853ff16f"
testDependencies:
- maven::jvm::org.mockito:mockito-core:5.3.1
- io.mockk:mockk-android:1.13.5::jvm

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
  add("commonMainApi", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
  add("commonMainApi", project(":mymodule"))
}
File extraGradle = file("build.extra.gradle")
if (extraGradle.exists()) {
    apply from: extraGradle
}


## mymodule/src/hello.txt
world


## mymodule/.gitignore
/src
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
  add("jvmTestApi", "org.mockito:mockito-core:5.3.1")
  add("jvmTestApi", "io.mockk:mockk-android:1.13.5")
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


## modules/Ademo2/.gitarchive
95696dd942ebc8db4ee9d9f4835ce12d853ff16f

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


## modules/Ademo3/.gitarchive
95696dd942ebc8db4ee9d9f4835ce12d853ff16f

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
https://github.com/korlibs/kproject.git/samples/demo1/content#08f93c2e49b65d1d8258e4e1408580772558b038 ::: 08f93c2e49b65d1d8258e4e1408580772558b038:0c0e030c04f1f9fafc0c08f5f006f4fe07fff30608fc07f2f10b03fc07030f05
https://github.com/korlibs/kproject.git/samples/demo2#95696dd942ebc8db4ee9d9f4835ce12d853ff16f ::: 95696dd942ebc8db4ee9d9f4835ce12d853ff16f:f10dfcf9f2f409050d060a0f00f00a0f0cf80b09fbfefa02fe05f70bf104f105
https://github.com/korlibs/kproject.git/samples/demo3.kproject.yml#95696dd942ebc8db4ee9d9f4835ce12d853ff16f ::: 95696dd942ebc8db4ee9d9f4835ce12d853ff16f:0e040e0a0ffaf602050d080501fd05f5f102f80afcfff60500ffff08f606fff6
""".trimIndent(),
            out.joinToString("\n").trim()
                .replace(KProjectVersion.VERSION, "0.0.1-SNAPSHOT")
                .trim()
        )
    }
}
