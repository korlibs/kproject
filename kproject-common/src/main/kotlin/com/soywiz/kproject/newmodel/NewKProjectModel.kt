package com.soywiz.kproject.newmodel

import com.soywiz.kproject.internal.*

//enum class NewKProjectType { LIBRARY, EXECUTABLE }

data class NewKProjectModel(
    val file: FileRef,
    val name: String?,
    val src: Dependency? = null,
    val version: String? = null,
    val targets: List<String> = emptyList(),
    val plugins: List<KPPlugin> = emptyList(),
    val dependencies: List<Dependency> = emptyList()
) {
    companion object
}

fun NewKProjectModel.Companion.loadFile(file: FileRef): NewKProjectModel {
    return parseObject(Yaml.decode(file.readText()), file)
}

fun NewKProjectModel.Companion.parseObject(data: Any?, file: FileRef = MemoryFileRef("unknown.kproject.yml", byteArrayOf())): NewKProjectModel {
    val data = data.dyn

    return NewKProjectModel(
        file = file,
        name = data["name"].toStringOrNull() ?: (if (file.name != "kproject.yml") file.name.removeSuffix(".kproject.yml") else file.parent().name),
        src = data["src"].orNull { Dependency.parseObject(it.value, file) },
        version = data["version"].orNull { it.str },
        targets = data["targets"].list.map { it.str },
        plugins = data["plugins"].list.map { KPPlugin.parseObject(it.value) },
        dependencies = data["dependencies"].list.map { Dependency.parseObject(it.value, file) },
    )
}
