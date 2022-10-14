package com.soywiz.kproject.git

import com.soywiz.kproject.model.*
import com.soywiz.kproject.util.*
import org.eclipse.jgit.api.*
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.*
import org.eclipse.jgit.errors.*
import org.eclipse.jgit.lib.*
import java.io.*
import java.util.zip.*

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
            val entry = ZipEntry(path.trim('/').removePrefix(out.removePrefix?.trim('/') ?: "").trim('/'))
            out.putNextEntry(entry)
            out.write(loader.bytes)
            out.closeEntry()
        }
    }

    kotlin.runCatching { ArchiveCommand.unregisterFormat("mzip") }
    ArchiveCommand.registerFormat("mzip", ZipArchiveFormat())
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
    return try {
        describe().setTarget(rel).call()
        true
    } catch (e: MissingObjectException) {
        false
    } catch (e: RefNotFoundException) {
        false
    }
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
                .setProgressMonitor(TextProgressMonitor(PrintWriter(System.out)))
                .setURI(gitRepo)
                .setDirectory(gitFolder)
                //.setBare(true)
                .call()
        }
    }

    if (!git.checkRefExists(rel)) {
        git.pull()
            .setProgressMonitor(TextProgressMonitor(PrintWriter(System.out)))
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
