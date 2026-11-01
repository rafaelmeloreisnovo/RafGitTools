package com.rafgittools.navigator

import android.content.Context
import com.google.gson.GsonBuilder
import com.rafgittools.domain.model.github.GithubRepository
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Offline, user-triggered GitHub LIST snapshot. Metadata, NOT cloned Git content.
 * App-private, append-only, no tokens, URLs with capabilities, messages or network writes.
 */
class LocalGithubInventoryStore(context: Context) {
    private val directory = File(context.filesDir, "github-inventory")
    private val gson = GsonBuilder().disableHtmlEscaping().serializeNulls().create()

    data class Repo(
        val full_name: String,
        val owner: String,
        val description: String?,
        val visibility: String,
        val language: String?,
        val default_branch: String,
        val is_fork: Boolean,
        val stars: Int,
        val forks: Int,
        val open_issues_reported: Int,
        val updated_at: String,
        val files: String = "TOKEN_VAZIO_NOT_SCANNED",
        val workflows: String = "TOKEN_VAZIO_NOT_SCANNED",
        val checks: String = "TOKEN_VAZIO_NOT_VERIFIED",
        val dependencies: String = "TOKEN_VAZIO_NOT_DERIVED"
    )
    data class Snapshot(
        val schema: String = "rafgittools.local-github-inventory.v1",
        val producer: String = "GITHUB_AUTHENTICATED_REPOSITORY_LIST",
        val profile: String,
        val observed_epoch_ms: Long,
        val enumeration: String,
        val observed_repositories: Int,
        val all_accounts: String = "TOKEN_VAZIO_NOT_ATTESTED",
        val cloned_content: Boolean = false,
        val claim_allowed: Boolean = false,
        val not_indexed_facets: List<String> = listOf(
            "FILE_TREE", "BRANCHES", "TAGS", "WORKFLOWS",
            "WORKFLOW_RUNS", "COMMIT_HISTORY", "PULL_REQUESTS",
            "ISSUE_DETAILS", "LICENSE_PROVENANCE", "DEPENDENCY_GRAPH",
            "SECURITY_ADVISORIES", "BRANCH_PROTECTION", "RELEASE_ARTIFACTS",
            "INDEPENDENT_RUNTIME_TESTS"
        ),
        val repositories: List<Repo>
    )
    data class Saved(val snapshot: Snapshot, val sha256: String, val bytes: Long)

    companion object {
        const val MAX_REPOS = 5000
        const val MAX_BYTES = 2 * 1024 * 1024
        const val MAX_HISTORY = 128
        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
            .digest(bytes).joinToString("") { "%02x".format(it) }

        fun observe(
            profile: String, rows: List<GithubRepository>, complete: Boolean, at: Long
        ): Snapshot {
            require(profile.matches(Regex("[A-Za-z0-9-]{1,39}"))) { "TOKEN_VAZIO_PROFILE" }
            require(rows.size <= MAX_REPOS && at > 0L) { "TOKEN_VAZIO_INVENTORY_LIMIT" }
            val records = rows.distinctBy { it.id }.map {
                require(it.fullName.matches(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+"))) {
                    "TOKEN_VAZIO_REPOSITORY_NAME"
                }
                Repo(
                    full_name = it.fullName, owner = it.owner.login,
                    description = it.description?.take(512),
                    visibility = if (it.isPrivate) "PRIVATE" else "PUBLIC",
                    language = it.language, default_branch = it.defaultBranch,
                    is_fork = it.isFork, stars = it.stargazersCount,
                    forks = it.forksCount, open_issues_reported = it.openIssuesCount,
                    updated_at = it.updatedAt
                )
            }.sortedBy { it.full_name.lowercase() }
            return Snapshot(
                profile = profile, observed_epoch_ms = at,
                enumeration = if (complete) "OBSERVED_LIST_PAGES_COMPLETE" else "TOKEN_VAZIO_PARTIAL",
                observed_repositories = records.size, repositories = records
            )
        }
    }

    /** Export only a locally verified snapshot, never a token or remote blob. */
    fun verifiedJson(saved: Saved): ByteArray {
        validate(saved.snapshot)
        val payload = (gson.toJson(saved.snapshot) + "\n").toByteArray(Charsets.UTF_8)
        require(payload.size <= MAX_BYTES && sha256(payload) == saved.sha256) {
            "TOKEN_VAZIO_EXPORT_HASH_MISMATCH"
        }
        return payload
    }

    fun save(snapshot: Snapshot): Saved {
        validate(snapshot)
        val bytes = (gson.toJson(snapshot) + "\n").toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES) { "TOKEN_VAZIO_INDEX_SIZE_LIMIT" }
        require(directory.isDirectory || directory.mkdirs()) { "TOKEN_VAZIO_LOCAL_DIRECTORY" }
        require((directory.listFiles()?.count { it.name.endsWith(".json") } ?: 0) < MAX_HISTORY) {
            "TOKEN_VAZIO_HISTORY_REVIEW_REQUIRED"
        }
        val hash = sha256(bytes)
        val profileHash = sha256(snapshot.profile.toByteArray(Charsets.UTF_8)).take(12)
        val name = "inventory-" + profileHash + "-" + snapshot.observed_epoch_ms +
            "-" + hash.take(16) + ".json"
        val final = File(directory, name)
        require(!final.exists()) { "TOKEN_VAZIO_DUPLICATE" }
        val part = File(directory, "." + name + ".part")
        require(part.createNewFile()) { "TOKEN_VAZIO_CONCURRENT_WRITE" }
        try {
            FileOutputStream(part).use { it.write(bytes); it.flush(); it.fd.sync() }
            require(sha256(part.readBytes()) == hash) { "TOKEN_VAZIO_WRITE_INTEGRITY" }
            require(part.renameTo(final)) { "TOKEN_VAZIO_ATOMIC_RENAME" }
            require(sha256(final.readBytes()) == hash) { "TOKEN_VAZIO_READBACK_INTEGRITY" }
            return Saved(snapshot, hash, bytes.size.toLong())
        } finally {
            if (part.exists()) part.delete()
        }
    }

    fun latest(): Saved? {
        if (!directory.isDirectory) return null
        val files = directory.listFiles()?.filter { it.name.endsWith(".json") }.orEmpty()
        require(files.size <= MAX_HISTORY) { "TOKEN_VAZIO_HISTORY_REVIEW_REQUIRED" }
        for (file in files.sortedByDescending { it.lastModified() }) {
            if (file.length() !in 1L..MAX_BYTES.toLong()) continue
            val bytes = file.readBytes()
            val digest = sha256(bytes)
            if (!file.name.endsWith("-" + digest.take(16) + ".json")) continue
            val record = runCatching { gson.fromJson(String(bytes, Charsets.UTF_8), Snapshot::class.java) }
                .getOrNull() ?: continue
            if (runCatching { validate(record) }.isFailure) continue
            return Saved(record, digest, bytes.size.toLong())
        }
        return null
    }

    private fun validate(s: Snapshot) {
        require(s.schema == "rafgittools.local-github-inventory.v1")
        require(s.producer == "GITHUB_AUTHENTICATED_REPOSITORY_LIST")
        require(s.profile.matches(Regex("[A-Za-z0-9-]{1,39}")))
        require(s.observed_repositories == s.repositories.size && s.repositories.size <= MAX_REPOS)
        require(s.enumeration in setOf("OBSERVED_LIST_PAGES_COMPLETE", "TOKEN_VAZIO_PARTIAL"))
        require(!s.cloned_content && !s.claim_allowed)
        require(s.all_accounts == "TOKEN_VAZIO_NOT_ATTESTED")
        require(s.repositories.all {
            it.full_name.matches(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) &&
            it.visibility in setOf("PRIVATE", "PUBLIC") &&
            it.files == "TOKEN_VAZIO_NOT_SCANNED" &&
            it.workflows == "TOKEN_VAZIO_NOT_SCANNED" &&
            it.checks == "TOKEN_VAZIO_NOT_VERIFIED" &&
            it.dependencies == "TOKEN_VAZIO_NOT_DERIVED"
        })
    }
}
