package com.soywiz.kproject.model

import com.fasterxml.jackson.core.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.*
import java.io.*

object YAML {
    val mapper = ObjectMapper(YAMLFactory()).registerModule(kotlinModule {  })
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        .configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true)
        .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        .configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
        .configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true)
        .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
        .configure(JsonParser.Feature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS, true)
        .findAndRegisterModules()
}

fun KProject.Companion.load(file: File, settings: KSet, root: Boolean): KProject {
    return settings.projectMap.getOrPut(file.canonicalFile) {
        val content = file.readText()
        if (content.contains("!!")) error("!! can't appear in YAML for security reasons until snakeyml supports disabling specifying classes CVE-2022-1471")
        YAML.mapper.readValue<KProject>(content).also {
            it.root = root
            it.file = file.canonicalFile
            it.settings = settings
        }
    }
}

