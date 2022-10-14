package com.soywiz.kproject.model

/*
import org.yaml.snakeyaml.Yaml
import java.io.*

fun KProject.Companion.load(file: File, settings: KSet, root: Boolean): KProject {
    return settings.projectMap.getOrPut(file.canonicalFile) {
        Yaml().loadAs(file.readText(), KProject::class.java)
            .also {
                it.root = root
                it.file = file.canonicalFile
                it.settings = settings
            }
    }
}
*/
