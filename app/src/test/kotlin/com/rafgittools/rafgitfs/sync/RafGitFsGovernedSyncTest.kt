package com.rafgittools.rafgitfs.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RafGitFsGovernedSyncTest {
    private val stable = listOf(
        RafGitFsObservedFile("a.txt", "base-a", "local-a", true, true, baseSha = "base-a"),
        RafGitFsObservedFile("b.txt", "same", "same", true, true, baseSha = "same"),
        RafGitFsObservedFile("c.txt", "remote-c", null, false, true, baseSha = "remote-c")
    )

    @Test
    fun `plan is deterministic for same evidence and timestamp`() {
        val diffs = RafGitFsDiffPlanner.diff(stable)
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
    fun `remote write without workspace remains blocked`() {
        val plan = RafGitFsDiffPlanner.plan(
            "req", "profile", "owner/repo", "main", "base",
            RafGitFsDiffPlanner.diff(listOf(stable[0])),
            RafGitFsPlannedAction.OPEN_PULL_REQUEST,
            generatedAt = 42L
        )
        assertTrue(plan.requiresApproval)
        assertEquals(4, plan.steps.size)
        assertTrue(plan.steps.all { !it.executableNow })
        assertTrue(plan.steps.all { it.risk == RafGitFsOperationRisk.REMOTE_BRANCH_WRITE })
    }

    @Test
    fun `workspace enables exact four step branch sequence`() {
        val plan = RafGitFsDiffPlanner.plan(
            "req", "profile", "owner/repo", "main", "base",
            RafGitFsDiffPlanner.diff(listOf(stable[0])),
            RafGitFsPlannedAction.OPEN_PULL_REQUEST,
            generatedAt = 42L,
            workspaceId = "workspace-123"
        )
        assertEquals(
            listOf(
                RafGitFsPlannedAction.CREATE_BRANCH,
                RafGitFsPlannedAction.CREATE_COMMIT,
                RafGitFsPlannedAction.PUSH_BRANCH,
                RafGitFsPlannedAction.OPEN_PULL_REQUEST
            ),
            plan.steps.map { it.action }
        )
        assertTrue(plan.steps.all { it.executableNow && it.requiresApproval })
        assertEquals("workspace-123", plan.workspaceId)
    }

    @Test
    fun `three way diff distinguishes local remote and both changed`() {
        val result = RafGitFsDiffPlanner.diff(
            listOf(
                RafGitFsObservedFile("local", "base", "local-new", true, true, baseSha = "base"),
                RafGitFsObservedFile("remote", "remote-new", "base", true, true, baseSha = "base"),
                RafGitFsObservedFile("both", "remote-new", "local-new", true, true, baseSha = "base")
            )
        ).associateBy { it.path }
        assertEquals(RafGitFsDiffItem.Kind.LOCAL_CHANGED, result.getValue("local").kind)
        assertEquals(RafGitFsDiffItem.Kind.REMOTE_CHANGED, result.getValue("remote").kind)
        assertEquals(RafGitFsDiffItem.Kind.BOTH_CHANGED, result.getValue("both").kind)
        assertFalse(result.getValue("local").conflict)
        assertFalse(result.getValue("remote").conflict)
        assertTrue(result.getValue("both").conflict)
    }

    @Test
    fun `plan hash changes with workspace identity`() {
        val diffs = RafGitFsDiffPlanner.diff(listOf(stable[0]))
        val first = RafGitFsDiffPlanner.plan(
            "req", "profile", "owner/repo", "main", "base", diffs,
            RafGitFsPlannedAction.OPEN_PULL_REQUEST, 42L, "workspace-a"
        )
        val second = RafGitFsDiffPlanner.plan(
            "req", "profile", "owner/repo", "main", "base", diffs,
            RafGitFsPlannedAction.OPEN_PULL_REQUEST, 42L, "workspace-b"
        )
        assertNotEquals(first.planHash, second.planHash)
    }

    @Test
    fun `sanitizer redacts common secrets`() {
        val value = RafGitFsCanonical.sanitize(
            "Authorization=ghp_abcdefghijklmnopqrstuvwxyz123456 token=secret"
        )!!
        assertFalse(value.contains("abcdefghijklmnopqrstuvwxyz"))
        assertFalse(value.contains("secret"))
        assertTrue(value.contains("REDACTED"))
    }
}
