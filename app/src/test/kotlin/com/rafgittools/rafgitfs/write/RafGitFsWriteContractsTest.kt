package com.rafgittools.rafgitfs.write

import com.rafgittools.rafgitfs.sync.RafGitFsDiffItem
import com.rafgittools.rafgitfs.sync.RafGitFsDiffPlanner
import com.rafgittools.rafgitfs.sync.RafGitFsObservedFile
import com.rafgittools.rafgitfs.sync.RafGitFsPlannedAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RafGitFsWriteContractsTest {
    @Test
    fun `write DTO defaults are non force and draft`() {
        assertFalse(RafGitFsUpdateRefRequest("abc").force)
        assertTrue(
            RafGitFsOpenPullRequestRequest(
                title = "t",
                body = "b",
                head = "rafgitfs/main-1234",
                base = "main"
            ).draft
        )
    }

    @Test
    fun `local edit against stable base is not a conflict`() {
        val item = RafGitFsDiffPlanner.diff(
            listOf(
                RafGitFsObservedFile(
                    path = "docs/a.md",
                    remoteSha = "base-sha",
                    localSha = "local-sha",
                    localExists = true,
                    remoteExists = true,
                    baseSha = "base-sha"
                )
            )
        ).single()
        assertEquals(RafGitFsDiffItem.Kind.LOCAL_CHANGED, item.kind)
        assertFalse(item.conflict)
    }

    @Test
    fun `workspace publish is exactly four governed steps`() {
        val plan = RafGitFsDiffPlanner.plan(
            requestId = "request-1",
            profileId = "profile-1",
            repositoryFullName = "owner/repo",
            refName = "main",
            baseCommitSha = "base-commit",
            diffs = emptyList(),
            requestedAction = RafGitFsPlannedAction.OPEN_PULL_REQUEST,
            generatedAt = 42L,
            workspaceId = "workspace-1"
        )
        assertEquals(4, plan.steps.size)
        assertEquals(RafGitFsPlannedAction.CREATE_BRANCH, plan.steps[0].action)
        assertEquals(RafGitFsPlannedAction.CREATE_COMMIT, plan.steps[1].action)
        assertEquals(RafGitFsPlannedAction.PUSH_BRANCH, plan.steps[2].action)
        assertEquals(RafGitFsPlannedAction.OPEN_PULL_REQUEST, plan.steps[3].action)
        assertTrue(plan.steps.all { it.requiresApproval && it.executableNow })
        assertFalse(plan.claimAllowed)
    }
}
