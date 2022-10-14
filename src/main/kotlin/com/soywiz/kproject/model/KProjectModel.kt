package com.soywiz.kproject.model

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.core.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.*
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


class KSet(val settings: Settings) {
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
                project.settings.ensureGitSources(project.name, repo, path, version, "src", project.settings.settings.rootDir["modules"])
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

object JSON5 {
    val mapper: ObjectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        .configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true)
        .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        .configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
        .configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true)
        .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
        .configure(JsonParser.Feature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS, true)
}

data class KProject(
    val name: String,
    val version: String = "unknown",
    val type: String? = "library",
    val src: String? = null,
    val dependencies: List<String> = emptyList()
) : KDependency {
    @JsonIgnore lateinit var file: File
    @JsonIgnore lateinit var settings: KSet
    @JsonIgnore var root: Boolean = false

    val projectDir: File get() = file.parentFile

    companion object {
        fun load(file: File, settings: KSet, root: Boolean): KProject {
            return settings.projectMap.getOrPut(file.canonicalFile) {
                JSON5.mapper.readValue<KProject>(file.readText()).also {
                    it.root = root
                    it.file = file.canonicalFile
                    it.settings = settings
                }
            }
        }
    }

    fun resolveDependency(path: String): KDependency {
        val info = path.split("::")
        return when (info.first()) {
            "maven" -> {
                KGradleDependency(info[1], info.last())
            }
            "git" -> {
                val (_, name, repo, path, rel) = info
                val file = settings.ensureGitSources(name, repo, path, rel, "")
                load(File(file, "kproject.json5"), settings, false)
            }
            else -> {
                if (!path.startsWith(".")) error("dependency '$path' unrecognised")
                val file1 = File(file.parentFile, "$path.kproject.json5")
                val file2 = File(file.parentFile, "$path/kproject.json5")
                load(listOf(file1, file2).firstOrNull { it.exists() } ?: error("Can't find suitable kproject.json5: $file1, $file2"), settings, false)
            }
        }
    }

    fun resolveSource(): File {
        val src = when {
            src != null -> src
            file.name == "kproject.json5" -> "./"
            else -> "./${name}"
        }
        return KSource(this, src).resolveDir()
    }

    override val gradleSourceSet = "commonMainApi"
    override val gradleRef = "project(\":${name}\")"

    override fun resolve(settings: Settings) {
        val sourceDirectory = resolveSource()
        //println("resolve: $name -> $file")
        //if (!root) {
        settings.include(":${name}")
        settings.project(":${name}").projectDir = sourceDirectory
        //}
        val deps = dependencies.map { resolveDependency(it).also { it.resolve(settings) } }
        val buildGradleText = buildString {
            //appendLine("configure([project(\":${if (root) "" else name}\")]) {")
            //appendLine("configure([project(\":${name}\")]) {")
            appendLine("buildscript { repositories { mavenLocal(); mavenCentral(); google(); gradlePluginPortal() } }")
            appendLine("plugins {")
            appendLine("  id(\"com.soywiz.kproject\")")
            appendLine("}")
            appendLine("dependencies {")
            for (dep in deps) {
                appendLine("  add(\"${dep.gradleSourceSet}\", ${dep.gradleRef})")
            }
            appendLine("}")
            //appendLine("}")
        }
        //this.settings.addDeps(buildGradleText)
        java.io.File(sourceDirectory, "build.gradle").writeTextIfNew(buildGradleText)
        java.io.File(sourceDirectory, ".gitignore").writeTextIfNew(listOf("./build").joinToString("\n"))
    }
}
