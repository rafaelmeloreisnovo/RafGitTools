package com.rafgittools.ui.screens.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubOwnerScopeTest {
    private data class Repo(val id: Long, val owner: String)

    private val inventory = listOf(
        Repo(1, "rafaelmeloreisnovo"),
        Repo(2, "instituto-Rafael"),
        Repo(3, "instituto-Rafael")
    )

    @Test fun allIsReadOnlyFilterNotImplicitOrgGrant() {
        assertEquals(3, GithubOwnerScope.filter(inventory, null) { it.owner }.size)
    }

    @Test fun selectingOrganizationExcludesPersonalRepositories() {
        assertEquals(listOf(2L, 3L), GithubOwnerScope.filter(inventory, "instituto-rafael") { it.owner }.map { it.id })
    }

    @Test fun unknownOrBlankSelectionNeverFallsBackToAll() {
        assertTrue(GithubOwnerScope.filter(inventory, "unknown") { it.owner }.isEmpty())
        assertTrue(GithubOwnerScope.filter(inventory, "") { it.owner }.isEmpty())
    }

    @Test fun personalAndOrganizationRemainDistinct() {
        assertTrue(GithubOwnerScope.matches("instituto-Rafael", "instituto-rafael"))
        assertFalse(GithubOwnerScope.matches("instituto-Rafael", "rafaelmeloreisnovo"))
    }
}
