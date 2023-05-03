package com.soywiz.kproject.model

import com.soywiz.kproject.git.*
import kotlin.test.*

class GitRepositoryTest {
    @Test
    fun testClone() {
        assertEquals("github.com/korlibs/korge-ext", GitRepository("git@github.com:korlibs/korge-ext.git").cachePath)
    }

    @Test
    fun testArchive() {
        val kprojectRepo = GitRepository("https://github.com/korlibs/kproject.git")

        assertEquals(true, kprojectRepo.getGit().checkRelMatches("v0.1.2", "1e473f6d1e7db37982be808d8303ca908e754043"))

        assertEquals(
            "fef7f40204f4fa070f08090bf70d02f4f107020204030af2010bf6f10404fef8",
            GitRepositoryWithPathAndRef(kprojectRepo, "/example/sample", "v0.1.2").getContent().hash
        )

        //GitRepositoryWithPathAndRef(kprojectRepo, "/example/sample", "v0.9999.2").getContent()
    }
}
