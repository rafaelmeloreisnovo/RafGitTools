package com.rafgittools.data.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.rafgittools.data.cache.LocalRepositoryDao
import com.rafgittools.data.cache.LocalRepositoryEntity
import com.rafgittools.data.git.JGitService
import com.rafgittools.domain.model.SyncState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports a Storage Access Framework tree as an immutable Git snapshot into
 * RafGitTools' internal app-private storage.
 *
 * Design boundary:
 * - source SAF tree is READ ONLY;
 * - no direct JGit access to content:// URIs;
 * - import is staged under a temporary directory;
 * - a standard .git directory is required and validated by JGit;
 * - only a validated snapshot is registered in LocalRepositoryDao;
 * - source URI is never written to the receipt, only its SHA-256.
 */
@Singleton
class SafRepositoryImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val jGitService: JGitService,
    private val localRepositoryDao: LocalRepositoryDao,
) {
    suspend fun importTree(treeUri: Uri): Result<SafRepositoryImportReceipt> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(treeUri.scheme == "content") {
                    "SAF import requires a content:// tree URI"
                }

                val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
                val rootName = readDisplayName(treeUri, rootDocumentId)
                    ?.takeIf { it.isNotBlank() }
                    ?: "saf-repository"
                val safeRootName = sanitizeName(rootName)

                val importRoot = File(context.filesDir, PRIVATE_IMPORT_DIR).apply {
                    if (!exists() && !mkdirs()) {
                        throw IOException("Unable to create private SAF import directory")
                    }
                }

                val epochMs = System.currentTimeMillis()
                val tempDir = File(importRoot, ".incoming-$epochMs-$safeRootName")
                val finalDir = File(importRoot, "$safeRootName-saf-$epochMs")

                if (tempDir.exists() || finalDir.exists()) {
                    throw IOException("Import destination collision")
                }
                if (!tempDir.mkdirs()) {
                    throw IOException("Unable to create SAF staging directory")
                }

                var published = false
                try {
                    val counter = CopyCounter()
                    copyChildren(
                        treeUri = treeUri,
                        parentDocumentId = rootDocumentId,
                        targetDir = tempDir,
                        counter = counter,
                    )

                    val gitDir = File(tempDir, ".git")
                    if (!gitDir.isDirectory) {
                        throw IllegalArgumentException(
                            "Selected SAF tree is not a standard Git working tree: .git directory not exposed"
                        )
                    }

                    val branch = jGitService.openRepository(tempDir.absolutePath)
                        .getOrThrow()
                        .use { git -> git.repository.branch ?: "unknown" }

                    if (!tempDir.renameTo(finalDir)) {
                        throw IOException("Unable to publish validated SAF snapshot atomically")
                    }
                    published = true

                    val entity = LocalRepositoryEntity(
                        path = finalDir.absolutePath,
                        name = safeRootName,
                        remoteUrl = null,
                        currentBranch = branch,
                        syncState = SyncState.SYNCED.name,
                    )
                    localRepositoryDao.upsert(entity)

                    val uriHash = sha256Hex(treeUri.toString().toByteArray(Charsets.UTF_8))
                    val receiptDir = File(context.filesDir, RECEIPT_DIR).apply {
                        if (!exists() && !mkdirs()) {
                            throw IOException("Unable to create receipt directory")
                        }
                    }
                    val receiptFile = File(receiptDir, "saf-import-$epochMs.receipt")
                    val receiptBody = buildString {
                        appendLine("RAFGITTOOLS_SAF_REPOSITORY_IMPORT_V1")
                        appendLine("epoch_ms=$epochMs")
                        appendLine("source_scheme=content")
                        appendLine("source_provider=${treeUri.authority ?: "TOKEN_VAZIO"}")
                        appendLine("source_uri_sha256=$uriHash")
                        appendLine("source_uri_raw=NOT_RECORDED")
                        appendLine("source_mutation=NONE")
                        appendLine("root_name=$safeRootName")
                        appendLine("files=${counter.files}")
                        appendLine("directories=${counter.directories}")
                        appendLine("bytes=${counter.bytes}")
                        appendLine("git_validation=PASS")
                        appendLine("branch=$branch")
                        appendLine("destination=${finalDir.absolutePath}")
                        appendLine("dao_registration=PASS")
                        appendLine("claim_allowed=false")
                    }
                    receiptFile.writeText(receiptBody, Charsets.UTF_8)

                    SafRepositoryImportReceipt(
                        repositoryName = safeRootName,
                        repositoryPath = finalDir.absolutePath,
                        branch = branch,
                        fileCount = counter.files,
                        directoryCount = counter.directories,
                        byteCount = counter.bytes,
                        providerAuthority = treeUri.authority ?: "TOKEN_VAZIO",
                        sourceUriSha256 = uriHash,
                        receiptPath = receiptFile.absolutePath,
                    )
                } catch (t: Throwable) {
                    tempDir.deleteRecursively()
                    if (published) {
                        try {
                            localRepositoryDao.delete(finalDir.absolutePath)
                        } catch (_: Exception) {
                            // Best-effort rollback: filesystem cleanup still proceeds.
                        }
                        finalDir.deleteRecursively()
                    }
                    throw t
                }
            }
        }

    private fun copyChildren(
        treeUri: Uri,
        parentDocumentId: String,
        targetDir: File,
        counter: CopyCounter,
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            parentDocumentId,
        )
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )

        val cursor = context.contentResolver.query(
            childrenUri,
            projection,
            null,
            null,
            null,
        ) ?: throw IOException("SAF provider returned no cursor for child enumeration")

        cursor.use {
            val idIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (it.moveToNext()) {
                val documentId = it.getString(idIndex)
                val displayName = it.getString(nameIndex) ?: "unnamed"
                val mimeType = it.getString(mimeIndex)
                val safeName = sanitizeName(displayName)
                val destination = File(targetDir, safeName)

                if (destination.exists()) {
                    throw IOException("Duplicate SAF child name after sanitization: $safeName")
                }

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    counter.directories += 1
                    enforceLimits(counter)
                    if (!destination.mkdir()) {
                        throw IOException("Unable to create directory: $safeName")
                    }
                    copyChildren(
                        treeUri = treeUri,
                        parentDocumentId = documentId,
                        targetDir = destination,
                        counter = counter,
                    )
                } else {
                    counter.files += 1
                    enforceLimits(counter)

                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                        treeUri,
                        documentId,
                    )
                    val input = context.contentResolver.openInputStream(documentUri)
                        ?: throw IOException("Unable to open SAF document: $safeName")
                    input.use { source ->
                        destination.outputStream().buffered().use { sink ->
                            val buffer = ByteArray(COPY_BUFFER_BYTES)
                            while (true) {
                                val read = source.read(buffer)
                                if (read < 0) break
                                if (read == 0) continue
                                counter.bytes += read.toLong()
                                enforceLimits(counter)
                                sink.write(buffer, 0, read)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun readDisplayName(treeUri: Uri, documentId: String): String? {
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return context.contentResolver.query(documentUri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            cursor.getString(
                cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            )
        }
    }

    private fun sanitizeName(raw: String): String {
        val sanitized = raw
            .replace('/', '_')
            .replace('\\', '_')
            .replace('\u0000', '_')
            .trim()

        require(sanitized.isNotEmpty() && sanitized != "." && sanitized != "..") {
            "Invalid SAF document name"
        }
        return sanitized.take(MAX_NAME_CHARS)
    }

    private fun enforceLimits(counter: CopyCounter) {
        if (counter.files > MAX_FILES) {
            throw IOException("SAF import exceeds file limit ($MAX_FILES)")
        }
        if (counter.directories > MAX_DIRECTORIES) {
            throw IOException("SAF import exceeds directory limit ($MAX_DIRECTORIES)")
        }
        if (counter.bytes > MAX_BYTES) {
            throw IOException("SAF import exceeds byte limit ($MAX_BYTES)")
        }
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private data class CopyCounter(
        var files: Int = 0,
        var directories: Int = 0,
        var bytes: Long = 0L,
    )

    companion object {
        private const val PRIVATE_IMPORT_DIR = "repositories-saf"
        private const val RECEIPT_DIR = "connectivity-receipts"
        private const val COPY_BUFFER_BYTES = 64 * 1024
        private const val MAX_FILES = 50_000
        private const val MAX_DIRECTORIES = 20_000
        private const val MAX_BYTES = 512L * 1024L * 1024L
        private const val MAX_NAME_CHARS = 180
    }
}

data class SafRepositoryImportReceipt(
    val repositoryName: String,
    val repositoryPath: String,
    val branch: String,
    val fileCount: Int,
    val directoryCount: Int,
    val byteCount: Long,
    val providerAuthority: String,
    val sourceUriSha256: String,
    val receiptPath: String,
)
