package com.rafgittools.navigator

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.util.ArrayDeque

/**
 * Metadata-only recursive inventory for a user-selected SAF tree.
 * No source document bytes are opened, changed or deleted.
 */
object NovoexportSafInventory {
    data class Entry(
        val uri: String,
        val documentId: String,
        val name: String,
        val mimeType: String,
        val sizeBytes: Long?
    )

    data class Result(
        val treeUri: String,
        val visitedDocuments: Int,
        val visitedDirectories: Int,
        val candidateFiles: List<Entry>,
        val knownCandidateBytes: Long,
        val unknownSizeCandidateFiles: Int,
        val state: String = "INVENTORY_COMPLETE_METADATA_ONLY"
    )

    fun scan(
        resolver: ContentResolver,
        treeUri: Uri,
        maxDocuments: Int = 100_000
    ): Result {
        require(maxDocuments > 0) { "maxDocuments must be positive" }
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        require(rootId.isNotBlank()) { "Selected SAF tree has no root document id" }

        val pending = ArrayDeque<String>()
        val visitedDirectories = mutableSetOf<String>()
        val candidates = mutableListOf<Entry>()
        pending.add(rootId)

        var visitedDocuments = 0
        var knownBytes = 0L
        var unknownSizes = 0

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        while (pending.isNotEmpty()) {
            val directoryId = pending.removeFirst()
            if (!visitedDirectories.add(directoryId)) continue

            val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, directoryId)
            val cursor = resolver.query(children, projection, null, null, null)
                ?: error("SAF provider returned no cursor for directory inventory")
            cursor.use {
                val idIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                while (it.moveToNext()) {
                    visitedDocuments += 1
                    require(visitedDocuments <= maxDocuments) {
                        "SAF inventory exceeded configured maxDocuments=$maxDocuments"
                    }

                    val documentId = it.getString(idIndex) ?: error("SAF child has no document id")
                    val name = it.getString(nameIndex) ?: "TOKEN_VAZIO_DISPLAY_NAME"
                    val mimeType = it.getString(mimeIndex) ?: "application/octet-stream"

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        pending.addLast(documentId)
                        continue
                    }
                    if (!acceptsSourceName(name)) continue

                    val size = if (it.isNull(sizeIndex)) null else it.getLong(sizeIndex).takeIf { value -> value >= 0L }
                    if (size == null) {
                        unknownSizes += 1
                    } else {
                        require(size <= Long.MAX_VALUE - knownBytes) {
                            "SAF provider size aggregation overflow"
                        }
                        knownBytes += size
                    }

                    candidates += Entry(
                        uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId).toString(),
                        documentId = documentId,
                        name = name,
                        mimeType = mimeType,
                        sizeBytes = size
                    )
                }
            }
        }

        val ordered = candidates.sortedWith(compareBy<Entry>({ it.name.lowercase() }, { it.documentId }))
        return Result(
            treeUri = treeUri.toString(),
            visitedDocuments = visitedDocuments,
            visitedDirectories = visitedDirectories.size,
            candidateFiles = ordered,
            knownCandidateBytes = knownBytes,
            unknownSizeCandidateFiles = unknownSizes
        )
    }

    internal fun acceptsSourceName(name: String): Boolean {
        val normalized = name.substringAfterLast('/').lowercase()
        if (!normalized.endsWith(".json")) return false
        return normalized.startsWith("conversation") || normalized.startsWith("codex")
    }
}
