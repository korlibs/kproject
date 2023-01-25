package com.soywiz.kproject.model

import com.soywiz.kproject.internal.*
import java.io.*

fun KProject.Companion.load(file: File, settings: KSet, root: Boolean, forcedContent: String? = null): KProject {
    return settings.projectMap.getOrPut(file.canonicalFile) {
        val content = forcedContent ?: file.readText()
        val map = Yaml.read(content).dyn
        KProject(
            name = map["name"].toStringOrNull(),
            version = map["version"].toStringOrNull() ?: "unknown",
            type = map["type"].toStringOrNull() ?: "library",
            src = map["src"].toStringOrNull(),
            plugins = map["plugins"].list.map { it.str },
            gradle = map["gradle"].list.map { it.str },
            dependencies = map["dependencies"].list.map { it.str },
            targets = map["targets"].list.map { it.str }.toSet().let { it.ifEmpty { setOf("all") } },
        ).also {
            it.root = root
            it.file = file.canonicalFile
            it.settings = settings
        }
    }
}

