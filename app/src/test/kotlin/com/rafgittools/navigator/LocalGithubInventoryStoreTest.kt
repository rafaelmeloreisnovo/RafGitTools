package com.rafgittools.navigator

import com.rafgittools.domain.model.github.GithubRepository
import com.rafgittools.domain.model.github.GithubUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LocalGithubInventoryStoreTest {
    private fun repo(fullName: String = "rafaelmeloreisnovo/RafGitTools", privateRepo: Boolean = true) =
        GithubRepository(
            id = 42L, name = "RafGitTools", fullName = fullName,
            owner = GithubUser(
                id = 7L, login = "rafaelmeloreisnovo",
                avatarUrl = "", htmlUrl = "", type = "User"
            ),
            description = "Tool for people and AI",
            htmlUrl = "https://github.com/rafaelmeloreisnovo/RafGitTools",
            cloneUrl = "https://SECRET_TOKEN@github.com/repo.git",
            sshUrl = "git@github.com:private/repo.git",
            language = "Kotlin", stargazersCount = 7,
            forksCount = 2, watchersCount = 7, openIssuesCount = 11,
            isPrivate = privateRepo, isFork = false,
            defaultBranch = "main", createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2026-11-01T00:00:00Z", pushedAt = null
        )

    @Test fun localCatalogIsExplicitAndNotATreeOrUniversalInventory() {
        val snapshot = LocalGithubInventoryStore.observe(
            "rafaelmeloreisnovo", listOf(repo()), complete = true, at = 42L
        )
        assertEquals(1, snapshot.observed_repositories)
        assertEquals("OBSERVED_LIST_PAGES_COMPLETE", snapshot.enumeration)
        assertEquals("TOKEN_VAZIO_NOT_ATTESTED", snapshot.all_accounts)
        assertFalse(snapshot.cloned_content)
        assertFalse(snapshot.claim_allowed)
        assertEquals("PRIVATE", snapshot.repositories.single().visibility)
        assertEquals("TOKEN_VAZIO_NOT_SCANNED", snapshot.repositories.single().workflows)
        assertEquals("TOKEN_VAZIO_NOT_VERIFIED", snapshot.repositories.single().checks)
        assertEquals("TOKEN_VAZIO_NOT_DERIVED", snapshot.repositories.single().dependencies)
        assertEquals("Kotlin", snapshot.repositories.single().language)
    }

    @Test fun partialListStaysTypedUnknownAndDeduplicates() {
        val snapshot = LocalGithubInventoryStore.observe(
            "rafaelmeloreisnovo", listOf(repo(), repo()), complete = false, at = 42L
        )
        assertEquals(1, snapshot.observed_repositories)
        assertEquals("TOKEN_VAZIO_PARTIAL", snapshot.enumeration)
    }

    @Test fun noCredentialsOrCloneUrlInRepoMetadata() {
        val snap = LocalGithubInventoryStore.observe(
            "rafaelmeloreisnovo", listOf(repo()), complete = true, at = 42L
        )
        val data = snap.repositories.single().toString()
        assertFalse(data.contains("SECRET_TOKEN"))
        assertFalse(data.contains("git@github.com"))
        assertFalse(data.contains("https://"))
    }

    @Test fun rejectsInjectedPathsAndMissingIdentity() {
        reject { LocalGithubInventoryStore.observe("", listOf(repo()), true, 1L) }
        reject { LocalGithubInventoryStore.observe("rafaelmeloreisnovo", listOf(repo("../secret")), true, 1L) }
        reject { LocalGithubInventoryStore.observe("rafaelmeloreisnovo", listOf(repo()), true, -1L) }
    }

    @Test fun digestIsDeterministic() {
        val bytes = "LOCAL_METADATA".toByteArray()
        assertEquals(64, LocalGithubInventoryStore.sha256(bytes).length)
        assertEquals(LocalGithubInventoryStore.sha256(bytes), LocalGithubInventoryStore.sha256(bytes))
    }

    private inline fun reject(operation: () -> Unit) {
        try {
            operation()
            fail("Unsafe inventory accepted")
        } catch (_: IllegalArgumentException) {
            // Fail closed.
        }
    }
}
