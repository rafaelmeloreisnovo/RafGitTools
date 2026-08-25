package com.rafgittools.rafgitfs.remote

import com.rafgittools.rafgitfs.index.RafGitFsTreeMapper
import okhttp3.Headers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RafGitFsGithubEngineTest {
    @Test
    fun parsesNextPageFromGithubLinkHeader() {
        val header = "<https://api.github.com/user/repos?page=2&per_page=100>; rel=\"next\", " +
            "<https://api.github.com/user/repos?page=4&per_page=100>; rel=\"last\""
        assertEquals(2, RafGitFsPagination.nextPage(header))
        assertNull(RafGitFsPagination.nextPage(null))
    }

    @Test
    fun readsRateLimitEvidenceWithoutInventingDefaults() {
        val headers = Headers.Builder()
            .add("X-RateLimit-Limit", "5000")
            .add("X-RateLimit-Remaining", "0")
            .add("X-RateLimit-Used", "5000")
            .add("X-RateLimit-Reset", "1784980800")
            .add("X-RateLimit-Resource", "core")
            .add("Retry-After", "60")
            .build()
        val snapshot = RafGitFsRateLimitSnapshot.from(headers)
        assertTrue(snapshot.exhausted)
        assertEquals(60L, snapshot.retryAfterSeconds)
        assertEquals("core", snapshot.resource)
    }

    @Test
    fun mapsGitTreeTypesAndPreservesFavorites() {
        val mapped = RafGitFsTreeMapper.map(
            profileId = "p1",
            repositoryFullName = "owner/repo",
            refName = "main",
            entries = listOf(
                RafGitFsTreeEntryDto("docs", "040000", "tree", "abc1234"),
                RafGitFsTreeEntryDto("docs/readme.md", "100644", "blob", "def5678", 10),
                RafGitFsTreeEntryDto("current", "120000", "blob", "aaa1111", 4),
                RafGitFsTreeEntryDto("vendor/lib", "160000", "commit", "bbb2222")
            ),
            favoritePaths = setOf("docs/readme.md"),
            observedAt = 1L
        ).associateBy { it.path }
        assertEquals("DIRECTORY", mapped.getValue("docs").entryType)
        assertEquals("FILE", mapped.getValue("docs/readme.md").entryType)
        assertEquals("SYMLINK", mapped.getValue("current").entryType)
        assertEquals("SUBMODULE", mapped.getValue("vendor/lib").entryType)
        assertTrue(mapped.getValue("docs/readme.md").isFavorite)
        assertEquals("docs", mapped.getValue("docs/readme.md").parentPath)
    }

    @Test
    fun rejectsAmbiguousRepositoriesAndInvalidShas() {
        assertNull(RafGitFsGithubRemoteDataSource.splitRepository("owner/repo/extra"))
        assertEquals("owner" to "repo", RafGitFsGithubRemoteDataSource.splitRepository("owner/repo"))
        assertTrue(RafGitFsGithubRemoteDataSource.isGitSha("abcdef1"))
        assertFalse(RafGitFsGithubRemoteDataSource.isGitSha("TOKEN_VAZIO"))
    }
}
