package com.rafgittools.data.github

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionsControlPolicyTest {
    private val sha = "0123456789abcdef0123456789abcdef01234567"

    @Test fun `only explicit authorial repositories are selectable`() {
        assertTrue(ActionsControlPolicy.repositoryAllowed("RafGitTools"))
        assertTrue(ActionsControlPolicy.repositoryAllowed("termux-packages"))
        assertFalse(ActionsControlPolicy.repositoryAllowed("../other-repository"))
        assertFalse(ActionsControlPolicy.repositoryAllowed("random"))
    }

    @Test fun `inactive workflows cannot dispatch`() {
        assertTrue(ActionsControlPolicy.workflowAllowed(ActionsWorkflow(11, state = "active")))
        assertFalse(ActionsControlPolicy.workflowAllowed(ActionsWorkflow(11, state = "disabled_manually")))
        assertFalse(ActionsControlPolicy.workflowAllowed(ActionsWorkflow(-1, state = "active")))
    }

    @Test fun `running can cancel but completed cannot`() {
        assertTrue(ActionsControlPolicy.runCanCancel(ActionsRun(11, status = "queued")))
        assertTrue(ActionsControlPolicy.runCanCancel(ActionsRun(11, status = "in_progress")))
        assertFalse(ActionsControlPolicy.runCanCancel(ActionsRun(11, status = "completed")))
        assertFalse(ActionsControlPolicy.runCanCancel(ActionsRun(-1, status = "in_progress")))
    }

    @Test fun `only completed exact sha can rerun`() {
        assertTrue(ActionsControlPolicy.runCanRerun(ActionsRun(11, status = "completed", headSha = sha)))
        assertFalse(ActionsControlPolicy.runCanRerun(ActionsRun(11, status = "in_progress", headSha = sha)))
        assertFalse(ActionsControlPolicy.runCanRerun(ActionsRun(11, status = "completed", headSha = "TOKEN_VAZIO")))
        assertFalse(ActionsControlPolicy.runCanRerunFailedJobs(
            ActionsRun(11, status = "completed", conclusion = "success", headSha = sha)))
        assertTrue(ActionsControlPolicy.runCanRerunFailedJobs(
            ActionsRun(11, status = "completed", conclusion = "failure", headSha = sha)))
    }

    @Test fun `git refs reject ambiguity and path traversal`() {
        assertTrue(ActionsControlPolicy.isSafeRef("main"))
        assertTrue(ActionsControlPolicy.isSafeRef("feat/arm32-gate"))
        assertFalse(ActionsControlPolicy.isSafeRef("bad..ref"))
        assertFalse(ActionsControlPolicy.isSafeRef("bad//ref"))
        assertFalse(ActionsControlPolicy.isSafeRef("-dangerous"))
        assertFalse(ActionsControlPolicy.isSafeRef("main; rm -rf"))
        assertFalse(ActionsControlPolicy.isSafeRef(""))
    }
}
