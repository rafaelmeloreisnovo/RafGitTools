package com.rafgittools.bridge

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.google.gson.GsonBuilder
import com.rafgittools.navigator.ConversationProjectPackBuilder
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Writes only derived project-pack artifacts to a user-selected SAF tree and
 * verifies each write by SHA-256 readback. Raw conversation source bytes are
 * never copied by this exporter.
 */
object PrivateDerivedTreeExporter {
    data class ExportedArtifact(
        val name: String,
        val bytes: Long,
        val sha256: String,
        val readbackVerified: Boolean
    )

    data class Receipt(
        val schema: String = "rafgittools.private-derived-tree-export/v1",
        val destinationProviderAuthority: String,
        val generationLabel: String,
        val exportedArtifacts: List<ExportedArtifact>,
        val rawSourceCopied: Boolean = false,
        val privacyClass: String = "PRIVATE_DEFAULT_DENY",
        val claimAllowed: Boolean = false,
        val state: String = "DERIVED_EXPORT_READBACK_VERIFIED",
        val createdAtEpochMs: Long
    )

    data class Result(
        val state: String,
        val destinationProviderAuthority: String,
        val exportedArtifactCount: Int,
        val localReceiptFile: File
    )

    private val gson = GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()

    @Throws(IOException::class)
    fun exportProjectPack(
        context: Context,
        destinationTree: Uri,
        pack: ConversationProjectPackBuilder.Result,
        createdAtEpochMs: Long = System.currentTimeMillis()
    ): Result {
        require(pack.generationDir.isDirectory) { "Project-pack generation directory is missing" }
        val resolver = context.contentResolver
        val treeDocumentId = DocumentsContract.getTreeDocumentId(destinationTree)
        val rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(destinationTree, treeDocumentId)
        val folderName = "RAFGITTOOLS_GPT_PROJECT_PACK_${createdAtEpochMs}"
        val folderUri = DocumentsContract.createDocument(
            resolver,
            rootDocumentUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            folderName
        ) ?: throw IOException("Provider refused private project-pack folder creation")

        val artifacts = buildList {
            add(pack.manifestFile)
            add(pack.receiptFile)
            pack.shards.forEach { shard -> add(File(pack.generationDir, shard.filename)) }
        }
        artifacts.forEach { file ->
            if (!file.isFile) throw IOException("Derived project-pack artifact is missing: ${file.path}")
        }

        val exported = artifacts.map { source ->
            val destinationUri = DocumentsContract.createDocument(
                resolver,
                folderUri,
                mimeTypeFor(source),
                source.name
            ) ?: throw IOException("Provider refused project-pack artifact creation: ${source.name}")

            resolver.openOutputStream(destinationUri, "w")?.use { output ->
                source.inputStream().buffered(64 * 1024).use { input ->
                    input.copyTo(output, 64 * 1024)
                    output.flush()
                }
            } ?: throw IOException("Provider refused project-pack write: ${source.name}")

            val expected = sha256(source)
            val readback = resolver.openInputStream(destinationUri)?.use(::sha256)
                ?: throw IOException("Provider refused project-pack readback: ${source.name}")
            if (!readback.equals(expected, ignoreCase = true)) {
                throw IOException("Project-pack SHA-256 readback mismatch: ${source.name}")
            }
            ExportedArtifact(
                name = source.name,
                bytes = source.length(),
                sha256 = expected,
                readbackVerified = true
            )
        }

        val receipt = Receipt(
            destinationProviderAuthority = destinationTree.authority ?: "TOKEN_VAZIO_PROVIDER_AUTHORITY",
            generationLabel = pack.generationDir.name,
            exportedArtifacts = exported,
            createdAtEpochMs = createdAtEpochMs
        )
        val receiptRoot = File(context.filesDir, "gpt-project-pack-export-receipts")
        if (!receiptRoot.exists() && !receiptRoot.mkdirs()) {
            throw IOException("Cannot create project-pack export receipt directory")
        }
        val localReceipt = File(receiptRoot, "gpt-project-pack-export-$createdAtEpochMs.receipt.json")
        localReceipt.writeText(gson.toJson(receipt) + "\n", Charsets.UTF_8)

        return Result(
            state = receipt.state,
            destinationProviderAuthority = receipt.destinationProviderAuthority,
            exportedArtifactCount = exported.size,
            localReceiptFile = localReceipt
        )
    }

    private fun mimeTypeFor(file: File): String = when {
        file.name.endsWith(".jsonl", ignoreCase = true) -> "application/x-ndjson"
        file.name.endsWith(".json", ignoreCase = true) -> "application/json"
        else -> "application/octet-stream"
    }

    private fun sha256(file: File): String = file.inputStream().buffered(64 * 1024).use(::sha256)

    private fun sha256(input: java.io.InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
