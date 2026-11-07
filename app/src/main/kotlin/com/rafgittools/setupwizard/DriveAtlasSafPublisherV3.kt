package com.rafgittools.setupwizard

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import com.google.gson.JsonParser
import com.rafgittools.navigator.NovoexportSafInventory
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

/**
 * Publishes a compact, append-only snapshot in ONE user-selected SAF destination.
 * Every shard is written and SHA-256 read back before manifest.json is created.
 * Never reads archive contents. Never overwrites, deletes or modifies source files.
 * A failed run leaves an uncommitted folder without a manifest for audit/rollback.
 */
object DriveAtlasSafPublisherV3 {
    data class Receipt(
        /** Discoverable in the selected Drive folder; never a capability URI. */
        val runFolderName: String,
        val records: Int,
        val parts: Int,
        val archives: Int,
        val indexBytes: Long,
        val manifestSha256: String,
        val state: String = "SHARDED_INDEX_MANIFEST_READBACK_VERIFIED",
        val claimAllowed: Boolean = false
    )

    private fun digest(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    @Throws(IOException::class)
    private fun writeAndVerify(
        resolver: ContentResolver,
        destinationFolder: Uri,
        name: String,
        mime: String,
        bytes: ByteArray
    ): String {
        val file = DocumentsContract.createDocument(resolver, destinationFolder, mime, name)
            ?: throw IOException("HYPERATLAS_SAF_CREATE_DOCUMENT_BLOCKED")
        resolver.openOutputStream(file, "w")?.use { stream ->
            stream.write(bytes)
            stream.flush()
        } ?: throw IOException("HYPERATLAS_SAF_WRITE_UNAVAILABLE")
        val hash = MessageDigest.getInstance("SHA-256")
        var count = 0L
        resolver.openInputStream(file)?.use { stream ->
            val chunk = ByteArray(64 * 1024)
            while (true) {
                val n = stream.read(chunk)
                if (n < 0) break
                if (n == 0) continue
                count += n
                if (count > bytes.size.toLong()) throw IOException("HYPERATLAS_READBACK_TOO_LARGE")
                hash.update(chunk, 0, n)
            }
        } ?: throw IOException("HYPERATLAS_SAF_READBACK_UNAVAILABLE")
        val actual = hash.digest().joinToString("") { "%02x".format(it) }
        if (count != bytes.size.toLong() || actual != digest(bytes)) {
            throw IOException("HYPERATLAS_SAF_READBACK_MISMATCH")
        }
        return actual
    }

    @Throws(IOException::class)
    fun publish(
        resolver: ContentResolver,
        destinationTree: Uri,
        snapshot: NovoexportSafInventory.Result
    ): Receipt {
        if (destinationTree.scheme != "content") throw IOException("HYPERATLAS_SAF_TREE_REQUIRED")
        val rootDocument = try {
            DocumentsContract.buildDocumentUriUsingTree(
                destinationTree, DocumentsContract.getTreeDocumentId(destinationTree)
            )
        } catch (e: RuntimeException) {
            throw IOException("HYPERATLAS_INVALID_TREE_CAPABILITY", e)
        }
        val runName = "HYPERATLAS_V3_" + UUID.randomUUID().toString()
        val runFolder = DocumentsContract.createDocument(
            resolver, rootDocument, DocumentsContract.Document.MIME_TYPE_DIR, runName
        ) ?: throw IOException("HYPERATLAS_SAF_CREATE_RUN_FOLDER_BLOCKED")

        val manifestBytes = DriveAtlasCompactIndexV3.build(snapshot) { name, bytes ->
            writeAndVerify(resolver, runFolder, name, "text/plain", bytes)
        }
        // Commit record is always written last. A partial folder has no manifest.
        val hash = writeAndVerify(
            resolver, runFolder, "MANIFEST.json", "application/json", manifestBytes
        )
        val manifest = JsonParser.parseString(
            String(manifestBytes, Charsets.UTF_8)
        ).asJsonObject
        return Receipt(
            runFolderName = runName,
            records = manifest.get("records").asInt,
            parts = manifest.getAsJsonArray("parts").size(),
            archives = manifest.get("archive_candidates").asInt,
            indexBytes = manifest.get("index_bytes").asLong,
            manifestSha256 = hash
        )
    }
}
