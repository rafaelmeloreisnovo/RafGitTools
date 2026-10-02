package com.rafgittools.navigator

import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Private app-local queue state for metadata candidates discovered through SAF.
 * Queue persistence is runtime state, not execution evidence or a corpus-complete claim.
 */
object NovoexportQueueStore {
    const val PENDING = "PENDING"
    const val PROCESSING = "PROCESSING"
    const val COMPLETE = "COMPLETE"
    const val FAILED_RETRYABLE = "FAILED_RETRYABLE"
    const val BLOCKED = "BLOCKED"

    data class Item(
        val id: String,
        val uri: String,
        val documentId: String,
        val name: String,
        val mimeType: String,
        val sizeBytes: Long?,
        val presentInLatestInventory: Boolean,
        val state: String,
        val attempts: Int,
        val lastError: String
    )

    data class Snapshot(
        val schema: String = "rafgittools.novoexport-resumable-queue/v1",
        val treeUri: String,
        val createdEpochMs: Long,
        val updatedEpochMs: Long,
        val items: List<Item>,
        val claimAllowed: Boolean = false
    )

    data class MergeResult(
        val queueFile: File,
        val snapshot: Snapshot,
        val added: Int,
        val preserved: Int,
        val absentFromLatestInventory: Int
    )

    private val gson = Gson()

    fun mergeInventory(
        privateRoot: File,
        inventory: NovoexportSafInventory.Result,
        nowEpochMs: Long = System.currentTimeMillis()
    ): MergeResult {
        require(inventory.state == "INVENTORY_COMPLETE_METADATA_ONLY") {
            "Queue seed requires a completed metadata inventory"
        }
        if (!privateRoot.exists() && !privateRoot.mkdirs()) {
            error("Cannot create private NOVOexport queue directory")
        }
        require(privateRoot.isDirectory) { "Queue root is not a directory" }

        val queueFile = File(privateRoot, "queue-${sha256(inventory.treeUri.toByteArray(Charsets.UTF_8)).take(16)}.json")
        val previous = loadOrNull(queueFile)
        if (previous != null) {
            require(previous.treeUri == inventory.treeUri) {
                "Queue tree URI changed; cross-tree merge is blocked"
            }
        }

        val prior = previous?.items.orEmpty().associateBy { it.id }
        val currentIds = mutableSetOf<String>()
        var added = 0
        var preserved = 0

        val current = inventory.candidateFiles.map { entry ->
            val id = candidateId(inventory.treeUri, entry)
            require(currentIds.add(id)) { "Duplicate candidate identity in SAF inventory: $id" }
            val old = prior[id]
            if (old == null) {
                added += 1
                Item(
                    id = id,
                    uri = entry.uri,
                    documentId = entry.documentId,
                    name = entry.name,
                    mimeType = entry.mimeType,
                    sizeBytes = entry.sizeBytes,
                    presentInLatestInventory = true,
                    state = PENDING,
                    attempts = 0,
                    lastError = "TOKEN_VAZIO"
                )
            } else {
                preserved += 1
                val interrupted = old.state == PROCESSING
                val permissionRestored = old.state == BLOCKED &&
                    old.lastError.startsWith("SAF_PERMISSION_LOST:")
                old.copy(
                    uri = entry.uri,
                    documentId = entry.documentId,
                    name = entry.name,
                    mimeType = entry.mimeType,
                    sizeBytes = entry.sizeBytes,
                    presentInLatestInventory = true,
                    state = when {
                        interrupted -> FAILED_RETRYABLE
                        permissionRestored -> PENDING
                        else -> old.state
                    },
                    lastError = when {
                        interrupted -> "APP_RESTART_DURING_PROCESSING_RETRY_FROM_SOURCE"
                        permissionRestored -> "TOKEN_VAZIO"
                        else -> old.lastError
                    }
                )
            }
        }

        val absent = prior.values
            .filter { it.id !in currentIds }
            .map { it.copy(presentInLatestInventory = false) }

        val snapshot = Snapshot(
            treeUri = inventory.treeUri,
            createdEpochMs = previous?.createdEpochMs ?: nowEpochMs,
            updatedEpochMs = nowEpochMs,
            items = (current + absent).sortedBy { it.id }
        )
        writeSnapshot(queueFile, snapshot)
        return MergeResult(
            queueFile = queueFile,
            snapshot = snapshot,
            added = added,
            preserved = preserved,
            absentFromLatestInventory = absent.size
        )
    }

    fun load(queueFile: File): Snapshot =
        loadOrNull(queueFile) ?: error("No resumable queue snapshot found")

    fun transition(
        queueFile: File,
        itemId: String,
        newState: String,
        error: String = "TOKEN_VAZIO",
        nowEpochMs: Long = System.currentTimeMillis()
    ): Snapshot {
        val current = load(queueFile)
        val index = current.items.indexOfFirst { it.id == itemId }
        require(index >= 0) { "Queue item not found: $itemId" }
        val old = current.items[index]
        require(allowedTransition(old.state, newState)) {
            "Invalid queue transition: ${old.state} -> $newState"
        }
        if (newState == FAILED_RETRYABLE || newState == BLOCKED) {
            require(error.isNotBlank() && error != "TOKEN_VAZIO") {
                "Failure/block transition requires an explicit error"
            }
        }

        val updatedItem = old.copy(
            state = newState,
            attempts = old.attempts + if (newState == PROCESSING) 1 else 0,
            lastError = if (newState == FAILED_RETRYABLE || newState == BLOCKED) error else "TOKEN_VAZIO"
        )
        val items = current.items.toMutableList()
        items[index] = updatedItem
        val updated = current.copy(updatedEpochMs = nowEpochMs, items = items)
        writeSnapshot(queueFile, updated)
        return updated
    }

    fun markPublishedComplete(
        queueFile: File,
        itemId: String,
        publishedState: String,
        driveReadbackVerified: Boolean,
        githubReadbackVerified: Boolean,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Snapshot {
        require(publishedState == "PUBLISHED_READBACK_VERIFIED") {
            "Queue item completion requires a readback-verified publication"
        }
        require(driveReadbackVerified && githubReadbackVerified) {
            "Queue item completion requires successful Drive and Git readback"
        }
        return transition(queueFile, itemId, COMPLETE, nowEpochMs = nowEpochMs)
    }

    internal fun allowedTransition(from: String, to: String): Boolean = when (from) {
        PENDING -> to == PROCESSING || to == BLOCKED
        PROCESSING -> to == COMPLETE || to == FAILED_RETRYABLE || to == BLOCKED
        FAILED_RETRYABLE -> to == PROCESSING || to == BLOCKED
        BLOCKED -> to == PENDING
        COMPLETE -> false
        else -> false
    }

    private fun candidateId(treeUri: String, entry: NovoexportSafInventory.Entry): String {
        val canonical = listOf(
            treeUri,
            entry.documentId,
            entry.uri,
            entry.name,
            entry.mimeType,
            entry.sizeBytes?.toString() ?: "TOKEN_VAZIO_SIZE"
        ).joinToString("\n")
        return sha256(canonical.toByteArray(Charsets.UTF_8))
    }

    private fun loadOrNull(queueFile: File): Snapshot? {
        val backup = File(queueFile.parentFile, queueFile.name + ".bak")
        val source = when {
            queueFile.isFile -> queueFile
            backup.isFile -> backup
            else -> return null
        }
        val snapshot = gson.fromJson(source.readText(Charsets.UTF_8), Snapshot::class.java)
            ?: error("Queue snapshot is empty")
        require(snapshot.schema == "rafgittools.novoexport-resumable-queue/v1") {
            "Unsupported queue schema: ${snapshot.schema}"
        }
        require(!snapshot.claimAllowed) { "Queue runtime state cannot promote claims" }
        return snapshot
    }

    private fun writeSnapshot(queueFile: File, snapshot: Snapshot) {
        val parent = queueFile.parentFile ?: error("Queue file has no parent")
        if (!parent.exists() && !parent.mkdirs()) error("Cannot create queue parent")
        val temp = File(parent, queueFile.name + ".tmp")
        val backup = File(parent, queueFile.name + ".bak")
        val bytes = (gson.toJson(snapshot) + "\n").toByteArray(Charsets.UTF_8)

        FileOutputStream(temp).use { out ->
            out.write(bytes)
            out.flush()
            out.fd.sync()
        }

        if (backup.exists() && !backup.delete()) error("Cannot clear stale queue backup")
        if (queueFile.exists() && !queueFile.renameTo(backup)) {
            temp.delete()
            error("Cannot move current queue to recovery backup")
        }
        if (!temp.renameTo(queueFile)) {
            if (backup.exists()) backup.renameTo(queueFile)
            temp.delete()
            error("Cannot promote queue snapshot")
        }
        if (backup.exists() && !backup.delete()) {
            // A valid current queue already exists; stale backup cleanup is non-fatal.
        }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
