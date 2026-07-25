package com.rafgittools.rafgitfs.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RafGitFsGovernedSyncTest {
    private val observed = listOf(
        RafGitFsObservedFile("a.txt", "remote-a", "local-a", true, true),
        RafGitFsObservedFile("b.txt", "same", "same", true, true),
        RafGitFsObservedFile("c.txt", "remote-c", null, false, true)
    )

    @Test
    fun `plan is deterministic for same evidence and timestamp`() {
        val diffs = RafGitFsDiffPlanner.diff(observed)
        val first = RafGitFsDiffPlanner.plan(
            "req", "profile", "owner/repo", "main", "base", diffs,
            RafGitFsPlannedAction.CACHE_DOWNLOAD, generatedAt = 42L
        )
        val second = RafGitFsDiffPlanner.plan(
            "req", "profile", "owner/repo", "main", "base", diffs.reversed(),
            RafGitFsPlannedAction.CACHE_DOWNLOAD, generatedAt = 42L
        )
        assertEquals(first.planHash, second.planHash)
        assertFalse(first.claimAllowed)
    }

    @Test
    fun `remote write is never executable in Prompt 6`() {
        val plan = RafGitFsDiffPlanner.plan(
            "req", "profile", "owner/repo", "main", "base",
            RafGitFsDiffPlanner.diff(listOf(observed[2])),
            RafGitFsPlannedAction.PUSH_BRANCH,
            generatedAt = 42L
        )
        assertTrue(plan.requiresApproval)
        assertTrue(plan.steps.all { !it.executableNow })
        assertEquals(RafGitFsOperationRisk.REMOTE_BRANCH_WRITE, plan.steps.single().risk)
    }

    @Test
    fun `plan hash changes when action changes`() {
        val diffs = RafGitFsDiffPlanner.diff(listOf(observed[1]))
        val readPlan = RafGitFsDiffPlanner.plan(
            "req", "profile", "owner/repo", "main", "base", diffs,
            RafGitFsPlannedAction.NO_OP, 42L
        )
        val writePlan = RafGitFsDiffPlanner.plan(
            "req", "profile", "owner/repo", "main", "base", diffs,
            RafGitFsPlannedAction.CREATE_BRANCH, 42L
        )
        assertNotEquals(readPlan.planHash, writePlan.planHash)
    }

    @Test
    fun `sanitizer redacts common secrets`() {
        val value = RafGitFsCanonical.sanitize("Authorization=ghp_abcdefghijklmnopqrstuvwxyz123456 token=secret")!!
        assertFalse(value.contains("abcdefghijklmnopqrstuvwxyz"))
        assertFalse(value.contains("secret"))
        assertTrue(value.contains("REDACTED"))
    }
}
