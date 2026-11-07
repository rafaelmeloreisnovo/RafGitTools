package com.rafgittools.setupwizard

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.AtomicFile
import com.google.gson.GsonBuilder
import java.io.File
import java.io.IOException

/**
 * Private app-storage staging from a manually selected Google Drive SAF tree.
 * No source bytes, Drive writes, V3 replacement or account-wide access.
 */
object HyperAtlasV4SafLocal {
    data class Receipt(val completedDirectories: Int, val records: Long,
                       val state: String, val localOnly: Boolean = true,
                       val physicalE2E: Boolean = false)
    private val gson = GsonBuilder().serializeNulls().disableHtmlEscaping().create()
    private fun hash(s: String) = HyperAtlasV4Incremental.digest(s)

    private class LocalStore(private val directory: File,
                             private val scope: String) : HyperAtlasV4Incremental.Store {
        private val checkpoint = AtomicFile(File(directory, "CHECKPOINT.json"))
        override fun load(): HyperAtlasV4Incremental.Checkpoint? {
            if (!checkpoint.baseFile.exists() &&
                !File(directory, "CHECKPOINT.json.bak").exists()) return null
            return try {
                checkpoint.openRead().use { input ->
                    gson.fromJson(input.reader(Charsets.UTF_8),
                        HyperAtlasV4Incremental.Checkpoint::class.java)
                } ?: throw IOException("V4_CHECKPOINT_NULL")
            } catch (e: Exception) {
                throw IOException("V4_CHECKPOINT_CORRUPT", e)
            }
        }
        override fun commitDirectory(parentId: String, rows: List<HyperAtlasV4Incremental.Row>) {
            val name = "batch-" + hash(scope + "\u0000" + parentId) + ".jsonl"
            val file = AtomicFile(File(directory, name))
            val data = rows.joinToString(separator = "\n", postfix = if (rows.isEmpty()) "" else "\n") {
                gson.toJson(it)
            }.toByteArray(Charsets.UTF_8)
            val expected = hashBytes(data)
            // Exact replay is safe; divergent replay is rejected, never overwritten.
            if (file.baseFile.exists() || File(directory, name + ".bak").exists()) {
                val prior = file.openRead().use { it.readBytes() }
                if (!prior.contentEquals(data)) throw IOException("V4_DIVERGENT_BATCH_REPLAY")
                return
            }
            writeAtomically(file, data)
            val readback = file.openRead().use { it.readBytes() }
            if (readback.size != data.size || hashBytes(readback) != expected) {
                throw IOException("V4_BATCH_READBACK_MISMATCH")
            }
        }
        override fun save(state: HyperAtlasV4Incremental.Checkpoint) {
            if (state.scope != scope) throw IOException("V4_SCOPE_MISMATCH")
            val bytes = gson.toJson(state).toByteArray(Charsets.UTF_8)
            writeAtomically(checkpoint, bytes)
            val returned = checkpoint.openRead().use { it.readBytes() }
            if (!bytes.contentEquals(returned)) throw IOException("V4_CHECKPOINT_READBACK_MISMATCH")
        }
        private fun hashBytes(bytes: ByteArray): String =
            java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
        private fun writeAtomically(destination: AtomicFile, bytes: ByteArray) {
            val out = destination.startWrite()
            try {
                out.write(bytes)
                destination.finishWrite(out)
            } catch (e: Exception) {
                destination.failWrite(out)
                throw e
            }
        }
    }

    @Throws(IOException::class)
    fun scan(context: Context, selectedTree: Uri): Receipt {
        require(selectedTree.scheme == "content" &&
            GoogleDriveSafGate.isGoogleDrive(context, selectedTree)) { "V4_DRIVE_TREE_REQUIRED" }
        val resolver = context.contentResolver
        val root = DocumentsContract.getTreeDocumentId(selectedTree)
        require(root.isNotEmpty()) { "V4_MISSING_ROOT" }
        val scope = hash(selectedTree.toString())
        val workspace = File(context.filesDir, "hyperatlas-v4/$scope")
        if (!workspace.isDirectory && !workspace.mkdirs()) throw IOException("V4_PRIVATE_DIR_UNAVAILABLE")
        val source = object : HyperAtlasV4Incremental.Source {
            override fun children(parentId: String, maxChildren: Int): List<HyperAtlasV4Incremental.Child> {
                val uri = DocumentsContract.buildChildDocumentsUriUsingTree(selectedTree, parentId)
                val cols = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
                )
                val out = ArrayList<HyperAtlasV4Incremental.Child>()
                (resolver.query(uri, cols, null, null, null)
                    ?: throw IOException("V4_SAF_NULL_CURSOR")).use { c ->
                    val idCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    if (c.extras.getBoolean(DocumentsContract.EXTRA_LOADING, false))
                        throw IOException("V4_SAF_DIRECTORY_LOADING_RETRY")
                    while (c.moveToNext()) {
                        if (out.size >= maxChildren) throw IOException("V4_SAF_CHILD_LIMIT")
                        val id = c.getString(idCol) ?: throw IOException("V4_SAF_MISSING_CHILD_ID")
                        val name = c.getString(nameCol) ?: throw IOException("V4_SAF_MISSING_NAME")
                        val mime = c.getString(mimeCol) ?: "application/octet-stream"
                        val size = if (sizeCol >= 0 && !c.isNull(sizeCol))
                            c.getLong(sizeCol).takeIf { it >= 0 } else null
                        out += HyperAtlasV4Incremental.Child(id, name, mime, size,
                            mime == DocumentsContract.Document.MIME_TYPE_DIR)
                    }
                    if (c.extras.getBoolean(DocumentsContract.EXTRA_LOADING, false))
                        throw IOException("V4_SAF_DIRECTORY_LOADING_RETRY")
                }
                return out
            }
        }
        val state = HyperAtlasV4Incremental.run(scope, root, source, LocalStore(workspace, scope))
        return Receipt(state.completedDirectories, state.records, state.state)
    }
}
