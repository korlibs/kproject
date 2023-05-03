package com.soywiz.kproject.model

import com.soywiz.kproject.util.*
import java.io.*
import kotlin.io.path.*

sealed interface Dependency : Comparable<Dependency> {
    val name: String
    val version: Version

    override fun compareTo(other: Dependency): Int = version.compareTo(other.version)
    fun kprojectFile(): File? = null

    companion object
}

data class GitDependency(
    override val name: String,
    val repo: GitRepository,
    val path: String,
    val ref: String,
    val commit: String? = null,
    val hash: String? = null,
) : Dependency {

    override val version: Version get() = Version(ref)
}

data class MavenDependency(
    val group: String,
    override val name: String,
    override val version: Version,
    val target: String = "common",
) : Dependency {
    val coordinates: String = "$group:$name:${version.str}"

    companion object {
        fun fromCoordinates(coordinates: String, target: String = "common"): MavenDependency {
            val (group, name, version) = coordinates.split(':')
            return MavenDependency(group, name, Version(version), target)
        }
    }
}

data class FolderDependency(
    val path: String,
) : Dependency {
    override val name: String = Path(path).fileName.toString()
    override val version: Version get() = Version("999.999.999.999")
}

fun Dependency.Companion.parseString(str: String): Dependency {
    try {
        val parts = str.split("::")
        val firstPart = parts.first()
        when (firstPart) {
            // - git::adder::korlibs/kproject::/modules/adder::54f73b01cea9cb2e8368176ac45f2fca948e57db
            "git" -> {
                val (_, name, coordinates, path, ref) = parts
                return GitDependency(name, GitRepository("https://github.com/${normalizePath(coordinates)}.git"), path, ref, commit = parts.getOrNull(5), hash = parts.getOrNull(6))
            }
            // - maven::common::org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4
            "maven" -> {
                val (_, target, coordinates) = parts
                return MavenDependency.fromCoordinates(coordinates, target)
            }
            else -> when {
                // - git@github.com:korlibs/korge-ext.git/korge-tiled#0.0.1::734d96ccc18733064ef9fbda8ac359585011112d
                firstPart.contains(".git") -> {
                    val repo = firstPart.substringBefore(".git") + ".git"
                    val path = PathInfo(firstPart.substringAfter(".git").substringBefore('#').takeIf { it.isNotBlank() } ?: "/").fullPath
                    val ref = PathInfo(firstPart.substringAfter('#', "").takeIf { it.isNotEmpty() } ?: error("Missing ref as #")).fullPath
                    val name = PathInfo(File(path).name.removeSuffix(".git").takeIf { it.isNotEmpty() } ?: File(repo).name.removeSuffix(".git")).fullPath
                    return GitDependency(name, GitRepository(repo), path, ref, commit = parts.getOrNull(1), hash = parts.getOrNull(2))
                }
                // org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4
                firstPart.contains(':') -> {
                    val (group, name, version) = firstPart.split(":")
                    return MavenDependency(group, name, Version(version), parts.getOrNull(1) ?: "common")
                }
                // ../korge-tiled
                parts.size == 1 -> {
                    return FolderDependency(firstPart)
                }
            }
        }
        error("""
            Don't know how to handle '$str'
            Supported formats:
                ## FOLDER:
                - ../korge-tiled
                
                ## GIT:
                - git::adder::korlibs/kproject::/modules/adder::54f73b01cea9cb2e8368176ac45f2fca948e57db
                - git@github.com:korlibs/korge-ext.git/korge-tiled#0.0.1::734d96ccc18733064ef9fbda8ac359585011112d
                - https://github.com:korlibs/korge-ext.git/korge-tiled#0.0.1::734d96ccc18733064ef9fbda8ac359585011112d
                
                ## MAVEN:
                - maven::common::org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4
                - org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4
                - org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4::common
                - org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4::jvm
        """)
    } catch (e: Throwable) {
        throw IllegalArgumentException("Invalid format for string '$str'", e)
    }
}
