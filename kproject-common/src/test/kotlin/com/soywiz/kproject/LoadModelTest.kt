package com.soywiz.kproject

import com.soywiz.kproject.model.*
import java.io.*
import kotlin.test.*

class LoadModelTest {
    @Test
    fun test() {
        val kset = KSet(File("modules"))
        val file = File("kproject.yml")
        val project = KProject.load(file, kset, root = true, forcedContent = """
            name: korio
            type: library
            version: 3.2.0
            src: ./src
            dependencies:
            - ./libs/kds
            - git::adder::korlibs/kproject::/modules/adder::54f73b01cea9cb2e8368176ac45f2fca948e57db
            - maven::common::org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4
        """.trimIndent())

        assertEquals("korio", project.name)
        assertEquals("library", project.type)
        assertEquals("3.2.0", project.version)
        assertEquals("./src", project.src)
        assertEquals(listOf(
            "./libs/kds",
            "git::adder::korlibs/kproject::/modules/adder::54f73b01cea9cb2e8368176ac45f2fca948e57db",
            "maven::common::org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4",
        ), project.dependencies)
        assertEquals(true, project.root)
        assertEquals(file.canonicalFile, project.file)
        assertEquals(kset, project.settings)
    }
}
