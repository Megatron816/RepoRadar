package com.example.githubprofileexplorer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkValidatorTest {

    @Test
    fun acceptsProfileLinks() {
        assertEquals("octocat", DeepLinkValidator.username("https://github.com/octocat"))
        assertEquals("octocat", DeepLinkValidator.username("https://github.com/octocat/"))
    }

    @Test
    fun usesOwnerForRepositoryLinks() {
        assertEquals("octocat", DeepLinkValidator.username("https://github.com/octocat/hello-world"))
    }

    @Test
    fun rejectsOtherHostsAndSchemes() {
        assertNull(DeepLinkValidator.username("https://example.com/octocat"))
        assertNull(DeepLinkValidator.username("http://github.com/octocat"))
    }

    @Test
    fun rejectsInvalidUsernames() {
        assertNull(DeepLinkValidator.username("https://github.com/bad%20name"))
        assertNull(DeepLinkValidator.username("https://github.com/"))
    }
}
