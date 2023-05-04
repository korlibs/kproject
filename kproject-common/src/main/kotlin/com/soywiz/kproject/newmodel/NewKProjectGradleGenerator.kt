package com.soywiz.kproject.newmodel

import com.soywiz.kproject.internal.*
import com.soywiz.kproject.util.*
import com.soywiz.kproject.version.*

class NewKProjectGradleGenerator(val projectRootFolder: FileRef) {
    val resolver = NewKProjectResolver()

    data class ResolveDep(val file: FileRef, val folder: FileRef = file.parent())
    data class ProjectRef(val projectName: String, val projectDir: FileRef)

    fun generate(path: String) : List<ProjectRef> {
        resolver.load(projectRootFolder[path])

        val outProjects = arrayListOf<ProjectRef>()
        for (project in resolver.getAllProjects().values) {
            val file = resolveAndGetProjectFileRefFromDependency(project.dep, project.name)

            val buildGradleFile = file.folder[
                when (file.file.name) {
                    "kproject.yml" -> "build.gradle"
                    else -> file.file.name.removeSuffix(".kproject.yml") + "/build.gradle"
                }
            ]

            val proj = project.project
            if (proj != null) {
                outProjects += ProjectRef(project.name, buildGradleFile.parent())
                if (!buildGradleFile.parent()[".gitignore"].exists()) {
                    buildGradleFile.parent()[".gitignore"] = """
                        /.idea
                        /.gradle
                        /build
                        /build.gradle
                    """.trimIndent()
                }
                buildGradleFile.writeText(buildString {
                    appendLine("buildscript { repositories { mavenLocal(); mavenCentral(); google(); gradlePluginPortal() } }")
                    appendLine("plugins {")
                    appendLine("  id(\"com.soywiz.kproject\") version \"${KProjectVersion.VERSION}\"")

                    val gradlePlugins = proj.plugins.filterIsInstance<GradlePlugin>().map { it.name }

                    for (plugin in gradlePlugins) {
                        when (plugin) {
                            "serialization" -> {
                                appendLine("  id(\"org.jetbrains.kotlin.plugin.serialization\")")
                            }

                            else -> {
                                val parts = plugin.split(":", limit = 2)
                                appendLine(buildString {
                                    append("  id(\"${parts.first()}\")")
                                    if (parts.size >= 2) {
                                        append("  version \"${parts.last()}\"")
                                    }
                                })
                            }
                        }
                    }
                    appendLine("}")
                    //for (gradle in this@KProject.gradleNotNull) {
                    //    appendLine(gradle)
                    //}

                    appendLine("dependencies {")
                    for (dep in project.dependencies) {
                        val ddep = dep.dep
                        when (ddep) {
                            is MavenDependency -> {
                                appendLine("  add(\"${ddep.target}MainApi\", ${ddep.coordinates.quoted})")
                            }
                            else -> {
                                appendLine("  add(\"commonMainApi\", project(${":${dep.name}".quoted}))")
                            }
                        }
                    }
                    for (plugin in gradlePlugins) {
                        when (plugin) {
                            "serialization" -> {
                                appendLine("  add(\"commonMainApi\", \"org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0\")")
                            }
                        }
                    }
                    appendLine("}")

                    appendLine("File extraGradle = file(\"build.extra.gradle\")")
                    appendLine("if (extraGradle.exists()) {")
                    appendLine("    apply from: extraGradle")
                    appendLine("}")
                })
            }
            //println("file=$file")
            //println("buildGradleFile=$buildGradleFile")
        }

        return outProjects
    }

    fun resolveAndGetProjectFileRefFromDependency(dependency: Dependency, projectName: String = dependency.projectName): ResolveDep {
        return when (dependency) {
            is FileRefDependency -> {
                if (dependency.path is GitFileRef) {
                    val path = dependency.path
                    return resolveAndGetProjectFileRefFromDependency(GitDependency(projectName, path.git, path.path.pathInfo.fullPath, path.ref), projectName)
                }
                ResolveDep(dependency.path)
            }
            is GitDependency -> {
                //println("projectName=$projectName, dependency=$dependency")
                val targetFolder = projectRootFolder["modules/${projectName}"]
                val pathInfo = dependency.path.pathInfo
                val isFinalFile = pathInfo.name.endsWith("kproject.yml")
                val folder = when {
                    isFinalFile -> pathInfo.parent
                    else -> pathInfo
                }
                val gitWithPathAndRef = dependency.gitWithPathAndRef.copy(path = folder.fullPath)
                //println(gitWithPathAndRef.path)
                unzipTo(targetFolder, gitWithPathAndRef.getContent().zipFile)
                ResolveDep(when {
                    isFinalFile -> targetFolder[pathInfo.name]
                    else -> targetFolder["kproject.yml"]
                })
            }
            else -> TODO()
        }
    }
}
