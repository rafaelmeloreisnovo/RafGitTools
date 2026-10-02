package com.rafgittools.navigator

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.util.ArrayDeque

/**
 * Metadata-only recursive inventory for a user-selected SAF tree.
 * No source document bytes are opened, changed or deleted.
 *
 * allFiles/allDirectories describe the selected tree. candidateFiles remains
 * the narrower conversation/codex processing queue and must not be used as a
 * complete representation of the source tree.
 */
object NovoexportSafInventory {
    data class Entry(
        val uri: String,
        val documentId: String,
        val name: String,
        val mimeType: String,
        val sizeBytes: Long?,
        val relativePath: String = name,
        val parentDocumentId: String? = null
    )

    data class DirectoryEntry(
        val documentId: String,
        val parentDocumentId: String?,
        val name: String,
        val relativePath: String
    )

    data class Result(
        val treeUri: String,
        val visitedDocuments: Int,
        val visitedDirectories: Int,
        val candidateFiles: List<Entry>,
        val knownCandidateBytes: Long,
        val unknownSizeCandidateFiles: Int,
        val state: String = "INVENTORY_COMPLETE_METADATA_ONLY",
        val rootDocumentId: String = "",
        val allFiles: List<Entry> = emptyList(),
        val allDirectories: List<DirectoryEntry> = emptyList(),
        val knownTotalBytes: Long = 0L,
        val unknownSizeFiles: Int = 0
    )

    private data class DirectoryWork(
        val documentId: String,
        val relativePath: String
    )

    fun scan(
        resolver: ContentResolver,
        treeUri: Uri,
        maxDocuments: Int = 100_000
    ): Result {
        require(maxDocuments > 0) { "maxDocuments must be positive" }
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        require(rootId.isNotBlank()) { "Selected SAF tree has no root document id" }

        val pending = ArrayDeque<DirectoryWork>()
        val visitedDirectories = mutableSetOf<String>()
        val allFiles = mutableListOf<Entry>()
        val allDirectories = mutableListOf<DirectoryEntry>()
        val candidates = mutableListOf<Entry>()
        pending.addLast(DirectoryWork(rootId, ""))

        var visitedDocuments = 0
        var knownTotalBytes = 0L
        var unknownSizeFiles = 0
        var knownCandidateBytes = 0L
        var unknownCandidateFiles = 0

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        while (pending.isNotEmpty()) {
            val directory = pending.removeFirst()
            val directoryId = directory.documentId
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
                    val relativePath = childPath(directory.relativePath, name)

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        allDirectories += DirectoryEntry(
                            documentId = documentId,
                            parentDocumentId = directoryId,
                            name = name,
                            relativePath = relativePath
                        )
                        pending.addLast(DirectoryWork(documentId, relativePath))
                        continue
                    }

                    val size = if (it.isNull(sizeIndex)) null
                    else it.getLong(sizeIndex).takeIf { value -> value >= 0L }
                    if (size == null) {
                        unknownSizeFiles += 1
                    } else {
                        knownTotalBytes = addSize(knownTotalBytes, size, "SAF total size aggregation overflow")
                    }

                    val entry = Entry(
                        uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId).toString(),
                        documentId = documentId,
                        name = name,
                        mimeType = mimeType,
                        sizeBytes = size,
                        relativePath = relativePath,
                        parentDocumentId = directoryId
                    )
                    allFiles += entry

                    if (acceptsSourceName(name)) {
                        candidates += entry
                        if (size == null) {
                            unknownCandidateFiles += 1
                        } else {
                            knownCandidateBytes = addSize(
                                knownCandidateBytes,
                                size,
                                "SAF candidate size aggregation overflow"
                            )
                        }
                    }
                }
            }
        }

        val orderedFiles = allFiles
            .distinctBy { it.documentId }
            .sortedWith(compareBy<Entry>({ it.relativePath.lowercase() }, { it.documentId }))
        val orderedDirectories = allDirectories
            .distinctBy { it.documentId }
            .sortedWith(compareBy<DirectoryEntry>({ it.relativePath.lowercase() }, { it.documentId }))
        val orderedCandidates = candidates
            .distinctBy { it.documentId }
            .sortedWith(compareBy<Entry>({ it.relativePath.lowercase() }, { it.documentId }))

        return Result(
            treeUri = treeUri.toString(),
            visitedDocuments = visitedDocuments,
            visitedDirectories = visitedDirectories.size,
            candidateFiles = orderedCandidates,
            knownCandidateBytes = knownCandidateBytes,
            unknownSizeCandidateFiles = unknownCandidateFiles,
            rootDocumentId = rootId,
            allFiles = orderedFiles,
            allDirectories = orderedDirectories,
            knownTotalBytes = knownTotalBytes,
            unknownSizeFiles = unknownSizeFiles
        )
    }

    internal fun childPath(parentPath: String, name: String): String =
        if (parentPath.isBlank()) name else parentPath + "/" + name

    private fun addSize(current: Long, size: Long, error: String): Long {
        require(size <= Long.MAX_VALUE - current) { error }
        return current + size
    }

    internal fun acceptsSourceName(name: String): Boolean {
        val normalized = name.substringAfterLast('/').lowercase()
        if (!normalized.endsWith(".json")) return false
        return normalized.startsWith("conversation") || normalized.startsWith("codex")
    }
}
