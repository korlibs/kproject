package com.soywiz.kproject.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.soywiz.kproject.git.*
import com.soywiz.kproject.util.*
import org.gradle.api.initialization.*
import java.io.*

fun normalizePath(path: String): String = File(path).normalize().toString()
fun ensureRepo(repo: String): String = normalizePath(repo).also { if (it.count { it == '/' } != 1) error("Invalid repo '$repo'") }
fun getKProjectDir(): File = File("${System.getProperty("user.home")}/.kproject").also { it.mkdirs() }
operator fun File.get(path: String): File = File(this, path)
fun File.execToString(vararg params: String, throwOnError: Boolean = true): String {
    val result = Runtime.getRuntime().exec(params, arrayOf(), this)
    val stdout = result.inputStream.readBytes().toString(Charsets.UTF_8)
    val stderr = result.errorStream.readBytes().toString(Charsets.UTF_8)
    if (result.exitValue() != 0 && throwOnError) error("$stdout$stderr")
    return stdout + stderr
}


class KSet(val modulesDir: File) {
    constructor(settings: Settings) : this(settings.rootDir["modules"])

    val projectMap by lazy { LinkedHashMap<File, KProject>() }
    val kproject by lazy { getKProjectDir() }
    val depTexts = arrayListOf<String>()
    fun addDeps(text: String) {
        depTexts.add(text)
    }
    fun ensureGitSources(projectName: String, repo: String, path: String, rel: String, subfolder: String, modulesDirectory: File? = null): File {
        val repo = ensureRepo(repo)
        val outputCheckoutDir = when {
            modulesDirectory != null -> modulesDirectory[projectName]
            else -> kproject["modules/$repo/__checkouts__/${rel}/$projectName"]
        }
        outputCheckoutDir.mkdirs()
        val paramsFile = outputCheckoutDir[".params"]
        val paramsStr = "$projectName::$repo::$path::$rel"

        if (paramsFile.takeIf { it.exists() }?.readText() != paramsStr) {
            //println("*******************")
            outputCheckoutDir.deleteRecursively()
            paramsFile.parentFile.mkdirs()
            paramsFile.writeText(paramsStr)
        }
        //println("---------------")

        return getCachedGitCheckout(projectName, repo, path, rel, subfolder, outputCheckoutDir)
    }
}

class KSource(
    val project: KProject,
    val parts: List<String>,
) {
    val def = parts.joinToString("::")
    constructor(project: KProject, def: String) : this(project, def.split("::"))

    fun resolveDir(): File {
        return when (parts.first()) {
            "git" -> {
                val (_, repo, path, version) = parts
                //File(kProj.kproject, "modules/korge-dragonbones/v3.2.0")
                project.settings.ensureGitSources(project.rname, repo, path, version, "src", project.settings.modulesDir)
            }
            "bundled" -> {
                //parts.last()
                TODO()
            }
            "maven" -> {
                TODO()
            }
            else -> {
                if (!def.startsWith(".")) error("Unknown definition '$def'")

                //project.file.parentFile[def].absoluteFile
                //println("this.project.projectDir=${this.project.projectDir}, ${this.project.file}")
                //println("def=${def}")
                File(this.project.projectDir, def).canonicalFile
            }
        }
    }
}

interface KDependency {
    val gradleSourceSet: String
    val gradleRef: String
    fun resolve(settings: Settings)
}

data class KGradleDependency(
    val type: String,
    val coordinates: String
) : KDependency {
    override val gradleSourceSet = "${type}MainApi"
    override val gradleRef: String get() = "\"" + coordinates + "\""

    override fun resolve(settings: Settings) {
    }
}

data class KProject(
    val name: String? = null,
    val version: String = "unknown",
    val type: String? = "library",
    val src: String? = null,
    val plugins: List<String> = emptyList(),
    val gradle: List<String> = emptyList(),
    val dependencies: List<String> = emptyList(),
    val targets: Set<String> = setOf("all"),
) : KDependency {
    fun hasTarget(target: String): Boolean {
        if ("all" in this.targets) return true
        return target in targets
    }

    @JsonIgnore internal lateinit var file: File
    @JsonIgnore internal lateinit var settings: KSet
    @JsonIgnore internal var root: Boolean = false

    val rname: String get() = name ?: file.name.substringBefore('.').takeIf { it.isNotEmpty() } ?: file.parentFile.name

    val projectDir: File get() = file.parentFile

    companion object {

    }

    fun resolveDependency(path: String): KDependency {
        val info = path.split("::")
        return when (info.first()) {
            "maven" -> {
                KGradleDependency(info[1], info.last())
            }
            "git" -> {
                val (_, name, repo, path, rel) = info
                val file = settings.ensureGitSources(name, repo, path, rel, "", settings.modulesDir)
                load(File(file, "kproject.yml"), settings, false)
            }
            else -> {
                if (!path.startsWith(".")) error("dependency '$path' unrecognised")
                val file1 = File(file.parentFile, "$path.kproject.yml")
                val file2 = File(file.parentFile, "$path/kproject.yml")
                load(listOf(file1, file2).firstOrNull { it.exists() } ?: error("Can't find suitable kproject.yml: $file1, $file2"), settings, false)
            }
        }
    }

    fun resolveSource(): File {
        val src = when {
            src != null -> src
            file.name == "kproject.yml" -> "./"
            else -> "./${rname}"
        }
        return KSource(this, src).resolveDir()
    }

    override val gradleSourceSet get() = "commonMainApi"
    override val gradleRef get() = "project(\":${rname}\")"

    override fun resolve(settings: Settings) {
        val sourceDirectory = resolveSource()
        //println("resolve: $name -> $file")
        //if (!root) {
        settings.include(":${rname}")
        settings.project(":${rname}").projectDir = sourceDirectory
        //}
        val deps = dependencies.map { resolveDependency(it).also { it.resolve(settings) } }
        val buildGradleText = buildString {
            //appendLine("configure([project(\":${if (root) "" else name}\")]) {")
            //appendLine("configure([project(\":${name}\")]) {")
            appendLine("buildscript { repositories { mavenLocal(); mavenCentral(); google(); gradlePluginPortal() } }")
            when (this@KProject.type) {
                "dependencies" -> {
                }
                "library" -> {
                }
                else -> {
                }
            }
            appendLine("plugins {")
            appendLine("  id(\"com.soywiz.kproject\")")
            for (plugin in this@KProject.plugins) {
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
            for (gradle in this@KProject.gradle) {
                appendLine(gradle)
            }

            appendLine("dependencies {")
            for (dep in deps) {
                appendLine("  add(\"${dep.gradleSourceSet}\", ${dep.gradleRef})")
            }
            for (plugin in this@KProject.plugins) {
                when (plugin) {
                    "serialization" -> {
                        appendLine("  add(\"commonMainApi\", \"org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0\")")
                    }
                }
            }
            appendLine("}")
            //appendLine("}")
        }
        //this.settings.addDeps(buildGradleText)
        java.io.File(sourceDirectory, "build.gradle").writeTextIfNew(buildGradleText)
        java.io.File(sourceDirectory, ".gitignore").writeTextIfNew(listOf("build").joinToString("\n"))
    }
}
