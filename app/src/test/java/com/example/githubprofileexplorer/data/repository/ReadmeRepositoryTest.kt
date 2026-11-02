package com.example.githubprofileexplorer.data.repository

import com.example.githubprofileexplorer.data.model.ReadmeResponse
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadmeRepositoryTest {

    @Test
    fun decodesBase64WithLineBreaks() {
        val text = "# Title\n" + "word ".repeat(40)
        val encoded = Base64.getMimeEncoder().encodeToString(text.toByteArray())
        assertEquals(text, ReadmeRepository.decode(ReadmeResponse(encoded, "base64")))
    }

    @Test
    fun returnsPlainContentUnchanged() {
        assertEquals("plain", ReadmeRepository.decode(ReadmeResponse("plain", "utf-8")))
    }

    @Test
    fun missingContentIsNull() {
        assertNull(ReadmeRepository.decode(ReadmeResponse(null, "base64")))
    }

    @Test
    fun invalidBase64IsNull() {
        assertNull(ReadmeRepository.decode(ReadmeResponse("%%%not base64%%%", "base64")))
    }
}
