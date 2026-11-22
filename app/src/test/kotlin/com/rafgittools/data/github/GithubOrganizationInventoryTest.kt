package com.rafgittools.data.github

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GithubOrganizationInventoryTest {
    private data class Repo(val id: Long, val owner: String)

    @Test fun acceptsRealisticOrganizationLoginCaseInsensitively() {
        assertTrue(GithubOrganizationInventory.isValidLogin("instituto-Rafael"))
        assertTrue(GithubOrganizationInventory.matchesOwner("instituto-Rafael", "INSTITUTO-rafael"))
    }

    @Test fun rejectsMalformedOrganizationsAndInjectedPaths() {
        for (candidate in listOf("", "-org", "org-", "a/b", "../org", " org",
            "org name", "github_pat_redacted", "a".repeat(40))) {
            assertFalse(candidate, GithubOrganizationInventory.isValidLogin(candidate))
        }
    }

    @Test fun acceptsOnlyExactOrganizationOwnedRepositories() {
        val org = listOf(Repo(1, "instituto-Rafael"), Repo(2, "instituto-rafael"))
        assertEquals(org, GithubOrganizationInventory.requireScopedOwner("instituto-Rafael", org) {
            it.owner
        })
    }

    @Test fun crossOrganizationResponseIsRejectedRatherThanMixed() {
        val mixed = listOf(Repo(1, "instituto-Rafael"), Repo(2, "someone-else"))
        try {
            GithubOrganizationInventory.requireScopedOwner("instituto-Rafael", mixed) { it.owner }
            fail("cross-owner response must fail closed")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test fun privateVisibilityIsNotGrantedBySelectingOwner() {
        val repos = listOf(Repo(1, "rafaelmeloreisnovo"), Repo(2, "instituto-Rafael"))
        // No inferred permission or cross-owner fallback; this guard checks identity only.
        assertEquals(listOf(2L), repos.filter {
            GithubOrganizationInventory.matchesOwner("instituto-Rafael", it.owner)
        }.map { it.id })
    }
}
