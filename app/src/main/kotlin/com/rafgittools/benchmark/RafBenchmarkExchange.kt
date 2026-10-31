package com.rafgittools.benchmark

import org.json.JSONObject
import java.security.MessageDigest

/**
 * Deliberately inert Drive/SAF exchange envelope. A document may SUGGEST only
 * bounded synthetic-test parameters; it cannot issue commands, inject code,
 * authenticate itself, grant consent, select recipients or elevate secrets.
 */
object RafBenchmarkExchange {
    const val MAX_BYTES = 16 * 1024
    const val SCHEMA = "rafaelia.rafbenchmark.work-proposal.v1"
    private val producers = setOf(
        "rafaelmeloreisnovo/RafGitTools",
        "rafaelmeloreisnovo/Mapa",
        "instituto-Rafael/relativity-living-light"
    )
    data class Proposal(
        val id: String,
        val producer: String,
        val sourceRevision: String,
        val workers: Int,
        val bufferKiB: Int,
        val digest: String
    )

    fun parse(bytes: ByteArray): Proposal {
        require(bytes.isNotEmpty() && bytes.size <= MAX_BYTES) { "BLOCKED_INPUT_SIZE" }
        val doc = JSONObject(String(bytes, Charsets.UTF_8))
        val keys = listOf("schema", "job_id", "source_repository", "source_revision",
            "workers", "buffer_kib", "input", "approval", "claim_allowed")
        require(doc.length() == keys.size && keys.all(doc::has)) { "BLOCKED_FIELDS" }
        require(doc.getString("schema") == SCHEMA) { "BLOCKED_SCHEMA" }
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
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        return Proposal(id, producer, revision.lowercase(), workers, buffer, digest)
    }
}
