package com.rafgittools.setupwizard

import java.security.MessageDigest

/**
 * Read-only, metadata-only bridge from two DIFFERENT completed V4 captures to the
 * Mapa incremental planner manifest. Does not rescan SAF, write Drive or perform CAS.
 * Provider changes can occur without a metadata change; never claim byte equivalence.
 * The handoff is a BLOCKED candidate until external capture provenance is verified.
 */
object HyperAtlasV4ManifoldBridgeV1 {
    const val MANIFEST_SCHEMA = "RAFAELIA_INCREMENTAL_MANIFOLD_ROUTER_V1"
    private const val MAX_ROWS = 8192
    private const val MAX_DIRS = 4096
    private val HEX64 = Regex("[a-f0-9]{64}")
    private val SAFE_REF = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,120}")

    class Blocked(code: String) : IllegalArgumentException(code)

    data class Capture(
        val captureRef: String,
        val checkpoint: HyperAtlasV4Incremental.Checkpoint,
        val rows: List<HyperAtlasV4Incremental.Row>
    )
    data class Leader(
        val source_ref: String,
        val expected_revision: String,
        val observed_revision: String,
        val change_detection: String = "TOKEN_VAZIO_PROVIDER_REFRESH_NOT_VERIFIED"
    )
    data class Shard(
        val id: String,
        val source_ref: String,
        val revision: String,
        val depends_on: List<String> = emptyList()
    )
    data class Manifest(
        val schema: String = MANIFEST_SCHEMA,
        val leader: Leader,
        val shards: List<Shard>,
        val changed_shards: List<String>,
        val tasks: List<String> = emptyList()
    )
    data class Handoff(
        val manifest: Manifest,
        val previous_capture_ref: String,
        val current_capture_ref: String,
        val metadata_group_changes: Int,
        val state: String = "SCOPED_METADATA_HANDOFF_ONLY",
        val external_provider_cas: String = "NOT_RUN",
        val writer_exclusivity: String = "TOKEN_VAZIO_NOT_VERIFIED",
        val content_sha256: String = "TOKEN_VAZIO_NOT_TESTED",
        val claim_allowed: Boolean = false
    )

    private fun validRef(value: String): Boolean = SAFE_REF.matches(value)
    private fun sha256(): MessageDigest = MessageDigest.getInstance("SHA-256")

    /** Length-prefix every field to avoid separator ambiguity. */
    private fun MessageDigest.field(value: String) {
        val b = value.toByteArray(Charsets.UTF_8)
        val n = b.size
        update(byteArrayOf((n ushr 24).toByte(), (n ushr 16).toByte(),
            (n ushr 8).toByte(), n.toByte()))
        update(b)
    }
    private fun MessageDigest.hex(): String = digest().joinToString("") { "%02x".format(it) }

    private fun rowFingerprint(row: HyperAtlasV4Incremental.Row): String = sha256().apply {
        field(row.key); field(row.parentKey); field(row.type)
        field(row.path); field(row.mime)
        // Explicitly preserve NULL versus 0.
        field(if (row.sizeBytes == null) "NULL" else "INT:${row.sizeBytes}")
    }.hex()

    private fun captureGroups(capture: Capture): Map<String, String> {
        if (!validRef(capture.captureRef)) throw Blocked("BLOCKED_CAPTURE_REF")
        val cp = capture.checkpoint
        if (cp.schema != HyperAtlasV4Incremental.SCHEMA || !HEX64.matches(cp.scope))
            throw Blocked("BLOCKED_CHECKPOINT_SCHEMA_SCOPE")
        if (cp.state != "COMPLETE" || cp.pending.isNotEmpty())
            throw Blocked("BLOCKED_PARTIAL_CAPTURE")
        if (cp.visited.isEmpty() || cp.visited.size > MAX_DIRS ||
            cp.completedDirectories != cp.visited.size)
            throw Blocked("BLOCKED_DIRECTORY_COUNT")
        if (cp.records != capture.rows.size.toLong() || capture.rows.size > MAX_ROWS)
            throw Blocked("BLOCKED_RECORD_COUNT_OR_BUDGET")
        if (cp.visited.any { it.isEmpty() }) throw Blocked("BLOCKED_EMPTY_PROVIDER_ID")

        // Raw provider document IDs never leave this method or the app-private storage.
        val dirKeys = cp.visited.map { HyperAtlasV4Incremental.digest(cp.scope + "\u0000" + it) }.toSet()
        val grouped = dirKeys.associateWith { ArrayList<String>() }.toMutableMap()
        val unique = HashSet<String>()
        for (r in capture.rows) {
            if (!HEX64.matches(r.key) || !HEX64.matches(r.parentKey) ||
                r.parentKey !in grouped || !unique.add(r.key))
                throw Blocked("BLOCKED_ROW_IDENTITY_OR_PARENT")
            if (r.type != "FILE" && r.type != "DIRECTORY") throw Blocked("BLOCKED_ROW_TYPE")
            if (r.path.isEmpty() || r.path.length > 4096 || r.mime.isEmpty() ||
                (r.sizeBytes != null && r.sizeBytes < 0)) throw Blocked("BLOCKED_ROW_METADATA")
            grouped.getValue(r.parentKey).add(rowFingerprint(r))
        }
        return grouped.mapValues { (_, fingerprints) ->
            sha256().apply {
                field("rafgittools.hyperatlas.metadata-groups/v1")
                fingerprints.sorted().forEach { field(it) }
            }.hex()
        }
    }

    /**
     * This is LOCAL equality of caller-supplied revisions, NOT provider-atomic CAS.
     * The external writer must read provider revision, lock/lease, CAS and read back.
     */
    fun preview(
        previous: Capture,
        current: Capture,
        expectedLeaderRevision: String,
        observedLeaderRevision: String
    ): Handoff {
        if (!validRef(expectedLeaderRevision) || !validRef(observedLeaderRevision))
            throw Blocked("TOKEN_VAZIO_LEADER_REVISION")
        if (expectedLeaderRevision != observedLeaderRevision)
            throw Blocked("BLOCKED_LOCAL_REVISION_MISMATCH")
        if (previous.checkpoint.scope != current.checkpoint.scope)
            throw Blocked("BLOCKED_SCOPE_MISMATCH")
        val before = captureGroups(previous)
        val after = captureGroups(current)
        if (previous.captureRef == current.captureRef && before != after)
            throw Blocked("BLOCKED_CAPTURE_REF_COLLISION")
        val allKeys = (before.keys + after.keys).toSortedSet()
        val changed = allKeys.filter { before[it] != after[it] }
        val scope = current.checkpoint.scope
        val manifest = Manifest(
            leader = Leader("rafgittools:hyperatlas-v4:$scope", expectedLeaderRevision,
                observedLeaderRevision),
            shards = allKeys.map { key ->
                Shard("safdir:$key", "rafgittools:hyperatlas-v4:$scope#dir=$key",
                    after[key] ?: "TOMBSTONE:${before.getValue(key)}")
            },
            changed_shards = changed.map { "safdir:$it" }
        )
        return Handoff(manifest, previous.captureRef, current.captureRef, changed.size)
    }
}
