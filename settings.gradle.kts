import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.module.kotlin.*

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        //classpath("com.soywiz.korlibs.korio:korio-jvm:3.2.0")
        classpath("com.fasterxml.jackson.core:jackson-databind:2.13.4")
        classpath("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")
    }
}

rootProject.name = "kproject"

class GIT(val vfs: java.io.File) {
    companion object {
        fun ensureGitRepo(repo: String): String = when {
            repo.contains("://") || repo.contains("git@") || repo.contains(":") -> repo
            else -> "https://github.com/${java.io.File(repo).normalize()}.git"
        }
    }

    fun downloadArchiveSubfolders(repo: String, path: String, rel: String, vfs: java.io.File = this.vfs) {
        vfs.mkdirs()
        if (vfs.list().isNullOrEmpty()) {
            println(vfs.execToString("git", "clone", "--branch", rel, "--depth", "1", "--filter=blob:none", "--sparse", ensureGitRepo(repo), "."))
            println(vfs.execToString("git", "sparse-checkout", "set", java.io.File(path).normalize().toString().removePrefix("/")))
        }
        vfs.delete()
    }
}

class KSet {
    val projectMap by lazy { LinkedHashMap<java.io.File, KProject>() }
    val kproject by lazy { java.io.File("${System.getProperty("user.home")}/.kproject").also { it.mkdirs() } }
    fun ensureGitSources(projectName: String, repo: String, path: String, rel: String): java.io.File {
        val basePath = "modules/$projectName/${rel}"
        val folder = kproject[basePath].also { it.mkdirs() }
        val outSrc = folder["src"]
        val paramsFile = folder[".params"]
        val paramsStr = "$projectName::$repo::$path::$rel"

        if (paramsFile.takeIf { it.exists() }?.readText() != paramsStr) {
            //println("*******************")
            outSrc.deleteRecursively()
            paramsFile.parentFile.mkdirs()
            paramsFile.writeText(paramsStr)
        }
        //println("---------------")

        if (!outSrc.isDirectory) {
            val git = GIT(kproject["__temp_git_${System.nanoTime()}"].also { it.mkdirs() })
            try {
                git.downloadArchiveSubfolders(repo, path, rel)
                val finalFolder = git.vfs[path]
                outSrc.parentFile.mkdirs()
                finalFolder.renameTo(outSrc)
                println("finalFolder=$finalFolder -> outSrc=$outSrc")
            } finally {
                git.vfs.deleteRecursively()
            }
        }
        return folder
    }
}

fun java.io.File.execToString(vararg params: String): String {
    return java.lang.Runtime.getRuntime().exec(params, kotlin.arrayOf(), this).inputStream.readBytes().toString(Charsets.UTF_8)
}

operator fun java.io.File.get(path: String): java.io.File = java.io.File(this, path)

class KSource(
    val project: KProject,
    val parts: List<String>,
) {
    val def = parts.joinToString("::")
    constructor(project: KProject, def: String) : this(project, def.split("::"))

    fun resolveDir(): java.io.File {
        return when (parts.first()) {
            "git" -> {
                val (_, repo, path, version) = parts
                //File(kProj.kproject, "modules/korge-dragonbones/v3.2.0")
                project.settings.ensureGitSources(project.name, repo, path, version)
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
                println("this.project.projectDir=${this.project.projectDir}, ${this.project.file}")
                println("def=${def}")
                java.io.File(this.project.projectDir, def).canonicalFile
            }
        }
    }
}
/*
class KDependency(
    val parts: List<String>,
    val kproj: KSet
) {
    constructor(def: String, kproj: KSet) : this(def.split("::"), kproj)

    fun resolveDir(): File {
        return when (parts.first()) {
            "git" -> {
                val (_, repo, path, version) = parts

                File(kproj.kproject, "modules/korge-dragonbones/v3.2.0")
            }
            else ->  TODO()
        }
    }
}

 */

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
    val name: String,
    val version: String = "unknown",
    val type: String? = "library",
    val src: String? = null,
    val dependencies: List<String> = emptyList()
) : KDependency {
    @JsonIgnore lateinit var file: java.io.File
    @JsonIgnore lateinit var settings: KSet

    val projectDir: java.io.File get() = file.parentFile

    companion object {
        private val mapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true)
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
            .configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true)
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            .configure(JsonParser.Feature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS, true)

        fun load(file: java.io.File, settings: KSet): KProject {
            return settings.projectMap.getOrPut(file.canonicalFile) {
                mapper.readValue<KProject>(file.readText()).also {
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
            else -> {
                if (!path.startsWith(".")) error("dependency '$path' unrecognised")
                val file1 = java.io.File(file.parentFile, "$path.kproject.json5")
                val file2 = java.io.File(file.parentFile, "$path/kproject.json5")
                load(listOf(file1, file2).firstOrNull { it.exists() } ?: error("Can't find suitable kproject.json5: $file1, $file2"), settings)
            }
        }
    }

    fun resolveSource(): java.io.File {
        val src = src ?: "./src"
        return KSource(this, src).resolveDir()
    }

    override val gradleSourceSet = "commonMainApi"
    override val gradleRef = "project(\":${name}\")"

    override fun resolve(settings: Settings) {
        val file = resolveSource()
        println("resolve: $name -> $file")
        settings.include(":${name}")
        settings.project(":${name}").projectDir = file
        val deps = dependencies.map { resolveDependency(it).also { it.resolve(settings) } }
        val buildGradleText = buildString {
            appendLine("// WARNING!! AUTO GENERATED, DO NOT MODIFY BY HAND!")
            appendLine("dependencies {")
            for (dep in deps) {
                appendLine("  add(\"${dep.gradleSourceSet}\", ${dep.gradleRef})")
            }
            appendLine("}")
        }
        val buildGradleFile = java.io.File(file, "build.gradle")
        if (buildGradleFile.takeIf { it.exists() }?.readText() != buildGradleText) {
            buildGradleFile.writeText(buildGradleText)
        }
    }
}


val projectFile = File(rootDir, "kproject.json5")
if (!projectFile.exists()) {
    projectFile.writeText("{\"name\": \"${projectFile.parentFile.name}\"}")
}
val kProj = KSet()
val project = KProject.load(projectFile, kProj)
println(project)
println("kproject=$project")
//kProj.ensureGitSources(project.name, "korlibs/korge", "korge-dragonbones/src", "v3.2.0")
project.resolve(settings)

project(":").buildFileName = "gradle/build.gradle.kts"
println(project(":").buildFile)