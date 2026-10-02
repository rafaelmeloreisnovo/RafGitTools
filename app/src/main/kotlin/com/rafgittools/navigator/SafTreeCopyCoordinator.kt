package com.rafgittools.navigator

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.coroutineContext

data class SafTreeCopyProgress(
    val completedNodes: Int,
    val totalNodes: Int,
    val currentPathSha256: String = "TOKEN_VAZIO"
)

data class SafTreeCopyResult(
    val operationId: String,
    val targetRootName: String,
    val copiedFiles: Int,
    val createdDirectories: Int,
    val copiedBytes: Long,
    val receiptFile: java.io.File,
    val receiptChainHeadSha256: String,
    val claimAllowed: Boolean = false
)

/**
 * A short-lived in-memory plan. SAF capability URIs remain process memory only
 * and are excluded from receipts and serialized catalog artifacts.
 */
class SafTreeCopyPlan internal constructor(
    val operationId: String,
    val targetRootName: String,
    val fileCount: Int,
    val directoryCount: Int,
    val knownBytes: Long,
    val unknownSizeFiles: Int,
    val sourceRootSha256: String,
    val destinationRootSha256: String,
    val planSha256: String,
    internal val inventory: NovoexportSafInventory.Result,
    internal val destinationTreeUri: String
)

object SafTreeCopyCoordinator {
    private const val DIRECTORY_MIME_TYPE = Document.MIME_TYPE_DIR
    private const val STREAM_BUFFER_BYTES = 64 * 1024
    private const val RECEIPT_NAME = "saf-tree-copy-events.jsonl"

    private data class Node(
        val documentId: String,
        val parentDocumentId: String,
        val name: String,
        val relativePath: String,
        val pathSha256: String,
        val isDirectory: Boolean,
        val mimeType: String,
        val sizeBytes: Long?,
        val sourceUri: String
    )

    private data class StreamDigest(val bytes: Long, val sha256: String)

    fun preparePlan(
        context: Context,
        inventory: NovoexportSafInventory.Result,
        destinationTree: Uri
    ): SafTreeCopyPlan {
        require(inventory.state == "INVENTORY_COMPLETE_METADATA_ONLY") {
            "COPY_SOURCE_INVENTORY_INCOMPLETE"
        }
        require(inventory.rootDocumentId.isNotBlank()) { "COPY_SOURCE_ROOT_MISSING" }
        require(inventory.visitedDocuments == inventory.allFiles.size + inventory.allDirectories.size) {
            "COPY_SOURCE_INVENTORY_NODE_COUNT_MISMATCH"
        }
        val sourceTree = Uri.parse(inventory.treeUri)
        require(sourceTree.scheme == "content" && !sourceTree.authority.isNullOrBlank()) {
            "COPY_SOURCE_TREE_URI_INVALID"
        }
        require(destinationTree.scheme == "content" && !destinationTree.authority.isNullOrBlank()) {
            "COPY_DESTINATION_TREE_URI_INVALID"
        }
        require(hasPersistedGrant(context, sourceTree, read = true, write = false)) {
            "COPY_SOURCE_PERSISTENT_READ_PERMISSION_REQUIRED"
        }
        require(hasPersistedGrant(context, destinationTree, read = true, write = true)) {
            "COPY_DESTINATION_PERSISTENT_READ_WRITE_PERMISSION_REQUIRED"
        }

        val resolver = context.contentResolver
        val sourceRootUri = DocumentsContract.buildDocumentUriUsingTree(
            sourceTree,
            inventory.rootDocumentId
        )
        val destinationRootId = DocumentsContract.getTreeDocumentId(destinationTree)
        require(destinationRootId.isNotBlank()) { "COPY_DESTINATION_ROOT_MISSING" }
        val destinationParentUri = DocumentsContract.buildDocumentUriUsingTree(
            destinationTree,
            destinationRootId
        )

        if (sourceTree.authority == destinationTree.authority) {
            require(inventory.rootDocumentId != destinationRootId) {
                "COPY_SOURCE_AND_DESTINATION_ARE_SAME_ROOT"
            }
            val destinationInsideSource = try {
                DocumentsContract.isChildDocument(resolver, sourceRootUri, destinationParentUri)
            } catch (_: Exception) {
                throw IOException("COPY_DESTINATION_ANCESTRY_UNVERIFIED")
            }
            require(!destinationInsideSource) { "COPY_DESTINATION_IS_INSIDE_SOURCE" }
        }

        val nodes = validateInventory(inventory, sourceTree)
        val sourceRootSha = sha256(inventory.treeUri.toByteArray(Charsets.UTF_8))
        val destinationRootSha = sha256(destinationTree.toString().toByteArray(Charsets.UTF_8))
        val occupiedNames = listChildNames(resolver, destinationTree, destinationRootId)
        val operationId = UUID.randomUUID().toString()
        val targetRootName = "RafGitTools_Copy_" +
            operationId.replace("-", "").take(12).lowercase()
        require(!occupiedNames.contains(targetRootName)) { "COPY_TARGET_ROOT_NAME_COLLISION" }

        val canonicalPlan = buildString {
            append("rafgittools.saf-tree-copy-plan.v1").append('\n')
            append(sourceRootSha).append('\n')
            append(destinationRootSha).append('\n')
            append(operationId).append('\n')
            append(targetRootName).append('\n')
            nodes.sortedWith(compareBy<Node>({ it.relativePath }, { it.isDirectory }))
                .forEach { node ->
                    append(if (node.isDirectory) "D" else "F").append('|')
                    append(node.pathSha256).append('|')
                    append(sha256(node.documentId.toByteArray(Charsets.UTF_8))).append('|')
                    append(sha256(node.parentDocumentId.toByteArray(Charsets.UTF_8))).append('|')
                    append(node.mimeType).append('|')
                    append(node.sizeBytes?.toString() ?: "TOKEN_VAZIO").append('|')
                    append(sha256(node.sourceUri.toByteArray(Charsets.UTF_8))).append('\n')
                }
        }
        val planSha = sha256(canonicalPlan.toByteArray(Charsets.UTF_8))
        return SafTreeCopyPlan(
            operationId = operationId,
            targetRootName = targetRootName,
            fileCount = inventory.allFiles.size,
            directoryCount = inventory.allDirectories.size,
            knownBytes = inventory.knownTotalBytes,
            unknownSizeFiles = inventory.unknownSizeFiles,
            sourceRootSha256 = sourceRootSha,
            destinationRootSha256 = destinationRootSha,
            planSha256 = planSha,
            inventory = inventory,
            destinationTreeUri = destinationTree.toString()
        )
    }

    suspend fun execute(
        context: Context,
        plan: SafTreeCopyPlan,
        onProgress: suspend (SafTreeCopyProgress) -> Unit = {}
    ): SafTreeCopyResult {
        val receiptFile = java.io.File(
            java.io.File(context.filesDir, "saf-tree-copy"),
            RECEIPT_NAME
        )
        val receipt = SafTreeCopyReceiptStore(receiptFile)
        val resolver = context.contentResolver
        val sourceTree = Uri.parse(plan.inventory.treeUri)
        val destinationTree = Uri.parse(plan.destinationTreeUri)
        val destinationParentId = DocumentsContract.getTreeDocumentId(destinationTree)
        val destinationParentUri = DocumentsContract.buildDocumentUriUsingTree(
            destinationTree,
            destinationParentId
        )
        val nodes = validateInventory(plan.inventory, sourceTree)
        val totalNodes = nodes.size
        var completedNodes = 0
        var copiedFiles = 0
        var createdDirectories = 0
        var copiedBytes = 0L
        var targetRootUri: Uri? = null

        receipt.append(
            operationId = plan.operationId,
            eventType = "COPY_STARTED",
            sourceRootSha256 = plan.sourceRootSha256,
            destinationRootSha256 = plan.destinationRootSha256,
            targetRootName = plan.targetRootName,
            planSha256 = plan.planSha256,
            plannedFiles = plan.fileCount,
            plannedDirectories = plan.directoryCount,
            resultCode = "PLAN_CONFIRMED"
        )

        try {
            coroutineContext.ensureActive()
            val currentNames = listChildNames(
                resolver,
                destinationTree,
                destinationParentId
            )
            if (currentNames.contains(plan.targetRootName)) {
                throw IOException("COPY_TARGET_ROOT_NAME_COLLISION")
            }
            targetRootUri = DocumentsContract.createDocument(
                resolver,
                destinationParentUri,
                DIRECTORY_MIME_TYPE,
                plan.targetRootName
            ) ?: throw IOException("COPY_ROOT_CREATE_REFUSED")

            receipt.append(
                operationId = plan.operationId,
                eventType = "COPY_ROOT_CREATED",
                sourceRootSha256 = plan.sourceRootSha256,
                destinationRootSha256 = plan.destinationRootSha256,
                targetRootName = plan.targetRootName,
                destinationRefSha256 = sha256(targetRootUri.toString().toByteArray(Charsets.UTF_8)),
                resultCode = "CREATED"
            )

            val destinationBySourceDocumentId = mutableMapOf<String, Uri>()
            destinationBySourceDocumentId[plan.inventory.rootDocumentId] = targetRootUri
            val orderedDirectories = nodes
                .filter { it.isDirectory }
                .sortedWith(compareBy<Node>({ depth(it.relativePath) }, { it.relativePath }))
            for (directory in orderedDirectories) {
                coroutineContext.ensureActive()
                val pathHash = directory.pathSha256
                receipt.append(
                    operationId = plan.operationId,
                    eventType = "DIRECTORY_CREATE_INTENT",
                    sourceRootSha256 = plan.sourceRootSha256,
                    destinationRootSha256 = plan.destinationRootSha256,
                    targetRootName = plan.targetRootName,
                    pathSha256 = pathHash,
                    planSha256 = plan.planSha256,
                    resultCode = "NO_OVERWRITE"
                )
                val parentUri = destinationBySourceDocumentId[directory.parentDocumentId]
                    ?: throw IOException("COPY_DIRECTORY_PARENT_NOT_MAPPED")
                val createdUri = DocumentsContract.createDocument(
                    resolver,
                    parentUri,
                    DIRECTORY_MIME_TYPE,
                    directory.name
                ) ?: throw IOException("COPY_DIRECTORY_CREATE_REFUSED")
                destinationBySourceDocumentId[directory.documentId] = createdUri
                receipt.append(
                    operationId = plan.operationId,
                    eventType = "DIRECTORY_CREATED",
                    sourceRootSha256 = plan.sourceRootSha256,
                    destinationRootSha256 = plan.destinationRootSha256,
                    targetRootName = plan.targetRootName,
                    pathSha256 = pathHash,
                    planSha256 = plan.planSha256,
                    sourceRefSha256 = sha256(directory.sourceUri.toByteArray(Charsets.UTF_8)),
                    destinationRefSha256 = sha256(createdUri.toString().toByteArray(Charsets.UTF_8)),
                    resultCode = "CREATED"
                )
                createdDirectories += 1
                completedNodes += 1
                onProgress(SafTreeCopyProgress(completedNodes, totalNodes, pathHash))
            }

            val orderedFiles = nodes
                .filterNot { it.isDirectory }
                .sortedBy { it.relativePath }
            for (file in orderedFiles) {
                coroutineContext.ensureActive()
                val pathHash = file.pathSha256
                val sourceUri = Uri.parse(file.sourceUri)
                val parentUri = destinationBySourceDocumentId[file.parentDocumentId]
                    ?: throw IOException("COPY_FILE_PARENT_NOT_MAPPED")
                receipt.append(
                    operationId = plan.operationId,
                    eventType = "FILE_COPY_INTENT",
                    sourceRootSha256 = plan.sourceRootSha256,
                    destinationRootSha256 = plan.destinationRootSha256,
                    targetRootName = plan.targetRootName,
                    pathSha256 = pathHash,
                    planSha256 = plan.planSha256,
                    sourceRefSha256 = sha256(file.sourceUri.toByteArray(Charsets.UTF_8)),
                    resultCode = "NO_OVERWRITE"
                )
                var createdFileUri: Uri? = null
                try {
                    createdFileUri = DocumentsContract.createDocument(
                        resolver,
                        parentUri,
                        file.mimeType.ifBlank { "application/octet-stream" },
                        file.name
                    ) ?: throw IOException("COPY_FILE_CREATE_REFUSED")
                    val sourceDigest = resolver.openInputStream(sourceUri)
                        ?: throw IOException("COPY_SOURCE_OPEN_REFUSED")
                    val destinationDigest = resolver.openOutputStream(createdFileUri, "w")
                        ?: throw IOException("COPY_DESTINATION_OPEN_REFUSED")
                    val copied = sourceDigest.use { input ->
                        destinationDigest.use { output ->
                            copyAndHash(input, output)
                        }
                    }
                    if (file.sizeBytes != null && copied.bytes != file.sizeBytes) {
                        throw IOException("COPY_SOURCE_SIZE_CHANGED")
                    }
                    val readback = resolver.openInputStream(createdFileUri)
                        ?: throw IOException("COPY_READBACK_OPEN_REFUSED")
                    val verified = readback.use { input -> hashStream(input) }
                    if (verified.bytes != copied.bytes) throw IOException("COPY_READBACK_SIZE_MISMATCH")
                    if (verified.sha256 != copied.sha256) throw IOException("COPY_READBACK_HASH_MISMATCH")

                    receipt.append(
                        operationId = plan.operationId,
                        eventType = "FILE_COPIED_VERIFIED",
                        sourceRootSha256 = plan.sourceRootSha256,
                        destinationRootSha256 = plan.destinationRootSha256,
                        targetRootName = plan.targetRootName,
                        pathSha256 = pathHash,
                        planSha256 = plan.planSha256,
                        sourceRefSha256 = sha256(file.sourceUri.toByteArray(Charsets.UTF_8)),
                        destinationRefSha256 = sha256(createdFileUri.toString().toByteArray(Charsets.UTF_8)),
                        bytes = copied.bytes,
                        sourceSha256 = copied.sha256,
                        destinationSha256 = verified.sha256,
                        resultCode = "READBACK_MATCH"
                    )
                    copiedFiles += 1
                    copiedBytes = checkedAdd(copiedBytes, copied.bytes)
                    completedNodes += 1
                    onProgress(SafTreeCopyProgress(completedNodes, totalNodes, pathHash))
                } catch (cancelled: CancellationException) {
                    val deleted = createdFileUri?.let { deleteCreatedDocument(resolver, it) } ?: true
                    receipt.append(
                        operationId = plan.operationId,
                        eventType = "FILE_COPY_INTERRUPTED",
                        sourceRootSha256 = plan.sourceRootSha256,
                        destinationRootSha256 = plan.destinationRootSha256,
                        targetRootName = plan.targetRootName,
                        pathSha256 = pathHash,
                        planSha256 = plan.planSha256,
                        resultCode = if (deleted) "PARTIAL_FILE_REMOVED" else "PARTIAL_FILE_REMAINS"
                    )
                    throw cancelled
                } catch (failure: Exception) {
                    val deleted = createdFileUri?.let { deleteCreatedDocument(resolver, it) } ?: true
                    val code = safeErrorCode(failure)
                    receipt.append(
                        operationId = plan.operationId,
                        eventType = "FILE_COPY_FAILED",
                        sourceRootSha256 = plan.sourceRootSha256,
                        destinationRootSha256 = plan.destinationRootSha256,
                        targetRootName = plan.targetRootName,
                        pathSha256 = pathHash,
                        planSha256 = plan.planSha256,
                        resultCode = if (deleted) code else code + "_PARTIAL_FILE_REMAINS"
                    )
                    throw IOException(code)
                }
            }

            receipt.append(
                operationId = plan.operationId,
                eventType = "COPY_COMPLETED",
                sourceRootSha256 = plan.sourceRootSha256,
                destinationRootSha256 = plan.destinationRootSha256,
                targetRootName = plan.targetRootName,
                planSha256 = plan.planSha256,
                plannedFiles = plan.fileCount,
                plannedDirectories = plan.directoryCount,
                bytes = copiedBytes,
                resultCode = "ALL_FILES_READBACK_VERIFIED"
            )
            val verification = receipt.verify()
            if (!verification.verified) throw IOException("COPY_RECEIPT_CHAIN_INVALID")
            return SafTreeCopyResult(
                operationId = plan.operationId,
                targetRootName = plan.targetRootName,
                copiedFiles = copiedFiles,
                createdDirectories = createdDirectories,
                copiedBytes = copiedBytes,
                receiptFile = receiptFile,
                receiptChainHeadSha256 = verification.chainHeadSha256
            )
        } catch (cancelled: CancellationException) {
            appendTerminalEvent(receipt, plan, "COPY_INTERRUPTED", "USER_OR_PROCESS_CANCELLED")
            throw cancelled
        } catch (failure: Exception) {
            val code = safeErrorCode(failure)
            appendTerminalEvent(receipt, plan, "COPY_FAILED", code)
            throw IOException(code)
        }
    }

    internal fun safePathSegments(relativePath: String): List<String> {
        require(relativePath.isNotBlank()) { "COPY_PATH_EMPTY" }
        val segments = relativePath.split('/')
        require(segments.isNotEmpty()) { "COPY_PATH_EMPTY" }
        segments.forEach { segment ->
            require(segment.isNotBlank() && segment != "." && segment != "..") {
                "COPY_PATH_SEGMENT_INVALID"
            }
            require(segment.length <= 255) { "COPY_PATH_SEGMENT_TOO_LONG" }
            require(segment.none { it == '\\' || it.isISOControl() }) {
                "COPY_PATH_SEGMENT_INVALID"
            }
        }
        return segments
    }

    private fun validateInventory(
        inventory: NovoexportSafInventory.Result,
        sourceTree: Uri
    ): List<Node> {
        val directoriesById = inventory.allDirectories.associateBy { it.documentId }
        require(directoriesById.size == inventory.allDirectories.size) {
            "COPY_DUPLICATE_DIRECTORY_ID"
        }
        val fileIds = inventory.allFiles.map { it.documentId }.toSet()
        require(fileIds.size == inventory.allFiles.size) { "COPY_DUPLICATE_FILE_ID" }
        require(fileIds.intersect(directoriesById.keys).isEmpty()) { "COPY_NODE_ID_KIND_COLLISION" }

        val nodes = mutableListOf<Node>()
        val seenPaths = mutableSetOf<String>()
        val siblingNames = mutableSetOf<String>()
        inventory.allDirectories.forEach { directory ->
            require(directory.documentId.isNotBlank()) { "COPY_DIRECTORY_ID_MISSING" }
            val segments = safePathSegments(directory.relativePath)
            require(segments.last() == directory.name) { "COPY_DIRECTORY_PATH_NAME_MISMATCH" }
            require(!directory.name.contains('/') && !directory.name.contains('\\')) {
                "COPY_DIRECTORY_NAME_INVALID"
            }
            val expectedParentPath = if (directory.parentDocumentId == inventory.rootDocumentId) {
                ""
            } else {
                directoriesById[directory.parentDocumentId]?.relativePath
                    ?: throw IllegalArgumentException("COPY_DIRECTORY_PARENT_UNKNOWN")
            }
            val actualParentPath = segments.dropLast(1).joinToString("/")
            require(actualParentPath == expectedParentPath) { "COPY_DIRECTORY_PATH_PARENT_MISMATCH" }
            require(seenPaths.add(directory.relativePath)) { "COPY_DUPLICATE_TREE_PATH" }
            val siblingKey = directory.parentDocumentId + "\u0000" + directory.name
            require(siblingNames.add(siblingKey)) { "COPY_DUPLICATE_SIBLING_NAME" }
            nodes += Node(
                documentId = directory.documentId,
                parentDocumentId = directory.parentDocumentId
                    ?: throw IllegalArgumentException("COPY_DIRECTORY_PARENT_MISSING"),
                name = directory.name,
                relativePath = directory.relativePath,
                pathSha256 = sha256(directory.relativePath.toByteArray(Charsets.UTF_8)),
                isDirectory = true,
                mimeType = DIRECTORY_MIME_TYPE,
                sizeBytes = null,
                sourceUri = DocumentsContract.buildDocumentUriUsingTree(
                    sourceTree,
                    directory.documentId
                ).toString()
            )
        }
        inventory.allFiles.forEach { file ->
            require(file.documentId.isNotBlank()) { "COPY_FILE_ID_MISSING" }
            val segments = safePathSegments(file.relativePath)
            require(segments.last() == file.name) { "COPY_FILE_PATH_NAME_MISMATCH" }
            require(!file.name.contains('/') && !file.name.contains('\\')) { "COPY_FILE_NAME_INVALID" }
            require(file.mimeType != DIRECTORY_MIME_TYPE) { "COPY_FILE_MARKED_DIRECTORY" }
            require(file.sizeBytes == null || file.sizeBytes >= 0L) { "COPY_FILE_SIZE_INVALID" }
            val parentId = file.parentDocumentId
                ?: throw IllegalArgumentException("COPY_FILE_PARENT_MISSING")
            val expectedParentPath = if (parentId == inventory.rootDocumentId) {
                ""
            } else {
                directoriesById[parentId]?.relativePath
                    ?: throw IllegalArgumentException("COPY_FILE_PARENT_UNKNOWN")
            }
            val actualParentPath = segments.dropLast(1).joinToString("/")
            require(actualParentPath == expectedParentPath) { "COPY_FILE_PATH_PARENT_MISMATCH" }
            require(seenPaths.add(file.relativePath)) { "COPY_DUPLICATE_TREE_PATH" }
            val siblingKey = parentId + "\u0000" + file.name
            require(siblingNames.add(siblingKey)) { "COPY_DUPLICATE_SIBLING_NAME" }
            val sourceUri = Uri.parse(file.uri)
            require(sourceUri.scheme == "content" && sourceUri.authority == sourceTree.authority) {
                "COPY_FILE_SOURCE_URI_INVALID"
            }
            nodes += Node(
                documentId = file.documentId,
                parentDocumentId = parentId,
                name = file.name,
                relativePath = file.relativePath,
                pathSha256 = sha256(file.relativePath.toByteArray(Charsets.UTF_8)),
                isDirectory = false,
                mimeType = file.mimeType,
                sizeBytes = file.sizeBytes,
                sourceUri = file.uri
            )
        }
        return nodes
    }

    private fun hasPersistedGrant(
        context: Context,
        uri: Uri,
        read: Boolean,
        write: Boolean
    ): Boolean = context.contentResolver.persistedUriPermissions.any { grant ->
        grant.uri.normalizeScheme() == uri.normalizeScheme() &&
            (!read || grant.isReadPermission) &&
            (!write || grant.isWritePermission)
    }

    private fun listChildNames(
        resolver: android.content.ContentResolver,
        treeUri: Uri,
        parentDocumentId: String
    ): Set<String> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val projection = arrayOf(Document.COLUMN_DISPLAY_NAME)
        val cursor = resolver.query(childrenUri, projection, null, null, null)
            ?: throw IOException("COPY_DESTINATION_LIST_REFUSED")
        return cursor.use {
            val nameColumn = it.getColumnIndexOrThrow(Document.COLUMN_DISPLAY_NAME)
            buildSet {
                while (it.moveToNext()) {
                    val name = it.getString(nameColumn) ?: throw IOException("COPY_DESTINATION_NAME_MISSING")
                    add(name)
                }
            }
        }
    }

    private fun appendTerminalEvent(
        receipt: SafTreeCopyReceiptStore,
        plan: SafTreeCopyPlan,
        eventType: String,
        resultCode: String
    ) {
        runCatching {
            receipt.append(
                operationId = plan.operationId,
                eventType = eventType,
                sourceRootSha256 = plan.sourceRootSha256,
                destinationRootSha256 = plan.destinationRootSha256,
                targetRootName = plan.targetRootName,
                planSha256 = plan.planSha256,
                plannedFiles = plan.fileCount,
                plannedDirectories = plan.directoryCount,
                resultCode = resultCode
            )
        }
    }

    private fun copyAndHash(
        input: java.io.InputStream,
        output: java.io.OutputStream
    ): StreamDigest {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(STREAM_BUFFER_BYTES)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            total = checkedAdd(total, read.toLong())
            digest.update(buffer, 0, read)
            output.write(buffer, 0, read)
        }
        output.flush()
        return StreamDigest(total, digest.digest().toHex())
    }

    private suspend fun hashStream(input: java.io.InputStream): StreamDigest {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(STREAM_BUFFER_BYTES)
        var total = 0L
        while (true) {
            coroutineContext.ensureActive()
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            total = checkedAdd(total, read.toLong())
            digest.update(buffer, 0, read)
        }
        return StreamDigest(total, digest.digest().toHex())
    }

    private fun deleteCreatedDocument(
        resolver: android.content.ContentResolver,
        uri: Uri
    ): Boolean = try {
        DocumentsContract.deleteDocument(resolver, uri)
    } catch (_: Exception) {
        false
    }

    private fun checkedAdd(current: Long, delta: Long): Long {
        if (delta < 0L || delta > Long.MAX_VALUE - current) {
            throw IOException("COPY_BYTE_COUNT_OVERFLOW")
        }
        return current + delta
    }

    private fun depth(path: String): Int = path.count { it == '/' } + 1

    private fun safeErrorCode(failure: Exception): String {
        val candidate = failure.message ?: return "COPY_PROVIDER_ERROR"
        return if (candidate.matches(Regex("^[A-Z0-9_]{1,80}$"))) {
            candidate
        } else {
            "COPY_PROVIDER_ERROR"
        }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }
}
