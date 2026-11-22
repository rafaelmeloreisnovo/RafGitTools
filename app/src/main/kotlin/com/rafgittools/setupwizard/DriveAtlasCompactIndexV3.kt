package com.rafgittools.setupwizard

import com.google.gson.GsonBuilder
import com.rafgittools.navigator.NovoexportSafInventory
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/**
 * HyperAtlas v3: one compact object per JSONL line; no duplicated parallel tables.
 * Only metadata from the already-authorized SAF tree. No archive member reads.
 *
 * writeShard MUST return SHA-256 of independently read-back bytes.
 * The manifest is returned ONLY after all shards have been committed and verified.
 * This does not turn an SAF tree into an account-wide Drive snapshot.
 */
object DriveAtlasCompactIndexV3 {
    const val SCHEMA = "rafgittools.hyperatlas.saf-compact/v3"
    const val DEFAULT_SHARD_BYTES = 256 * 1024
    const val MAX_SHARD_BYTES = 512 * 1024
    const val MAX_PARTS = 256
    const val MAX_TOTAL_INDEX_BYTES = 32L * 1024L * 1024L
    private val gson = GsonBuilder().serializeNulls().disableHtmlEscaping().create()

    data class Row(
        val type: String,
        val path: String,
        val parent: String,
        val mime: String?,
        val size_bytes: Long?,
        val archive_kind: String?,
        val tokens: List<String>,
        val semantic_pass_mask: Int,
        val kind_flags: Int,
        val parent_known: Boolean
    )

    data class Part(
        val ordinal: Int,
        val name: String,
        val records: Int,
        val bytes: Int,
        val sha256: String
    )

    data class Manifest(
        val schema: String = SCHEMA,
        val source_scope: String = "USER_SELECTED_SAF_TREE_ONLY",
        val global_drive_coverage: String = "TOKEN_VAZIO_NOT_ATTESTED",
        val relation_model: String = "CONTAINS(parent,path)",
        val source_bytes_read: Boolean = false,
        val archive_members_read: Boolean = false,
        val source_mutated: Boolean = false,
        val claim_allowed: Boolean = false,
        val state: String = "SHARDED_METADATA_INDEX_READBACK_VERIFIED",
        val records: Int,
        val folders: Int,
        val files: Int,
        val archive_candidates: Int,
        val known_file_bytes: Long,
        val known_archive_bytes: Long,
        val unknown_size_files: Int,
        val index_bytes: Long,
        val index_sha256: String,
        val parts: List<Part>
    )

    private fun sha256(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(data)
            .joinToString("") { "%02x".format(it) }

    private fun pathOf(raw: String): String {
        require(raw.isNotBlank() && raw.length <= 2048 &&
            raw.none { it.code < 32 || it == '\\' } &&
            raw.split('/').none { it.isEmpty() || it == "." || it == ".." }) {
            "HYPERATLAS_UNSAFE_RELATIVE_PATH"
        }
        return raw
    }

    fun build(
        snapshot: NovoexportSafInventory.Result,
        shardLimitBytes: Int = DEFAULT_SHARD_BYTES,
        writeShardAndReadback: (name: String, data: ByteArray) -> String
    ): ByteArray {
        require(shardLimitBytes in 512..MAX_SHARD_BYTES) { "HYPERATLAS_SHARD_LIMIT" }
        require(snapshot.visitedDocuments in 0..DriveMetadataIndexGate.MAX_DOCUMENTS &&
            snapshot.allFiles.size + snapshot.allDirectories.size <= DriveMetadataIndexGate.MAX_DOCUMENTS) {
            "HYPERATLAS_SOURCE_SCOPE_BOUNDED"
        }
        val rows = ArrayList<Row>(snapshot.allFiles.size + snapshot.allDirectories.size)
        for (dir in snapshot.allDirectories) {
            val path = pathOf(dir.relativePath)
            require(dir.semanticPassMask == 0x1f) { "HYPERATLAS_SEMANTIC_GATE" }
            rows += Row("DIRECTORY", path, path.substringBeforeLast('/', ""), null,
                null, null, dir.semanticTokens, dir.semanticPassMask,
                dir.semanticKindFlags, dir.parentDocumentId != null)
        }
        var bytesKnown = 0L
        var archiveKnown = 0L
        var archives = 0
        var unknownFiles = 0
        for (file in snapshot.allFiles) {
            val path = pathOf(file.relativePath)
            val size = file.sizeBytes
            require(size == null || size >= 0L) { "HYPERATLAS_INVALID_SIZE" }
            require(file.semanticPassMask == 0x1f) { "HYPERATLAS_SEMANTIC_GATE" }
            if (size == null) unknownFiles++ else bytesKnown = Math.addExact(bytesKnown, size)
            val archive = DriveMetadataIndexGate.classifyArchive(path, file.mimeType)
            if (archive != null) {
                archives++
                if (size != null) archiveKnown = Math.addExact(archiveKnown, size)
            }
            rows += Row("FILE", path, path.substringBeforeLast('/', ""), file.mimeType,
                size, archive, file.semanticTokens, file.semanticPassMask,
                file.semanticKindFlags, file.parentDocumentId != null)
        }
        rows.sortWith(compareBy<Row>({ it.path }, { it.type }))
        for (i in 1 until rows.size) {
            require(rows[i - 1].path != rows[i].path) { "HYPERATLAS_DUPLICATE_PATH" }
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArrayOutputStream(shardLimitBytes)
        val parts = ArrayList<Part>()
        var countInPart = 0
        var totalBytes = 0L

        fun flush() {
            if (countInPart == 0) return
            require(parts.size < MAX_PARTS) { "HYPERATLAS_PART_COUNT_LIMIT" }
            val bytes = buffer.toByteArray()
            val name = "index-%05d.jsonl".format(java.util.Locale.ROOT, parts.size + 1)
            val hash = sha256(bytes)
            val observed = writeShardAndReadback(name, bytes)
            require(observed.equals(hash, ignoreCase = false)) {
                "HYPERATLAS_SHARD_READBACK_MISMATCH"
            }
            parts += Part(parts.size + 1, name, countInPart, bytes.size, hash)
            buffer.reset()
            countInPart = 0
        }

        for (row in rows) {
            val line = (gson.toJson(row) + "\n").toByteArray(Charsets.UTF_8)
            require(line.size <= shardLimitBytes) { "HYPERATLAS_ROW_EXCEEDS_SHARD_LIMIT" }
            require(totalBytes <= MAX_TOTAL_INDEX_BYTES - line.size) {
                "HYPERATLAS_TOTAL_INDEX_BUDGET"
            }
            if (buffer.size() + line.size > shardLimitBytes) flush()
            buffer.write(line)
            digest.update(line)
            totalBytes += line.size
            countInPart++
        }
        flush()
        val metadata = Manifest(
            records = rows.size,
            folders = snapshot.allDirectories.size,
            files = snapshot.allFiles.size,
            archive_candidates = archives,
            known_file_bytes = bytesKnown,
            known_archive_bytes = archiveKnown,
            unknown_size_files = unknownFiles,
            index_bytes = totalBytes,
            index_sha256 = digest.digest().joinToString("") { "%02x".format(it) },
            parts = parts
        )
        return (gson.toJson(metadata) + "\n").toByteArray(Charsets.UTF_8)
    }
}
