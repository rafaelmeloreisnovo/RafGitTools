package com.rafgittools.ui.screens.privacy

import com.rafgittools.data.privacy.PrivacyRepositoryDto
import com.rafgittools.data.privacy.PrivacyRepositoryOwnerDto
import com.rafgittools.data.privacy.PrivacyRepositoryPermissionsDto
import com.rafgittools.data.privacy.PrivacyVisibilityTarget
import com.rafgittools.data.privacy.toCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryPrivacySelectionTest {
    @Test
    fun `owner checklist supports independent multi-selection and empty scope`() {
        val owners = setOf("personal", "org-a", "org-b")
        val withoutOrgA = PrivacyOwnerSelection.toggle(null, owners, "org-a")
        assertEquals(setOf("personal", "org-b"), withoutOrgA)
        val onlyPersonal = PrivacyOwnerSelection.toggle(withoutOrgA, owners, "org-b")
        assertEquals(setOf("personal"), onlyPersonal)
        val none = PrivacyOwnerSelection.toggle(onlyPersonal, owners, "personal")
        assertTrue(none.isEmpty())
        val one = PrivacyOwnerSelection.toggle(none, owners, "org-b")
        assertEquals(setOf("org-b"), one)
    }

    @Test
    fun `unknown organization cannot enter the authorized scope`() {
        val owners = setOf("personal", "org-a")
        assertEquals(owners, PrivacyOwnerSelection.toggle(null, owners, "nonexistent"))
    }

    @Test
    fun `selected candidates must belong to owner scope and inverse visibility`() {
        val aPublic = candidate(1, "org-a", private = false)
        val aPrivate = candidate(2, "org-a", private = true)
        val bPublic = candidate(3, "org-b", private = false)
        val all = listOf(aPublic, aPrivate, bPublic)
        val state = RepositoryPrivacyUiState(
            candidates = all,
            selectedOwners = setOf("org-a"),
            selectedIds = setOf(1L, 2L, 3L),
            targetVisibility = PrivacyVisibilityTarget.PRIVATE
        )
        assertEquals(PrivacyVisibilityTarget.PUBLIC, state.sourceVisibility)
        assertEquals(listOf(1L), state.selectedCandidates().map { it.id })
        assertFalse(state.ownerIncluded("org-b"))
        assertTrue(state.ownerIncluded("org-a"))

        val reversed = state.copy(targetVisibility = PrivacyVisibilityTarget.PUBLIC, selectedIds = setOf(1L, 2L, 3L))
        assertEquals(PrivacyVisibilityTarget.PRIVATE, reversed.sourceVisibility)
        assertEquals(listOf(2L), reversed.selectedCandidates().map { it.id })

        val noOwners = state.copy(selectedOwners = emptySet())
        assertTrue(noOwners.selectedCandidates().isEmpty())
        val allOwners = state.copy(selectedOwners = null)
        assertEquals(listOf(1L, 3L), allOwners.selectedCandidates().map { it.id })
    }

    @Test
    fun `source and target always remain opposite directions`() {
        val initial = RepositoryPrivacyUiState()
        assertEquals(PrivacyVisibilityTarget.PUBLIC, initial.sourceVisibility)
        assertEquals(PrivacyVisibilityTarget.PRIVATE, initial.targetVisibility)
        assertFalse(initial.sourceVisibility == initial.targetVisibility)

        val reversed = initial.copy(targetVisibility = PrivacyVisibilityTarget.PUBLIC)
        assertEquals(PrivacyVisibilityTarget.PRIVATE, reversed.sourceVisibility)
        assertFalse(reversed.sourceVisibility == reversed.targetVisibility)
    }

    private fun candidate(id: Long, owner: String, private: Boolean) = PrivacyRepositoryDto(
        id = id,
        name = "repo-$id",
        fullName = "$owner/repo-$id",
        owner = PrivacyRepositoryOwnerDto(owner, "Organization"),
        isPrivate = private,
        isFork = false,
        visibility = if (private) "private" else "public",
        permissions = PrivacyRepositoryPermissionsDto(admin = true)
    ).toCandidate()
}
