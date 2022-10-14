package com.soywiz.kproject.util

import java.io.*

fun File.writeTextIfNew(text: String) {
    if (takeIf { it.exists() }?.readText() != text) {
        writeText(text)
    }
}
