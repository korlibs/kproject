@file:DependsOn("com.soywiz.korlibs.korio:korio-jvm:3.2.0")
@file:DependsOn("com.fasterxml.jackson.core:jackson-databind:2.13.4")
@file:DependsOn("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")

import com.fasterxml.jackson.module.kotlin.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.lang.management.*

runBlocking {
    val project = Project.load(File("sample/korge-dragonbones.kproject.json"))
    println(project)

    println("kproject=$project")
    KProj.ensureGitSources(project.name, "korlibs/korge", "korge-dragonbones/src", "v3.2.0")

    /*
    localCurrentDirVfs["temp"].also { it.mkdirs() }

    val git = GIT()
    git.downloadArchive("korlibs/korge", "korge-dragonbones/src", "v3.2.0")

     */
}

object KProj {
    val kproject = runBlocking { localVfs(Environment.expand("~/.kproject")).also { it.mkdirs() }.jail() }

    suspend fun ensureGitSources(projectName: String, repo: String, path: String, rel: String) {
        val git = GIT(kproject["__temp_git_${ManagementFactory.getRuntimeMXBean().name}_${System.nanoTime()}"].also { it.mkdirs() })
        git.downloadArchiveSubfolders(repo, path, rel)
        val finalFolder = git.vfs[path]
        val basePath = "modules/$projectName/${rel}"
        val folder = kproject[basePath].also { it.mkdirs() }
        finalFolder.renameTo("$basePath/src")
        git.vfs.deleteRecursive()
    }
}

class GIT(val vfs: VfsFile) {
    companion object {
        fun ensureGitRepo(repo: String): String = when {
            repo.contains("://") || repo.contains("git@") || repo.contains(":") -> repo
            else -> "https://github.com/${PathInfo(repo).normalize()}.git"
        }
    }

    suspend fun downloadArchiveSubfolders(repo: String, path: String, rel: String, vfs: VfsFile = this.vfs) {
        vfs.mkdirs()
        if (vfs.listNames().isEmpty()) {
            println(vfs.execToString("git", "clone", "--branch", rel, "--depth", "1", "--filter=blob:none", "--sparse", ensureGitRepo(repo), "."))
            println(vfs.execToString("git", "sparse-checkout", "set", PathInfo(path).normalize()))
        }
        vfs.delete()
    }
}

data class Project(
    val name: String,
    val version: String = "unknown",
    val type: String? = "library",
    val src: String? = null,
    val dependencies: List<String> = emptyList()
) {
    companion object {
        private val mapper = jacksonObjectMapper()

        fun load(file: File): Project {
            return mapper.readValue<Project>(file.readText())
        }
    }
}

suspend fun VfsFile.deleteRecursive() {
    this.list().collect {
        if (it.isDirectory()) {
            it.deleteRecursive()
        } else {
            it.delete()
        }
    }
    this.delete()
}
