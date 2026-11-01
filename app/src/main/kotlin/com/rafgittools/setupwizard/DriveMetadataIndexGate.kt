package com.rafgittools.setupwizard

import com.rafgittools.navigator.NovoexportSafInventory
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Metadata-only manifest of a user-selected SAF subtree.
 *
 * Does NOT persist content URIs, provider document IDs, account identity, access tokens,
 * conversation bodies, or source bytes. No network calls, hidden indexing or permission
 * requests. A user must choose both source and output in Android's system picker.
 */
object DriveMetadataIndexGate {
    const val MAX_DOCUMENTS = 5000
    const val MAX_INDEX_BYTES = 2 * 1024 * 1024

    private fun safePath(path: String): String {
        require(path.isNotBlank() && path.length <= 2048) { "TOKEN_VAZIO_UNSAFE_PATH" }
        require(path.split('/').none { it.isEmpty() || it == "." || it == ".." }) {
            "TOKEN_VAZIO_UNSAFE_PATH"
        }
        require(path.none { it.code < 32 }) { "TOKEN_VAZIO_UNSAFE_PATH" }
        return path
    }

    fun build(snapshot: NovoexportSafInventory.Result): ByteArray {
        require(snapshot.visitedDocuments in 0..MAX_DOCUMENTS) {
            "TOKEN_VAZIO_PARTIAL_INVENTORY_LIMIT"
        }
        require(snapshot.allFiles.size + snapshot.allDirectories.size <= MAX_DOCUMENTS) {
            "TOKEN_VAZIO_PARTIAL_INVENTORY_LIMIT"
        }
        val folders = snapshot.allDirectories.map { safePath(it.relativePath) }.sorted()
        val files = snapshot.allFiles.map {
            val path = safePath(it.relativePath)
            JSONObject()
                .put("path", path)
                .put("parent", path.substringBeforeLast('/', ""))
                .put("mime", it.mimeType)
                .put("size_bytes", it.sizeBytes ?: JSONObject.NULL)
        }.sortedBy { it.getString("path") }
        val candidates = snapshot.candidateFiles.map { safePath(it.relativePath) }.sorted()
        val edges = JSONArray()
        for (folder in folders) {
            edges.put(JSONObject()
                .put("kind", "CONTAINS")
                .put("from", folder.substringBeforeLast('/', ""))
                .put("to", folder))
        }
        for (file in files) {
            edges.put(JSONObject()
                .put("kind", "CONTAINS")
                .put("from", file.getString("parent"))
                .put("to", file.getString("path")))
        }
        val root = JSONObject()
            .put("schema", "rafgittools.drive-saf-metadata-index.v1")
            .put("source_scope", "USER_SELECTED_SAF_TREE_ONLY")
            .put("global_drive_coverage", "TOKEN_VAZIO_NOT_ATTESTED")
            .put("provider_sharing_visibility", "TOKEN_VAZIO_SAF_UNAVAILABLE")
            .put("source_bytes_read", false)
            .put("source_mutated", false)
            .put("content_indexed", false)
            .put("claim_allowed", false)
            .put("visited_documents", snapshot.visitedDocuments)
            .put("visited_directories", snapshot.visitedDirectories)
            .put("known_bytes", snapshot.knownTotalBytes)
            .put("unknown_size_files", snapshot.unknownSizeFiles)
            .put("directory_paths", JSONArray(folders))
            .put("files", JSONArray(files))
            .put("conversation_codex_candidates", JSONArray(candidates))
            .put("relations", edges)
        val data = (root.toString() + "\n").toByteArray(Charsets.UTF_8)
        require(data.size <= MAX_INDEX_BYTES) { "TOKEN_VAZIO_INDEX_PARTITION_REQUIRED" }
        return data
    }

    fun digest(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
