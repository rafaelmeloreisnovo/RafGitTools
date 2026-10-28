package com.rafgittools.bridge

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.File
import java.io.IOException

/**
 * GitHub private tree metadata -> app-private index; no conversation body copied.
 * Caller must verify private repository via authenticated GitHub API.
 * Export ONLY through the existing user-selected Android SAF tree gate.
 */
object PrivateGitHubTreeIndexGate {
    private val REPOSITORY = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
    private val SHA = Regex("^[0-9a-f]{40}$")

    data class Snapshot(
        val indexFile: File,
        val commitSha: String,
        val fileCount: Int,
        val byteCount: Long,
        val largeBlobCount: Int
    )

    @Throws(IllegalArgumentException::class)
    fun buildIndex(
        repository: String,
        commitSha: String,
        expectedTreeSha: String,
        githubTree: JsonObject
    ): JsonObject {
        require(REPOSITORY.matches(repository) && repository.split('/').none { it == "." || it == ".." }) {
            "Invalid repository name"
        }
        require(SHA.matches(commitSha) && SHA.matches(expectedTreeSha)) { "Invalid exact SHA" }
        require(githubTree.get("truncated")?.asBoolean == false) { "GitHub tree incomplete" }
        require(githubTree.get("sha")?.asString == expectedTreeSha) { "GitHub tree identity mismatch" }
        val raw = githubTree.getAsJsonArray("tree") ?: error("Missing Git tree entries")
        require(raw.size() <= 100000) { "Git tree size bound exceeded" }
        val records = JsonArray()
        val seen = HashSet<String>()
        var total = 0L
        var large = 0
        for (entry in raw) {
            require(entry.isJsonObject) { "Invalid tree entry" }
            val item = entry.asJsonObject
            if (item.get("type")?.asString != "blob") continue
            val path = item.get("path")?.asString ?: error("Missing path")
            val objectSha = item.get("sha")?.asString ?: error("Missing object SHA")
            val size = item.get("size")?.asLong ?: error("Missing byte size")
            require(
                path.isNotBlank() && !path.startsWith("/") &&
                    !path.contains("\\") && path.split("/").none { it.isEmpty() || it == "." || it == ".." } &&
                    path.none { it.code < 32 } && seen.add(path)
            ) { "Invalid or duplicate path" }
            require(SHA.matches(objectSha) && size >= 0) { "Invalid blob entry" }
            total = Math.addExact(total, size)
            if (size > 2L * 1024L * 1024L) large++
            val r = JsonObject()
            r.addProperty("path", path)
            r.addProperty("bytes", size)
            r.addProperty("git_blob_sha1", objectSha)
            r.addProperty("privacy", "C3_RESTRICTED_METADATA")
            records.add(r)
        }
        require(records.size() > 0) { "Empty file tree" }
        val out = JsonObject()
        out.addProperty("schema", "rafgittools.github_private_tree_index.v1")
        out.addProperty("source", repository)
        out.addProperty("commit_sha", commitSha)
        out.addProperty("tree_sha", expectedTreeSha)
        out.addProperty("state", "PRIVATE_METADATA_ONLY")
        out.addProperty("classification", "C3_RESTRICTED_METADATA")
        out.addProperty("file_count", records.size())
        out.addProperty("byte_count", total)
        out.addProperty("oversize_not_scanned", large)
        out.addProperty("raw_conversations_copied", false)
        out.addProperty("content_sha256_verified", false)
        out.addProperty("claim_allowed", false)
        out.add("entries", records)
        return out
    }

    @Throws(IOException::class)
    fun storePrivate(context: Context, index: JsonObject): Snapshot {
        require(index.get("schema")?.asString == "rafgittools.github_private_tree_index.v1")
        val sha = index.get("commit_sha")?.asString ?: error("Missing commit")
        require(SHA.matches(sha))
        val dir = File(context.filesDir, "private-github-tree-index")
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Cannot create app-private index dir")
        val final = File(dir, "github-tree-${sha}.json")
        val stage = File(dir, "github-tree-${sha}.part")
        // No logger and no public artifact. Preserve previous final until a complete write exists.
        stage.outputStream().buffered(64 * 1024).use { out ->
            out.write(index.toString().toByteArray(Charsets.UTF_8))
            out.flush()
        }
        if (!stage.renameTo(final)) {
            stage.delete()
            throw IOException("Cannot commit private index atomically")
        }
        return Snapshot(
            indexFile = final,
            commitSha = sha,
            fileCount = index.get("file_count").asInt,
            byteCount = index.get("byte_count").asLong,
            largeBlobCount = index.get("oversize_not_scanned").asInt
        )
    }
}
