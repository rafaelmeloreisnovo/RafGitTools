package com.rafgittools.bridge

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.ArrayDeque
import java.util.UUID

data class SafDirectoryPreview(
    val providerAuthority: String,
    val visitedDocumentCount: Int,
    val directoryCount: Int,
    val fileCount: Int,
    val knownBytes: Long,
    val unknownSizeFileCount: Int,
    val inventoryFingerprint: String,
    val samplePaths: List<String>
)

data class SafDirectoryFileRecord(
    val relativePath: String,
    val mimeType: String,
    val bytes: Long,
    val sha256: String,
    val sourceDocumentRefSha256: String
)

data class SafDirectoryIndexManifestV1(
    val schemaVersion: String = "rafgittools.saf-directory-index.v1",
    val sourceProviderAuthority: String,
    val sourceTreeRefSha256: String,
    val sourceSharingVisibility: String = "TOKEN_VAZIO_SAF_DOES_NOT_EXPOSE_SHARING",
    val privacyClass: String = "PRIVATE_LOCAL_ONLY",
    val sourceMutation: String = "NONE",
    val publicationState: String = "BLOCKED_UNTIL_EXPLICIT_DESTINATION_AND_PRIVACY_REVIEW",
    val destinationProvider: String = "TOKEN_VAZIO_EXPLICIT_TARGET_REQUIRED",
    val destinationRepository: String = "TOKEN_VAZIO_EXPLICIT_TARGET_REQUIRED",
    val destinationRef: String = "TOKEN_VAZIO_EXPLICIT_TARGET_REQUIRED",
    val destinationPath: String = "TOKEN_VAZIO_EXPLICIT_TARGET_REQUIRED",
    val directories: List<String>,
    val files: List<SafDirectoryFileRecord>,
    val totalBytes: Long,
    val claimAllowed: Boolean = false
)

data class SafDirectoryIntakeReceiptV1(
    val schemaVersion: String = "rafgittools.saf-directory-receipt.v1",
    val operationId: String,
    val state: String = "COPIED_READBACK_VERIFIED",
    val indexPath: String = "index/manifest.v1.json",
    val indexSha256: String,
    val inputInventoryFingerprint: String,
    val copiedFileCount: Int,
    val copiedDirectoryCount: Int,
    val copiedBytes: Long,
    val readbackVerified: Boolean,
    val sourceMutation: String = "NONE",
    val externalTransfer: String = "NONE",
    val privacyClass: String = "PRIVATE_LOCAL_ONLY",
    val sourceSharingVisibility: String = "TOKEN_VAZIO_SAF_DOES_NOT_EXPOSE_SHARING",
    val claimAllowed: Boolean = false,
    val createdAtEpochMs: Long
)

data class SafDirectoryCopyResult(
    val snapshotDirectory: File,
    val manifestFile: File,
    val receiptFile: File,
    val fileCount: Int,
    val directoryCount: Int,
    val totalBytes: Long,
    val manifestSha256: String,
    val receiptSha256: String
)

internal data class SafDirectoryFingerprintFile(
    val relativePath: String,
    val mimeType: String,
    val declaredBytes: Long?,
    val documentId: String
)

/**
 * Copy-only SAF folder intake for an explicitly selected source tree.
 *
 * The first pass is metadata-only. A second user action copies the reviewed tree into
 * app-private storage, hashes every file, verifies staged readback, and writes a private index
 * plus receipt. It never writes to the source tree or to Drive/GitHub.
 */
object SafDirectoryIntakeGate {
    const val DEFAULT_MAX_DOCUMENTS: Int = 50_000
    const val DEFAULT_MAX_TOTAL_BYTES: Long = 512L * 1024L * 1024L
    private const val MAX_FILE_BYTES: Long = DEFAULT_MAX_TOTAL_BYTES
    private const val BUFFER_BYTES = 64 * 1024
    private const val MAX_SEGMENT_BYTES = 255
    private const val MAX_RELATIVE_PATH_BYTES = 2048

    private val gson = GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()

    fun preview(
        resolver: ContentResolver,
        treeUri: Uri,
        maxDocuments: Int = DEFAULT_MAX_DOCUMENTS
    ): SafDirectoryPreview {
        val inventory = readInventory(resolver, treeUri, maxDocuments)
        return SafDirectoryPreview(
            providerAuthority = inventory.providerAuthority,
            visitedDocumentCount = inventory.visitedDocumentCount,
            directoryCount = inventory.directories.size,
            fileCount = inventory.files.size,
            knownBytes = inventory.knownBytes,
            unknownSizeFileCount = inventory.unknownSizeFileCount,
            inventoryFingerprint = inventory.fingerprint,
            samplePaths = inventory.files.take(8).map { it.relativePath }
        )
    }

    @Throws(IOException::class)
    fun copyAndIndex(
        context: Context,
        treeUri: Uri,
        expectedInventoryFingerprint: String,
        createdAtEpochMs: Long = System.currentTimeMillis(),
        maxDocuments: Int = DEFAULT_MAX_DOCUMENTS,
        maxTotalBytes: Long = DEFAULT_MAX_TOTAL_BYTES
    ): SafDirectoryCopyResult {
        if (!expectedInventoryFingerprint.matches(Regex("^[a-f0-9]{64}$"))) {
            throw IOException("PREVIEW_FINGERPRINT_INVALID")
        }
        if (maxTotalBytes <= 0L) throw IOException("MAX_TOTAL_BYTES_MUST_BE_POSITIVE")

        val inventory = readInventory(context.contentResolver, treeUri, maxDocuments)
        if (inventory.fingerprint != expectedInventoryFingerprint) {
            throw IOException("SOURCE_CHANGED_AFTER_PREVIEW")
        }
        if (inventory.knownBytes > maxTotalBytes) {
            throw IOException("SOURCE_EXCEEDS_MAX_TOTAL_BYTES")
        }

        val privateRoot = File(context.filesDir, "saf-directory-intake")
        if (!privateRoot.isDirectory && !privateRoot.mkdirs()) {
            throw IOException("PRIVATE_INTAKE_DIRECTORY_UNAVAILABLE")
        }

        if (inventory.knownBytes > privateRoot.usableSpace) {
            throw IOException("INSUFFICIENT_PRIVATE_STORAGE_FOR_KNOWN_SOURCE_BYTES")
        }

        val operationId = UUID.randomUUID().toString()
        val snapshotDirectory = File(
            privateRoot,
            "snapshot-" + createdAtEpochMs + "-" + operationId
        )
        if (!snapshotDirectory.mkdirs()) throw IOException("SNAPSHOT_DIRECTORY_UNAVAILABLE")

        try {
            val payloadRoot = File(snapshotDirectory, "payload")
            if (!payloadRoot.mkdirs()) throw IOException("PAYLOAD_DIRECTORY_UNAVAILABLE")

            inventory.directories
                .sortedWith(compareBy<String>({ it.count { char -> char == '/' } }, { it }))
                .forEach { relativePath ->
                    val directory = resolveInside(payloadRoot, relativePath)
                    if (!directory.isDirectory && !directory.mkdirs()) {
                        throw IOException("PAYLOAD_SUBDIRECTORY_UNAVAILABLE")
                    }
                }

            var totalBytes = 0L
            val records = mutableListOf<SafDirectoryFileRecord>()
            inventory.files.sortedBy { it.relativePath }.forEach { source ->
                if (source.declaredBytes != null && source.declaredBytes > MAX_FILE_BYTES) {
                    throw IOException("SOURCE_FILE_EXCEEDS_MAX_FILE_BYTES")
                }
                val remainingBytes = maxTotalBytes - totalBytes
                if (remainingBytes <= 0L) throw IOException("SOURCE_EXCEEDS_MAX_TOTAL_BYTES")

                val target = resolveInside(payloadRoot, source.relativePath)
                val parent = target.parentFile ?: throw IOException("PAYLOAD_PARENT_MISSING")
                if (!parent.isDirectory && !parent.mkdirs()) {
                    throw IOException("PAYLOAD_PARENT_UNAVAILABLE")
                }
                if (target.exists()) throw IOException("PAYLOAD_PATH_COLLISION")

                val part = File(
                    parent,
                    "." + target.name + "." + UUID.randomUUID().toString() + ".part"
                )
                val input = context.contentResolver.openInputStream(source.uri)
                    ?: throw IOException("SAF_SOURCE_STREAM_UNAVAILABLE")
                val sourceDigest = input.use { copyToPart(it, part, minOf(remainingBytes, MAX_FILE_BYTES)) }
                if (source.declaredBytes != null && source.declaredBytes != sourceDigest.bytes) {
                    throw IOException("SAF_SOURCE_SIZE_CHANGED_DURING_COPY")
                }

                val stagedDigest = digestFile(part)
                if (stagedDigest != sourceDigest) throw IOException("STAGED_READBACK_MISMATCH")
                if (!part.renameTo(target)) throw IOException("STAGED_FILE_PROMOTION_FAILED")

                val finalDigest = digestFile(target)
                if (finalDigest != sourceDigest) throw IOException("FINAL_READBACK_MISMATCH")
                if (sourceDigest.bytes > maxTotalBytes - totalBytes) {
                    throw IOException("SOURCE_EXCEEDS_MAX_TOTAL_BYTES")
                }
                totalBytes += sourceDigest.bytes

                records += SafDirectoryFileRecord(
                    relativePath = source.relativePath,
                    mimeType = source.mimeType,
                    bytes = sourceDigest.bytes,
                    sha256 = sourceDigest.sha256,
                    sourceDocumentRefSha256 = sha256Text(source.documentId)
                )
            }

            val manifest = SafDirectoryIndexManifestV1(
                sourceProviderAuthority = inventory.providerAuthority,
                sourceTreeRefSha256 = inventory.sourceTreeRefSha256,
                directories = inventory.directories.sorted(),
                files = records.sortedBy { it.relativePath },
                totalBytes = totalBytes
            )
            val manifestBytes = (gson.toJson(manifest) + "\n").toByteArray(Charsets.UTF_8)
            val manifestSha256 = sha256Bytes(manifestBytes)
            val indexDirectory = File(snapshotDirectory, "index")
            if (!indexDirectory.mkdirs()) throw IOException("INDEX_DIRECTORY_UNAVAILABLE")
            val manifestFile = File(indexDirectory, "manifest.v1.json")
            atomicWrite(manifestFile, manifestBytes)

            val receipt = SafDirectoryIntakeReceiptV1(
                operationId = operationId,
                indexSha256 = manifestSha256,
                inputInventoryFingerprint = inventory.fingerprint,
                copiedFileCount = records.size,
                copiedDirectoryCount = inventory.directories.size,
                copiedBytes = totalBytes,
                readbackVerified = true,
                createdAtEpochMs = createdAtEpochMs
            )
            val receiptBytes = (gson.toJson(receipt) + "\n").toByteArray(Charsets.UTF_8)
            val receiptFile = File(indexDirectory, "receipt.v1.json")
            atomicWrite(receiptFile, receiptBytes)

            return SafDirectoryCopyResult(
                snapshotDirectory = snapshotDirectory,
                manifestFile = manifestFile,
                receiptFile = receiptFile,
                fileCount = records.size,
                directoryCount = inventory.directories.size,
                totalBytes = totalBytes,
                manifestSha256 = manifestSha256,
                receiptSha256 = sha256Bytes(receiptBytes)
            )
        } catch (failure: Throwable) {
            if (!snapshotDirectory.deleteRecursively()) {
                failure.addSuppressed(IOException("INCOMPLETE_PRIVATE_SNAPSHOT_CLEANUP_FAILED"))
            }
            throw failure
        }
    }

    internal fun safePathSegment(name: String): String {
        require(name.isNotBlank()) { "SAF_PATH_SEGMENT_EMPTY" }
        require(name != "." && name != "..") { "SAF_PATH_TRAVERSAL_BLOCKED" }
        require('/' !in name && '\\' !in name) { "SAF_PATH_SEPARATOR_BLOCKED" }
        require(name.none { it.code < 0x20 || it == '\u007f' }) { "SAF_PATH_CONTROL_CHARACTER_BLOCKED" }
        require(name.toByteArray(Charsets.UTF_8).size <= MAX_SEGMENT_BYTES) {
            "SAF_PATH_SEGMENT_TOO_LONG"
        }
        return name
    }

    internal fun safeRelativePath(parent: String, name: String): String {
        val segment = safePathSegment(name)
        val result = if (parent.isEmpty()) segment else parent + "/" + segment
        require(result.toByteArray(Charsets.UTF_8).size <= MAX_RELATIVE_PATH_BYTES) {
            "SAF_RELATIVE_PATH_TOO_LONG"
        }
        return result
    }

    internal fun inventoryFingerprint(
        providerAuthority: String,
        sourceTreeRefSha256: String,
        directories: List<String>,
        files: List<SafDirectoryFingerprintFile>
    ): String {
        val canonical = StringBuilder()
        appendField(canonical, "rafgittools.saf-directory-inventory.v1")
        appendField(canonical, providerAuthority)
        appendField(canonical, sourceTreeRefSha256)
        directories.sorted().forEach {
            appendField(canonical, "D")
            appendField(canonical, it)
        }
        files.sortedBy { it.relativePath }.forEach {
            appendField(canonical, "F")
            appendField(canonical, it.relativePath)
            appendField(canonical, it.mimeType)
            appendField(canonical, it.declaredBytes?.toString() ?: "TOKEN_VAZIO_SIZE")
            appendField(canonical, sha256Text(it.documentId))
        }
        return sha256Bytes(canonical.toString().toByteArray(Charsets.UTF_8))
    }

    private fun readInventory(
        resolver: ContentResolver,
        treeUri: Uri,
        maxDocuments: Int
    ): Inventory {
        if (maxDocuments <= 0) throw IOException("MAX_DOCUMENTS_MUST_BE_POSITIVE")
        val providerAuthority = treeUri.authority?.takeIf { it.isNotBlank() }
            ?: throw IOException("SAF_PROVIDER_AUTHORITY_UNAVAILABLE")
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        if (rootDocumentId.isBlank()) throw IOException("SAF_ROOT_DOCUMENT_ID_UNAVAILABLE")

        val pending = ArrayDeque<PendingDirectory>()
        pending.addLast(PendingDirectory(rootDocumentId, ""))
        val seenDocumentIds = mutableSetOf(rootDocumentId)
        val seenRelativePaths = mutableSetOf<String>()
        val directories = mutableListOf<String>()
        val files = mutableListOf<SourceDocument>()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        var visitedDocuments = 0
        var knownBytes = 0L
        var unknownSizes = 0

        while (pending.isNotEmpty()) {
            val parent = pending.removeFirst()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parent.documentId)
            val cursor = resolver.query(childrenUri, projection, null, null, null)
                ?: throw IOException("SAF_DIRECTORY_QUERY_UNAVAILABLE")

            cursor.use {
                val idIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                if (idIndex < 0 || nameIndex < 0 || mimeIndex < 0) {
                    throw IOException("SAF_REQUIRED_METADATA_COLUMN_MISSING")
                }

                val children = mutableListOf<ChildDocument>()
                while (it.moveToNext()) {
                    visitedDocuments += 1
                    if (visitedDocuments > maxDocuments) {
                        throw IOException("SAF_MAX_DOCUMENTS_EXCEEDED")
                    }

                    val documentId = it.getString(idIndex)
                        ?: throw IOException("SAF_DOCUMENT_ID_MISSING")
                    val displayName = it.getString(nameIndex)
                        ?: throw IOException("SAF_DISPLAY_NAME_MISSING")
                    val mimeType = it.getString(mimeIndex)
                        ?.takeIf { value -> value.isNotBlank() }
                        ?: throw IOException("SAF_MIME_TYPE_MISSING")
                    val relativePath = try {
                        safeRelativePath(parent.relativePath, displayName)
                    } catch (failure: IllegalArgumentException) {
                        throw IOException(failure.message ?: "SAF_PATH_UNSAFE", failure)
                    }
                    val declaredBytes = if (sizeIndex < 0 || it.isNull(sizeIndex)) {
                        null
                    } else {
                        it.getLong(sizeIndex).takeIf { value -> value >= 0L }
                    }

                    children += ChildDocument(
                        documentId = documentId,
                        relativePath = relativePath,
                        mimeType = mimeType,
                        declaredBytes = declaredBytes
                    )
                }

                children.sortedBy { child -> child.relativePath }.forEach { child ->
                    if (!seenDocumentIds.add(child.documentId)) {
                        throw IOException("SAF_DUPLICATE_DOCUMENT_REFERENCE")
                    }
                    if (!seenRelativePaths.add(child.relativePath)) {
                        throw IOException("SAF_RELATIVE_PATH_COLLISION")
                    }
                    if (child.mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        directories += child.relativePath
                        pending.addLast(PendingDirectory(child.documentId, child.relativePath))
                    } else {
                        if (child.declaredBytes == null) {
                            unknownSizes += 1
                        } else {
                            if (child.declaredBytes > Long.MAX_VALUE - knownBytes) {
                                throw IOException("SAF_KNOWN_SIZE_OVERFLOW")
                            }
                            knownBytes += child.declaredBytes
                        }
                        files += SourceDocument(
                            uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, child.documentId),
                            documentId = child.documentId,
                            relativePath = child.relativePath,
                            mimeType = child.mimeType,
                            declaredBytes = child.declaredBytes
                        )
                    }
                }
            }
        }

        val orderedDirectories = directories.sorted()
        val orderedFiles = files.sortedBy { it.relativePath }
        val sourceTreeRefSha256 = sha256Text(treeUri.toString())
        val fingerprintFiles = orderedFiles.map {
            SafDirectoryFingerprintFile(
                relativePath = it.relativePath,
                mimeType = it.mimeType,
                declaredBytes = it.declaredBytes,
                documentId = it.documentId
            )
        }
        return Inventory(
            providerAuthority = providerAuthority,
            sourceTreeRefSha256 = sourceTreeRefSha256,
            visitedDocumentCount = visitedDocuments,
            directories = orderedDirectories,
            files = orderedFiles,
            knownBytes = knownBytes,
            unknownSizeFileCount = unknownSizes,
            fingerprint = inventoryFingerprint(
                providerAuthority,
                sourceTreeRefSha256,
                orderedDirectories,
                fingerprintFiles
            )
        )
    }

    private fun resolveInside(root: File, relativePath: String): File {
        val canonicalRoot = root.canonicalFile
        val target = File(canonicalRoot, relativePath).canonicalFile
        if (!target.path.startsWith(canonicalRoot.path + File.separator)) {
            throw IOException("SAF_PATH_ESCAPES_PRIVATE_SNAPSHOT")
        }
        return target
    }

    private fun copyToPart(input: InputStream, part: File, maxBytes: Long): StreamDigest {
        val digest = MessageDigest.getInstance("SHA-256")
        var copiedBytes = 0L
        val buffer = ByteArray(BUFFER_BYTES)
        FileOutputStream(part, false).use { output ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                if (read.toLong() > maxBytes - copiedBytes) {
                    throw IOException("SAF_COPY_BYTE_LIMIT_EXCEEDED")
                }
                digest.update(buffer, 0, read)
                output.write(buffer, 0, read)
                copiedBytes += read.toLong()
            }
            output.flush()
            output.fd.sync()
        }
        return StreamDigest(
            bytes = copiedBytes,
            sha256 = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        )
    }

    private fun digestFile(file: File): StreamDigest =
        file.inputStream().buffered(BUFFER_BYTES).use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(BUFFER_BYTES)
            var bytes = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                digest.update(buffer, 0, read)
                bytes += read.toLong()
            }
            StreamDigest(
                bytes = bytes,
                sha256 = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
            )
        }

    private fun atomicWrite(target: File, bytes: ByteArray) {
        val parent = target.parentFile ?: throw IOException("INDEX_PARENT_MISSING")
        val part = File(parent, "." + target.name + "." + UUID.randomUUID().toString() + ".part")
        FileOutputStream(part, false).use { output ->
            output.write(bytes)
            output.flush()
            output.fd.sync()
        }
        if (!part.renameTo(target)) {
            part.delete()
            throw IOException("INDEX_ATOMIC_PROMOTION_FAILED")
        }
    }

    private fun appendField(builder: StringBuilder, value: String) {
        val byteLength = value.toByteArray(Charsets.UTF_8).size
        builder.append(byteLength).append(':').append(value)
    }

    private fun sha256Text(value: String): String =
        sha256Bytes(value.toByteArray(Charsets.UTF_8))

    private fun sha256Bytes(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte) }

    private data class PendingDirectory(val documentId: String, val relativePath: String)

    private data class ChildDocument(
        val documentId: String,
        val relativePath: String,
        val mimeType: String,
        val declaredBytes: Long?
    )

    private data class SourceDocument(
        val uri: Uri,
        val documentId: String,
        val relativePath: String,
        val mimeType: String,
        val declaredBytes: Long?
    )

    private data class Inventory(
        val providerAuthority: String,
        val sourceTreeRefSha256: String,
        val visitedDocumentCount: Int,
        val directories: List<String>,
        val files: List<SourceDocument>,
        val knownBytes: Long,
        val unknownSizeFileCount: Int,
        val fingerprint: String
    )

    private data class StreamDigest(val bytes: Long, val sha256: String)
}
