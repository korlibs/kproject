import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.*
import javax.print.DocFlavor.STRING

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

//apply(from = "settings.extra.gradle.kts")

class GIT(val vfs: File) {
    companion object {
        fun ensureGitRepo(repo: String): String = when {
            repo.contains("://") || repo.contains("git@") || repo.contains(":") -> repo
            else -> "https://github.com/${File(repo).normalize()}.git"
        }
    }

    fun downloadArchiveSubfolders(repo: String, path: String, rel: String, vfs: File = this.vfs) {
        vfs.mkdirs()
        if (vfs.list().isNullOrEmpty()) {
            println(vfs.execToString("git", "clone", "--branch", rel, "--depth", "1", "--filter=blob:none", "--sparse", ensureGitRepo(repo), "."))
            println(vfs.execToString("git", "sparse-checkout", "set", File(path).normalize().toString().removePrefix("/")))
        }
        vfs.delete()
    }
}

class KSet {
    val projectMap by lazy { LinkedHashMap<File, KProject>() }
    val kproject by lazy { File("${System.getProperty("user.home")}/.kproject").also { it.mkdirs() } }
    fun ensureGitSources(projectName: String, repo: String, path: String, rel: String): File {
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

fun File.execToString(vararg params: String): String {
    return Runtime.getRuntime().exec(params, arrayOf(), this).inputStream.readBytes().toString(Charsets.UTF_8)
}

operator fun File.get(path: String): File = File(this, path)

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
                project.settings.ensureGitSources(project.name, repo, path, version)
            }
            "maven" -> {
                TODO()
            }
            else -> {
                if (!def.startsWith(".")) error("Unknown definition '$def'")
                File(def)
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
    @JsonIgnore lateinit var file: File
    @JsonIgnore lateinit var settings: KSet

    companion object {
        private val mapper = jacksonObjectMapper().configure(
            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false
        )

        fun load(file: File, settings: KSet): KProject {
            return settings.projectMap.getOrPut(file.canonicalFile) {
                mapper.readValue<KProject>(file.readText()).also {
                    it.file = file
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
                load(File(file.parentFile, "$path.kproject.json"), settings)
            }
        }
    }

    fun resolveSource(): File {
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
        File(file, "build.gradle").writeText(buildString {
            appendLine("dependencies {")
            for (dep in deps) {
                appendLine("  add(\"${dep.gradleSourceSet}\", ${dep.gradleRef})")
            }
            appendLine("}")
        })
    }
}


val projectFile = File("./kproject.json")
if (!projectFile.exists()) {
    projectFile.writeText("{\"name\": \"${projectFile.parentFile.name}\"}")
}
val kProj = KSet()
val project = KProject.load(projectFile, kProj)
println(project)
println("kproject=$project")
//kProj.ensureGitSources(project.name, "korlibs/korge", "korge-dragonbones/src", "v3.2.0")
project.resolve(settings)
