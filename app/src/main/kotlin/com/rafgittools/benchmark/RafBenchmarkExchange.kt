package com.rafgittools.benchmark

import org.json.JSONObject
import java.security.MessageDigest

/**
 * JSON is untrusted input. Parsing validates a *proposal*, never a signed
 * approval or permission to run. V1 is accepted read-only for compatibility.
 * V2 binds one catalog route and one executable allowlisted workload.
 */
object RafBenchmarkExchange {
    const val MAX_BYTES = 16 * 1024
    const val SCHEMA = "rafaelia.rafbenchmark.work-proposal.v1"
    const val SCHEMA_V2 = "rafaelia.rafbenchmark.work-proposal.v2"
    private val producers = setOf(
        "rafaelmeloreisnovo/RafGitTools",
        "rafaelmeloreisnovo/Mapa",
        "instituto-Rafael/relativity-living-light"
    )
    private val baseKeys = setOf("schema", "job_id", "source_repository",
        "source_revision", "workers", "buffer_kib", "input", "approval",
        "claim_allowed")
    private val v2Keys = baseKeys + setOf("module_id", "workload", "execution_target")
    private val runnableReferences = (76..83).map { "K%03d".format(it) }.toSet()

    data class Proposal(
        val id: String,
        val producer: String,
        val sourceRevision: String,
        val workers: Int,
        val bufferKiB: Int,
        val digest: String,
        val moduleId: String = "K076",
        val workload: RafBenchmarkCore.Workload = RafBenchmarkCore.Workload.STREAM_WRITE,
        val authorityVerified: Boolean = false
    )

    fun parse(bytes: ByteArray): Proposal {
        require(bytes.isNotEmpty() && bytes.size <= MAX_BYTES) { "BLOCKED_INPUT_SIZE" }
        val doc = JSONObject(String(bytes, Charsets.UTF_8))
        val schema = doc.getString("schema")
        require(schema == SCHEMA || schema == SCHEMA_V2) { "BLOCKED_SCHEMA" }
        val actual = doc.keys().asSequence().toSet()
        require(actual == if (schema == SCHEMA) baseKeys else v2Keys) { "BLOCKED_FIELDS" }
        val id = doc.getString("job_id")
        require(Regex("[A-Za-z0-9_.-]{1,64}").matches(id)) { "BLOCKED_JOB_ID" }
        val producer = doc.getString("source_repository")
        require(producer in producers) { "BLOCKED_SOURCE_AUTHORITY" }
        val revision = doc.getString("source_revision")
        require(Regex("[a-fA-F0-9]{40}").matches(revision)) { "BLOCKED_UNPINNED_SOURCE" }
        require(doc.getString("input") == "SYNTHETIC_ONLY") { "BLOCKED_DATA_CLASS" }
        require(doc.getString("approval") == "REQUIRES_HUMAN_CLICK") { "BLOCKED_APPROVAL" }
        require(!doc.getBoolean("claim_allowed")) { "BLOCKED_CLAIM" }
        val workers = doc.getInt("workers")
        val buffer = doc.getInt("buffer_kib")
        RafBenchmarkCore.validate(workers, buffer)
        val moduleId = if (schema == SCHEMA) "K076" else doc.getString("module_id")
        require(moduleId in runnableReferences &&
            RafApplicationsCatalog.byId(moduleId) != null) { "BLOCKED_MODULE" }
        require(if (schema == SCHEMA) true else
            doc.getString("execution_target") == "LOCAL_ANDROID") { "BLOCKED_TARGET" }
        val workload = if (schema == SCHEMA) RafBenchmarkCore.Workload.STREAM_WRITE
            else runCatching { RafBenchmarkCore.Workload.valueOf(doc.getString("workload")) }
                .getOrElse { throw IllegalArgumentException("BLOCKED_WORKLOAD") }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        return Proposal(id, producer, revision.lowercase(), workers, buffer, digest,
            moduleId, workload, authorityVerified = false)
    }
}
