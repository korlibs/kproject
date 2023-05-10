package com.soywiz.kproject.model

import com.soywiz.kproject.internal.*
import kotlin.test.*

class NewKProjectModelTest {
    @Test
    fun test() {
        val project = NewKProjectModel.parseObject(Yaml.decode("""
            name: "korge-compose"
            #targets: [jvm, desktop]
            #targets: [jvm, js, desktop, ios]
            #targets: [jvm, js, desktop]
            #targets: [all]
            #targets: [jvm, desktop]
            plugins:
              #- com.soywiz.korge
              - org.jetbrains.compose
            dependencies:
              # https://github.com/JetBrains/compose-jb/releases/tag/v1.3.0
              # https://androidx.dev/storage/compose-compiler/repository
              # https://github.com/JetBrains/compose-jb/issues/2108#issuecomment-1157978869
              #- "maven::common::com.soywiz.korlibs.korge2:korge"
              #- "maven::common::org.jetbrains.compose.runtime:runtime:1.3.3"
              #- "maven::common::com.soywiz.korlibs.korge2:korge:4.0.0-alpha-2"
              #- "maven::common::org.jetbrains.compose.runtime:runtime:1.3.0"
              - "maven::common::com.soywiz.korlibs.korge2:korge"
              - "maven::common::org.jetbrains.compose.runtime:runtime:1.4.0"
        """.trimIndent()))

        assertEquals("korge-compose", project.name)
        assertEquals("GradlePlugin(name=org.jetbrains.compose)", project.plugins.joinToString(",") { it.toString() })
        assertEquals("MavenDependency(group=com.soywiz.korlibs.korge2, name=korge, version=, target=common),MavenDependency(group=org.jetbrains.compose.runtime, name=runtime, version=1.4.0, target=common)", project.dependencies.joinToString(",") { it.toString() })
    }

    @Test
    fun testEdgeCases() {
        assertEquals(emptyList(), NewKProjectModel.parseObject(Yaml.decode("""
            dependencies:
        """.trimIndent())).dependencies)
        assertEquals(emptyList(), NewKProjectModel.parseObject(Yaml.decode("dependencies:")).dependencies)
        assertEquals(emptyList(), NewKProjectModel.parseObject(Yaml.decode("")).dependencies)
    }
}
