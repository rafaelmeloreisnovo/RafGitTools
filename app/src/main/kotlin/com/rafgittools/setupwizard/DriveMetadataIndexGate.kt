package com.rafgittools.setupwizard

import com.google.gson.GsonBuilder
import com.rafgittools.navigator.NovoexportSafInventory
import java.security.MessageDigest

/**
 * Metadata-only snapshot of the user-selected SAF tree; no source content is read.
 * The selected account, source URI, and capability-bearing document IDs are excluded.
 * A separate, explicit Android Save As action controls where this index is exported.
 */
object DriveMetadataIndexGate {
    const val MAX_DOCUMENTS = 5000
    const val MAX_INDEX_BYTES = 2 * 1024 * 1024
    private val gson = GsonBuilder().serializeNulls().disableHtmlEscaping().create()

    data class FileMetadata(
        val path: String,
        val parent: String,
        val mime: String,
        val size_bytes: Long?
    )
    data class TokenizedPath(
        val path: String,
        val tokens: List<String>,
        val path_components: Int,
        val semantic_pass_mask: Int,
        val kind_flags: Int,
        val size_known: Boolean,
        val parent_known: Boolean
    )
    data class Relation(val kind: String = "CONTAINS", val from: String, val to: String)
    data class Manifest(
        val schema: String = "rafgittools.drive-saf-metadata-index.v2",
        val source_scope: String = "USER_SELECTED_SAF_TREE_ONLY",
        val global_drive_coverage: String = "TOKEN_VAZIO_NOT_ATTESTED",
        val provider_sharing_visibility: String = "TOKEN_VAZIO_SAF_UNAVAILABLE",
        val source_bytes_read: Boolean = false,
        val source_mutated: Boolean = false,
        val content_indexed: Boolean = false,
        val claim_allowed: Boolean = false,
        val visited_documents: Int,
        val visited_directories: Int,
        val known_bytes: Long,
        val unknown_size_files: Int,
        val directory_paths: List<String>,
        val files: List<FileMetadata>,
        val semantic_token_paths: List<TokenizedPath>,
        val conversation_codex_candidates: List<String>,
        val relations: List<Relation>
    )

    private fun safePath(path: String): String {
        require(path.isNotBlank() && path.length <= 2048) { "TOKEN_VAZIO_UNSAFE_PATH" }
        require(path.split('/').none { it.isEmpty() || it == "." || it == ".." }) {
            "TOKEN_VAZIO_UNSAFE_PATH"
        }
        require(path.none { it.code < 32 }) { "TOKEN_VAZIO_UNSAFE_PATH" }
        return path
    }

    fun build(snapshot: NovoexportSafInventory.Result): ByteArray {
        require(snapshot.visitedDocuments in 0..MAX_DOCUMENTS &&
                snapshot.allFiles.size + snapshot.allDirectories.size <= MAX_DOCUMENTS) {
            "TOKEN_VAZIO_PARTIAL_INVENTORY_LIMIT"
        }
        val folders = snapshot.allDirectories.map { safePath(it.relativePath) }.sorted()
        val tokenizedDirectories = snapshot.allDirectories.map {
            val path = safePath(it.relativePath)
            require(it.semanticPassMask == 0x1f && it.pathComponents > 0) {
                "TOKEN_VAZIO_NATIVE_ANALYSIS_INCOMPLETE"
            }
            TokenizedPath(path, it.semanticTokens, it.pathComponents,
                it.semanticPassMask, it.semanticKindFlags,
                size_known = false, parent_known = it.parentDocumentId != null)
        }
        val files = snapshot.allFiles.map {
            val path = safePath(it.relativePath)
            require(it.sizeBytes == null || it.sizeBytes >= 0) { "TOKEN_VAZIO_UNSAFE_SIZE" }
            require(it.semanticPassMask == 0x1f && it.pathComponents > 0) {
                "TOKEN_VAZIO_NATIVE_ANALYSIS_INCOMPLETE"
            }
            FileMetadata(path, path.substringBeforeLast('/', ""), it.mimeType, it.sizeBytes)
        }.sortedBy { it.path }
        val tokenizedFiles = snapshot.allFiles.map {
            TokenizedPath(safePath(it.relativePath), it.semanticTokens, it.pathComponents,
                it.semanticPassMask, it.semanticKindFlags,
                size_known = it.sizeBytes != null,
                parent_known = it.parentDocumentId != null)
        }
        val candidates = snapshot.candidateFiles.map { safePath(it.relativePath) }.sorted()
        val edges = folders.map {
            Relation(from = it.substringBeforeLast('/', ""), to = it)
        } + files.map { Relation(from = it.parent, to = it.path) }
        val envelope = Manifest(
            visited_documents = snapshot.visitedDocuments,
            visited_directories = snapshot.visitedDirectories,
            known_bytes = snapshot.knownTotalBytes,
            unknown_size_files = snapshot.unknownSizeFiles,
            directory_paths = folders,
            files = files,
            semantic_token_paths = (tokenizedDirectories + tokenizedFiles).sortedBy { it.path },
            conversation_codex_candidates = candidates,
            relations = edges
        )
        val data = (gson.toJson(envelope) + "\n").toByteArray(Charsets.UTF_8)
        require(data.size <= MAX_INDEX_BYTES) { "TOKEN_VAZIO_INDEX_PARTITION_REQUIRED" }
        return data
    }

    fun digest(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
