package com.example.githubprofileexplorer.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadFileNameTest {

    @Test
    fun buildsOwnerRepoBranchName() {
        assertEquals("octocat-hello-main.zip", DownloadRepository.fileNameFor("octocat", "hello", "main"))
    }

    @Test
    fun replacesUnsafeCharacters() {
        assertEquals(
            "octo-my_repo-feature_x.zip",
            DownloadRepository.fileNameFor("octo", "my repo", "feature/x")
        )
    }
}
