package com.soywiz.kproject.util

import kotlin.test.*

class NormalizePathTest {
    @Test
    fun test() {
        assertEquals("/", PathInfo("/").fullPath)
        assertEquals("/", PathInfo("//").fullPath)
        assertEquals("/hello", PathInfo("/hello").fullPath)
        assertEquals("/hello/world", PathInfo("/hello/world").fullPath)
        assertEquals("/world", PathInfo("/hello/../world").fullPath)
        assertEquals("/world", PathInfo("/////hello///..///world/").fullPath)
        assertEquals("", PathInfo("").fullPath)
        assertEquals("", PathInfo("../../../../").fullPath)
    }
}
