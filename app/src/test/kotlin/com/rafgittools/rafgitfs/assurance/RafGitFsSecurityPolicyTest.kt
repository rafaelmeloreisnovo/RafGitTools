package com.rafgittools.rafgitfs.assurance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RafGitFsSecurityPolicyTest {
    private fun valid() = RafGitFsPublicationContext(
        generatedBranch = "rafgitfs/main-12345678",
        baseBranch = "main",
        planHash = "a".repeat(64),
        approvalExact = true,
        unresolvedConflicts = 0,
        stagedFileCount = 2,
        forcePush = false,
        draftPullRequest = true,
        claimAllowed = false,
        secretsPersistedInRoom = false,
        workspacePrivate = true
    )

    @Test
    fun `complete governed publication is allowed`() {
        val assessment = RafGitFsSecurityPolicy.assessPublication(valid())
        assertEquals(RafGitFsSecurityDecision.ALLOW, assessment.decision)
        assertTrue(assessment.blockingCodes.isEmpty())
        assertTrue(assessment.tokenVazioCodes.isEmpty())
    }

    @Test
    fun `force claim and non draft are blocked`() {
        val assessment = RafGitFsSecurityPolicy.assessPublication(
            valid().copy(forcePush = true, draftPullRequest = false, claimAllowed = true)
        )
        assertEquals(RafGitFsSecurityDecision.BLOCK, assessment.decision)
        assertTrue(assessment.blockingCodes.contains("SEC-FORCE-010"))
        assertTrue(assessment.blockingCodes.contains("SEC-PR-011"))
        assertTrue(assessment.blockingCodes.contains("SEC-CLAIM-001"))
    }

    @Test
    fun `missing plan hash remains token vazio`() {
        val assessment = RafGitFsSecurityPolicy.assessPublication(valid().copy(planHash = null))
        assertEquals(RafGitFsSecurityDecision.TOKEN_VAZIO, assessment.decision)
        assertEquals(listOf("SEC-PLAN-004"), assessment.tokenVazioCodes)
    }

    @Test
    fun `protected branch is recognized`() {
        assertTrue(RafGitFsSecurityPolicy.protectedBranch("MAIN"))
        assertFalse(RafGitFsSecurityPolicy.protectedBranch("rafgitfs/main-12345678"))
    }
}
