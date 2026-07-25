package com.rafgittools.rafgitfs.assurance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RafGitFsCloseoutTest {
    @Test
    fun `unmeasured performance remains token vazio`() {
        val baseline = RafGitFsPerformanceBudget.unmeasuredBaseline()
        assertTrue(baseline.isNotEmpty())
        assertTrue(baseline.all { it.state == RafGitFsMetricState.TOKEN_VAZIO })
    }

    @Test
    fun `observed performance is evaluated without promotion`() {
        val pass = RafGitFsPerformanceBudget.assess(
            RafGitFsMetricSample("plan_1000_entries", 120.0, 150.0, 30, "JVM-CI")
        )
        val fail = RafGitFsPerformanceBudget.assess(
            RafGitFsMetricSample("plan_1000_entries", 180.0, 150.0, 30, "JVM-CI")
        )
        assertEquals(RafGitFsMetricState.PASS, pass.state)
        assertEquals(RafGitFsMetricState.FAIL, fail.state)
    }

    @Test
    fun `closeout preserves source evidence and open gaps`() {
        val snapshot = RafGitFsIndustrialCloseout.sourceSnapshot(generatedAt = 42L)
        assertEquals("RafGitFS-V1", snapshot.version)
        assertFalse(snapshot.claimAllowed)
        assertEquals(64, snapshot.closeoutHash.length)
        assertTrue(snapshot.unresolvedTokenVazio > 0)
        assertTrue(snapshot.capabilities.any {
            it.capability == "remote_delete" && it.evidence == RafGitFsEvidenceLevel.BLOCKED_BY_POLICY
        })
        assertTrue(snapshot.capabilities.any {
            it.capability == "android_device_execution" && it.evidence == RafGitFsEvidenceLevel.TOKEN_VAZIO
        })
    }

    @Test
    fun `closeout hash is deterministic and time bound`() {
        val first = RafGitFsIndustrialCloseout.sourceSnapshot(42L)
        val second = RafGitFsIndustrialCloseout.sourceSnapshot(42L)
        val later = RafGitFsIndustrialCloseout.sourceSnapshot(43L)
        assertEquals(first.closeoutHash, second.closeoutHash)
        assertNotEquals(first.closeoutHash, later.closeoutHash)
    }
}
