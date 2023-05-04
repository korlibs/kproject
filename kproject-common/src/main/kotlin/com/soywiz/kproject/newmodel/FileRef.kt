package com.soywiz.kproject.newmodel

import com.soywiz.kproject.git.*
import com.soywiz.kproject.util.*
import java.io.*

sealed interface FileRef {
    val name: String
    fun writeBytes(data: ByteArray)
    fun writeText(data: String) = writeBytes(data.toByteArray(Charsets.UTF_8))
    fun readBytes(): ByteArray
    fun readText(): String = readBytes().toString(Charsets.UTF_8)
    operator fun get(path: String): FileRef
    operator fun set(path: String, data: ByteArray) = this[path].writeBytes(data)
    operator fun set(path: String, data: String) = this[path].writeText(data)
    fun parent(): FileRef = get("..")
}

data class MemoryFiles(val map: MutableMap<String, ByteArray> = LinkedHashMap()) {
    override fun toString(): String = "MemoryFiles[${map.size}]"

    val root: MemoryFileRef = MemoryFileRef(this, PathInfo("/"))
    operator fun set(path: String, data: ByteArray): MemoryFiles {
        map[PathInfo(path).fullPath.trim('/')] = data
        return this
    }
    operator fun set(path: String, data: String): MemoryFiles = set(path, data.toByteArray(Charsets.UTF_8))
}

data class MemoryFileRef(val files: MemoryFiles, val path: PathInfo) : FileRef {
    override val name: String get() = path.name
    val normalized: String get() = path.fullPath.trim('/')
    companion object {
        operator fun invoke(): MemoryFileRef = MemoryFiles().root
        operator fun invoke(name: String, data: ByteArray): MemoryFileRef = MemoryFiles().set(name, data).root[name]
        operator fun invoke(data: ByteArray): MemoryFileRef = invoke("file.bin", data)
    }

    override fun writeBytes(data: ByteArray) {
        files.map[normalized] = data
    }

    override fun readBytes(): ByteArray = files.map[normalized] ?: error("Can't find file $path")
    override fun get(path: String): MemoryFileRef = MemoryFileRef(files, this.path.access(path))
}

val File.fileRef: LocalFileRef get() = LocalFileRef(this)

data class LocalFileRef(val file: File) : FileRef {
    override val name: String get() = file.name
    override fun writeBytes(data: ByteArray) {
        file.parentFile.mkdirs()
        file.writeBytes(data)
    }
    override fun readBytes(): ByteArray = file.readBytes()
    override fun get(path: String): LocalFileRef = LocalFileRef(File(file, path))
}

data class GitFileRef(val git: GitRepository, val ref: String, val path: String) : FileRef {
    override val name: String get() = PathInfo(path).name
    override fun writeBytes(data: ByteArray) = TODO()
    override fun readBytes(): ByteArray = git.getGit().readFile(ref, path)
    override fun get(path: String): GitFileRef = GitFileRef(git, ref, PathInfo(this.path).access(path).fullPath)
}
