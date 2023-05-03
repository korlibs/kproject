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
            "0103fbff07fd0df2fcfef70702f609fd05f7f80ff9f6f7f40908010ef5f105f4",
            GitRepositoryWithPathAndRef(kprojectRepo, "/example/sample", "v0.1.2").getContent().hash
        )

        //GitRepositoryWithPathAndRef(kprojectRepo, "/example/sample", "v0.9999.2").getContent()
    }
}
