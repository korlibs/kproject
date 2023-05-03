package com.soywiz.kproject.util

import com.soywiz.kproject.newmodel.*
import kotlin.test.*

class NewKProjectResolverTest {
    @Test
    fun testResolver() {
        val files = MemoryFiles().root
        files["demo/kproject.yml"] = """
            dependencies:
              - ../demo2
        """.trimIndent()
        files["demo2/kproject.yml"] = """
            name: Ademo2
            dependencies:
              - ../demo3.kproject.yml
        """.trimIndent()
        files["demo3.kproject.yml"] = """
            name: Ademo3
            dependencies:
              - ../demo
        """.trimIndent()
        val resolver = NewKProjectResolver()
        val mainProject = resolver.load(files["demo/kproject.yml"])
        //println(mainProject)
        /*
        println("---")
        for (dep in mainProject.dependencies) {
            println(resolver.getProjectByDependency(dep))
        }
        println("---")
        for (dep in resolver.getProjectByName("Ademo2").project.dependencies) {
            println(resolver.getProjectByDependency(dep))
        }
        println("---")
        println(resolver.getAllProjects().values.joinToString("\n"))
        */
        assertEquals(listOf("demo", "Ademo2", "Ademo3"), resolver.getProjectNames().toList())
        val paths = resolver.getAllProjects().map {
            it.key to ((it.value.dep as? FileRefDependency?)?.path as? MemoryFileRef?)?.path?.fullPath
        }.toMap()

        assertEquals(mapOf(
            "demo" to "/demo/kproject.yml",
            "Ademo2" to "/demo2/kproject.yml",
            "Ademo3" to "/demo3.kproject.yml",
        ), paths)
    }
}
