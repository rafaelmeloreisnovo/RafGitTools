package com.rafgittools.setupwizard

import org.junit.Assert.*
import org.junit.Test

class HyperAtlasV4ManifoldBridgeV1Test {
    private val v = HyperAtlasV4Incremental
    private val b = HyperAtlasV4ManifoldBridgeV1
    private val scope = v.digest("content://fixture-private-tree")
    private fun row(parent: String = "root", id: String = "a", path: String = "file.wav", size: Long? = null) =
        HyperAtlasV4Incremental.Row(v.digest(scope + "\u0000" + id), v.digest(scope + "\u0000" + parent),
            "FILE", path, "audio/wav", size)
    private fun snapshot(ref: String, rows: List<HyperAtlasV4Incremental.Row> = listOf(row()),
                         visited: Set<String> = setOf("root"), state: String = "COMPLETE",
                         scopeOverride: String = scope,
                         countOverride: Long = rows.size.toLong()) =
        HyperAtlasV4ManifoldBridgeV1.Capture(ref, HyperAtlasV4Incremental.Checkpoint(scope = scopeOverride, pending = emptyList(), visited = visited,
            completedDirectories = visited.size, records = countOverride, state = state), rows)
    private fun bridge(a: HyperAtlasV4ManifoldBridgeV1.Capture = snapshot("cap-a"), z: HyperAtlasV4ManifoldBridgeV1.Capture = snapshot("cap-b"),
                       expected: String = "rev-a", observed: String = "rev-a") =
        b.preview(a, z, expected, observed)
    private fun blocked(code: String, fn: () -> Unit) {
        val error = runCatching(fn).exceptionOrNull()
        assertTrue("expected blocked: $code, got ${error?.message}", error is HyperAtlasV4ManifoldBridgeV1.Blocked)
        assertEquals(code, error?.message)
    }
    @Test fun sameMetadataNoWorkAndNeverClaimsFileContent() {
        val p = bridge()
        assertEquals(emptyList<String>(), p.manifest.changed_shards)
        assertEquals("TOKEN_VAZIO_NOT_TESTED", p.content_sha256)
        assertEquals("NOT_RUN", p.external_provider_cas)
        assertFalse(p.claim_allowed)
    }
    @Test fun changedMetadataIdentifiesOnlyChangedDirectory() {
        val p = bridge(z = snapshot("cap-b", listOf(row(path="renamed.wav"))))
        assertEquals(1, p.metadata_group_changes)
        assertEquals(1, p.manifest.changed_shards.size)
        assertEquals(1, p.manifest.shards.size)
        assertEquals("TOKEN_VAZIO_PROVIDER_REFRESH_NOT_VERIFIED", p.manifest.leader.change_detection)
    }
    @Test fun orderIndependentAndRepeatIdempotent() {
        val rows = listOf(row(id="a"), row(id="b", path="b.wav", size=0))
        val a = bridge(snapshot("cap-a", rows), snapshot("cap-b", rows.reversed()))
        val c = bridge(snapshot("cap-a", rows), snapshot("cap-b", rows))
        assertEquals(a, c)
        assertEquals(0, a.metadata_group_changes)
    }
    @Test fun preservesUnknownVsZero() {
        val p = bridge(z=snapshot("cap-b", listOf(row(size=0))))
        assertEquals(1, p.metadata_group_changes)
    }
    @Test fun refusesIncompleteCapture() = blocked("BLOCKED_PARTIAL_CAPTURE") {
        bridge(z=snapshot("cap-b", state="IN_PROGRESS"))
    }
    @Test fun refusesCounterMismatch() = blocked("BLOCKED_RECORD_COUNT_OR_BUDGET") {
        bridge(z=snapshot("cap-b", countOverride=5))
    }
    @Test fun refusesCrossScope() = blocked("BLOCKED_SCOPE_MISMATCH") {
        bridge(z=snapshot("cap-b", scopeOverride=v.digest("different")))
    }
    @Test fun refusesConcurrentLeaderChange() = blocked("BLOCKED_LOCAL_REVISION_MISMATCH") {
        bridge(observed="rev-b")
    }
    @Test fun refusesRefCollisionWithDivergentData() = blocked("BLOCKED_CAPTURE_REF_COLLISION") {
        bridge(z=snapshot("cap-a", listOf(row(path="changed"))))
    }
    @Test fun sameRefExactReplayIsHarmless() {
        assertEquals(0,bridge(z=snapshot("cap-a")).metadata_group_changes)
    }
    @Test fun refusesDuplicateRowIdentity() = blocked("BLOCKED_ROW_IDENTITY_OR_PARENT") {
        bridge(z=snapshot("cap-b", listOf(row(), row(path="b"))))
    }
    @Test fun refusesUnvisitedParent() = blocked("BLOCKED_ROW_IDENTITY_OR_PARENT") {
        bridge(z=snapshot("cap-b", listOf(row(parent="orphan"))))
    }
    @Test fun emptyDirectoryCreatesAnAddressableShard() {
        val p = bridge(z=snapshot("cap-b", listOf(row()), visited=setOf("root", "empty")))
        assertEquals(1, p.metadata_group_changes)
        assertEquals(2, p.manifest.shards.size)
    }
    @Test fun doesNotLeakPathsOrRawIds() {
        val p = bridge(z=snapshot("cap-b", listOf(row(path="SENSITIVE_PRIVATE_NAME"))))
        val text = p.toString()
        assertFalse(text.contains("SENSITIVE_PRIVATE_NAME"))
        assertFalse(text.contains("content://"))
    }
}
