import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.core.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.*
import org.eclipse.jgit.api.*
import org.eclipse.jgit.api.errors.*
import org.eclipse.jgit.errors.MissingObjectException
import org.eclipse.jgit.lib.*
import org.gradle.kotlin.dsl.support.*
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.zip.*

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        //classpath("com.soywiz.korlibs.korio:korio-jvm:3.2.0")
        classpath("org.eclipse.jgit:org.eclipse.jgit:6.3.0.202209071007-r")
        classpath("com.fasterxml.jackson.core:jackson-databind:2.13.4")
        classpath("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")
    }
}

rootProject.name = "kproject"

fun normalizePath(path: String): String = File(path).normalize().toString()
fun ensureRepo(repo: String): String = normalizePath(repo).also { if (it.count { it == '/' } != 1) error("Invalid repo '$repo'") }
fun getKProjectDir(): File = java.io.File("${System.getProperty("user.home")}/.kproject").also { it.mkdirs() }
operator fun java.io.File.get(path: String): java.io.File = java.io.File(this, path)
fun java.io.File.execToString(vararg params: String, throwOnError: Boolean = true): String {
    val result = java.lang.Runtime.getRuntime().exec(params, kotlin.arrayOf(), this)
    val stdout = result.inputStream.readBytes().toString(Charsets.UTF_8)
    val stderr = result.errorStream.readBytes().toString(Charsets.UTF_8)
    if (result.exitValue() != 0 && throwOnError) error("$stdout$stderr")
    return stdout + stderr
}

fun getCachedGitCheckout(
    projectName: String,
    repo: String,
    projectPath: String,
    rel: String,
    subfolder: String = ""
): File {
    val VERSION = 2
    val repo = ensureRepo(repo)
    val gitRepo = "https://github.com/$repo.git"
    val kprojectRoot = getKProjectDir()
    val gitFolder = File(kprojectRoot, "modules/$repo/__git__")

    val outputCheckoutDir = File(kprojectRoot, "modules/$repo/__checkouts__/$rel/$projectName")
    if (outputCheckoutDir[".gitarchive"].takeIf { it.exists() }?.readText() == "$VERSION") return outputCheckoutDir

    val git = when {
        gitFolder.exists() -> Git.open(gitFolder)
        else -> {
            println("Cloning $gitRepo into $gitFolder...")
            Git.cloneRepository()
                .setProgressMonitor(TextProgressMonitor(java.io.PrintWriter(System.out)))
                .setURI(gitRepo)
                .setDirectory(gitFolder)
                //.setBare(true)
                .call()
        }
    }

    if (!git.checkRefExists(rel)) {
        git.pull()
            .setProgressMonitor(TextProgressMonitor(java.io.PrintWriter(System.out)))
            .call()
    }

    if (!git.checkRefExists(rel)) {
        error("Can't find '$rel' in git repo")
    }

    //val localZipFile = File.createTempFile("kproject", ".zip")
    val localZipFile = File(outputCheckoutDir.parentFile, outputCheckoutDir.name + ".zip")

    try {
        println("Getting archive for '$projectName':$rel at $gitRepo :: $projectPath...")

        localZipFile.writeBytes(git.archiveZip(path = projectPath, rel = rel))
        unzipTo(File(outputCheckoutDir, subfolder), localZipFile)
        outputCheckoutDir[".gitarchive"].writeText("$VERSION")
    } finally {
        localZipFile.delete()
    }

    return outputCheckoutDir
}

/*
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
*/

class KSet {
    val projectMap by lazy { LinkedHashMap<java.io.File, KProject>() }
    val kproject by lazy { getKProjectDir() }
    val depTexts = arrayListOf<String>()
    fun addDeps(text: String) {
        depTexts.add(text)
    }
    fun ensureGitSources(projectName: String, repo: String, path: String, rel: String, subfolder: String): java.io.File {
        val repo = ensureRepo(repo)
        val basePath = "modules/$repo/__checkouts__/${rel}/$projectName"
        val folder = kproject[basePath].also { it.mkdirs() }
        val paramsFile = folder[".params"]
        val paramsStr = "$projectName::$repo::$path::$rel"

        if (paramsFile.takeIf { it.exists() }?.readText() != paramsStr) {
            //println("*******************")
            folder.deleteRecursively()
            paramsFile.parentFile.mkdirs()
            paramsFile.writeText(paramsStr)
        }
        //println("---------------")

        return getCachedGitCheckout(projectName, repo, path, rel, subfolder)

        //if (!outSrc.isDirectory) {
        //    val git = GIT(kproject["__temp_git_${System.nanoTime()}"].also { it.mkdirs() })
        //    try {
        //        git.downloadArchiveSubfolders(repo, path, rel)
        //        val finalFolder = git.vfs[path]
        //        outSrc.parentFile.mkdirs()
        //        if (!finalFolder.exists()) error("$finalFolder doesn't exists")
        //        finalFolder.renameTo(outSrc)
        //        println("finalFolder=$finalFolder -> outSrc=$outSrc")
        //    } finally {
        //        //git.vfs.deleteRecursively()
        //    }
        //}
        //return folder
    }
}

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
                project.settings.ensureGitSources(project.name, repo, path, version, "src")
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
    @JsonIgnore lateinit var file: java.io.File
    @JsonIgnore lateinit var settings: KSet

    val projectDir: java.io.File get() = file.parentFile

    companion object {
        fun load(file: java.io.File, settings: KSet): KProject {
            return settings.projectMap.getOrPut(file.canonicalFile) {
                JSON5.mapper.readValue<KProject>(file.readText()).also {
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
                load(File(file, "kproject.json5"), settings)
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
        //println("resolve: $name -> $file")
        settings.include(":${name}")
        settings.project(":${name}").projectDir = file
        val deps = dependencies.map { resolveDependency(it).also { it.resolve(settings) } }
        val buildGradleText = buildString {
            appendLine("configure([project(\":${name}\")]) {")
            appendLine("  dependencies {")
            for (dep in deps) {
                appendLine("    add(\"${dep.gradleSourceSet}\", ${dep.gradleRef})")
            }
            appendLine("  }")
            appendLine("}")
        }
        this.settings.addDeps(buildGradleText)
        //val buildGradleFile = java.io.File(file, "build.gradle")
        //if (buildGradleFile.takeIf { it.exists() }?.readText() != buildGradleText) {
        //    buildGradleFile.writeText(buildGradleText)
        //}
    }
}

run {
    File(rootDir, "gradle/.gitignore").writeTextIfNew(
        """
            build.gradle*
            deps.gradle*
            *.settings.gradle.kts
        """.trimIndent()
    )
    File(rootDir, "gradle/build.gradle.kts").writeTextIfNew(
        // language: Kotlin
        """
        // AUTOGENERATED: DO NOT MODIFY
        plugins {
            kotlin("multiplatform") version "1.7.20"
        }

        allprojects {
            repositories {
                mavenLocal()
                mavenCentral()
                google()
            }

            apply(plugin = "kotlin-multiplatform")

            kotlin {
                metadata {
                    compilations.all {
                        kotlinOptions.suppressWarnings = true
                    }
                }
                jvm {
                    compilations.all {
                        kotlinOptions.jvmTarget = "1.8"
                        kotlinOptions.suppressWarnings = true
                        kotlinOptions.freeCompilerArgs = listOf("-Xno-param-assertions")
                    }
                }
                js(org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.IR) {
                    browser {
                        compilations.all {
                            //kotlinOptions.sourceMap = true
                            kotlinOptions.suppressWarnings = true
                        }
                    }
                }
                for (target in listOf("common", "jvm", "js")) {
                    sourceSets[target + "Main"].kotlin.srcDir(file("src/" + target + "/src"))
                    sourceSets[target + "Main"].resources.srcDir(file("src/" + target + "/resources"))
                }
                sourceSets.maybeCreate("concurrentMain").apply {
                    kotlin.srcDir(file("src/concurrentMain/kotlin"))
                    kotlin.srcDir(file("src/concurrent/src"))
                }
                sourceSets["jvmMain"].apply {
                    kotlin.srcDir(file("src/jvmAndroidMain/kotlin"))
                    resources.srcDir(file("src/jvmAndroidMain/resources"))
                }

                sourceSets["concurrentMain"].dependsOn(sourceSets["commonMain"])
                sourceSets["jvmMain"].dependsOn(sourceSets["concurrentMain"])

                for (sourceSet in sourceSets) {
                    val optInAnnotations = listOf(
                        "kotlin.RequiresOptIn",
                        "kotlin.experimental.ExperimentalTypeInference",
                        "kotlin.ExperimentalMultiplatform",
                        "kotlinx.coroutines.DelicateCoroutinesApi",
                        "kotlinx.coroutines.ExperimentalCoroutinesApi",
                        "kotlinx.coroutines.ObsoleteCoroutinesApi",
                        "kotlinx.coroutines.InternalCoroutinesApi",
                        "kotlinx.coroutines.FlowPreview"
                    )

                    sourceSet.languageSettings {
                        optInAnnotations.forEach { optIn(it) }
                        progressiveMode = true
                    }
                }
            }
        }
        
        File(rootDir, "build.extra.gradle").takeIf { it.exists() }?.let { apply(from = it) }
        File(rootDir, "build.extra.gradle.kts").takeIf { it.exists() }?.let { apply(from = it) }
        File(rootDir, "gradle/deps.gradle").takeIf { it.exists() }?.let { apply(from = it) }
        File(rootDir, "gradle/deps.gradle.kts").takeIf { it.exists() }?.let { apply(from = it) }
    """.trimIndent())
}

fun File.writeTextIfNew(text: String) {
    if (takeIf { it.exists() }?.readText() != text) {
        writeText(text)
    }
}

fun Git.archiveZip(
    path: String,
    rel: String,
): ByteArray {
    class ExtZipOutputStream(out: OutputStream, val params: Map<String?, Any?>?) : ZipOutputStream(out) {
        var removePrefix: String? = params?.getOrElse("removePrefix") { null }?.toString()
    }

    class ZipArchiveFormat : ArchiveCommand.Format<ExtZipOutputStream> {
        override fun suffixes(): Iterable<String> = setOf(".mzip")
        override fun createArchiveOutputStream(s: OutputStream): ExtZipOutputStream = createArchiveOutputStream(s, null)
        override fun createArchiveOutputStream(s: OutputStream, o: Map<String?, Any?>?): ExtZipOutputStream = ExtZipOutputStream(s, o).also { it.setLevel(1) }
        override fun putEntry(out: ExtZipOutputStream, tree: ObjectId, path: String, mode: FileMode, loader: ObjectLoader?) {
            //println("ZIP: $path -- ${loader?.bytes?.size}")
            if (loader == null) return
            // loader is null for directories...
            val entry = java.util.zip.ZipEntry(path.trim('/').removePrefix(out.removePrefix?.trim('/') ?: "").trim('/'))
            out.putNextEntry(entry)
            out.write(loader.bytes)
            out.closeEntry()
        }
    }

    kotlin.runCatching { ArchiveCommand.unregisterFormat("mzip") }
    ArchiveCommand.registerFormat("mzip", ZipArchiveFormat());
    try {
        val mem = ByteArrayOutputStream()
        this.archive()
            .setTree(this.repository.resolve(rel))
            .setPaths(path, path.trim('/'))
            .setFilename("archive.mzip")
            .setFormat("mzip")
            .setFormatOptions(mapOf("removePrefix" to "$path/"))
            .setOutputStream(mem)
            .call()
        return mem.toByteArray()
    } finally {
        ArchiveCommand.unregisterFormat("mzip")
    }
}

fun Git.checkRefExists(rel: String): Boolean {
    if (this.repository.findRef(rel) != null) return true
    //return this.repository.findRef(rel) != null
    ///*
    try {

        describe().setTarget(rel).call()
        return true
    } catch (e: MissingObjectException) {
        return false
    } catch (e: RefNotFoundException) {
        return false
    }

     //*/
}

fun main(settings: Settings) {
    val projectFile = File(rootDir, "kproject.json5").also {
        if (!it.exists()) it.writeText(
            """
                {
                    // Name of the project
                    name: "untitled",
                    version: "unknown",
                    // Dependency list, to other kproject.json5 modules or maven
                    // Examples: 
                    // - "./libs/krypto",
                    // - "maven::common::org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4"
                    dependencies: [
                    ],
                }
            """.trimIndent()
        )
    }

    File(rootDir, ".editorconfig").takeIf { !it.exists() }?.writeText("""
        [*]
        charset=utf-8
        end_of_line=lf
        insert_final_newline=true
        indent_style=space
        indent_size=4
        ij_kotlin_name_count_to_use_star_import = 1
        ij_kotlin_blank_lines_before_declaration_with_comment_or_annotation_on_separate_line = 0
        ij_kotlin_variable_annotation_wrap = off
    
        [*.json,*.json5]
        indent_size=2
    
        [*.yml]
        indent_size = 2
    """.trimIndent())

    File(rootDir, ".gitignore").takeIf { !it.exists() }?.writeText("""
        /.idea
        /.gradle
        /build
    """.trimIndent())

    val kProj = KSet()
    val project = KProject.load(projectFile, kProj)
    project.resolve(settings)

    File(settings.rootDir, "gradle/deps.gradle").writeText("// WARNING!! AUTO GENERATED, DO NOT MODIFY BY HAND!\n\n" + kProj.depTexts.joinToString("\n"))
    settings.project(":").buildFileName = "gradle/build.gradle.kts"
}


main(settings)


//getCachedGitCheckout("adder", "korlibs/kproject", "modules/adder", "04625c0832255cc473a24faf75e82abbe61db56c")

//git.checkout().setName("614ebf68ce00abe6e23c5cfb3d31c601c3e75ed2").call()

// [Ref[refs/heads/main=04625c0832255cc473a24faf75e82abbe61db56c(-1)]]
//println(git.branchList().call())
