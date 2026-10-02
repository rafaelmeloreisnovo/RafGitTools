package com.rafgittools.data.github

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryCreationModelsTest {

    @Test
    fun createRequestAlwaysUsesPrivateVisibilityAndInitializesReadme() {
        val body = JsonParser.parseString(
            Gson().toJson(CreateUserRepositoryRequest(name = "Atlas", description = "Research"))
        ).asJsonObject

        assertEquals("Atlas", body.get("name").asString)
        assertEquals("Research", body.get("description").asString)
        assertTrue(body.get("private").asBoolean)
        assertTrue(body.get("auto_init").asBoolean)
    }

    @Test
    fun repositoryNameIsTrimmedAndLimitedToGithubSafeCharacters() {
        assertEquals("Atlas_v2.repo", normalizeGitHubRepositoryName(" Atlas_v2.repo "))
        assertNull(normalizeGitHubRepositoryName(""))
        assertNull(normalizeGitHubRepositoryName("two words"))
        assertNull(normalizeGitHubRepositoryName("owner/repo"))
        assertNull(normalizeGitHubRepositoryName(".."))
    }
}
