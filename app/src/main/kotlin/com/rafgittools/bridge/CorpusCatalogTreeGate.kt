package com.rafgittools.bridge

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.google.gson.GsonBuilder
import java.io.File
import java.io.IOException
import java.security.MessageDigest

data class CatalogExportedFile(
    val name: String,
    val bytes: Long,
    val sha256: String,
    val verifiedByReadback: Boolean
)

data class CatalogTreeExportReceipt(
    val schemaVersion: String = "1.0.0",
    val destinationProviderAuthority: String,
    val rawCorpusCopied: Boolean = false,
    val exportedArtifacts: List<CatalogExportedFile>,
    val state: String = "CATALOG_EXPORTED_VERIFIED",
    val claimAllowed: Boolean = false,
    val createdAtEpochMs: Long
)

data class CatalogTreeExportResult(
    val localReceiptFile: File,
    val destinationProviderAuthority: String,
    val exportedArtifactCount: Int
)

/**
 * Explicit SAF tree export gate.
 *
 * Only catalog artifacts are written. Raw corpus bytes are never copied by this gate.
 */
object CorpusCatalogTreeGate {
    private val gson = GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()

    @Throws(IOException::class)
    fun exportCatalog(
        context: Context,
        treeUri: Uri,
        artifacts: List<File>,
        createdAtEpochMs: Long = System.currentTimeMillis()
    ): CatalogTreeExportResult {
        require(artifacts.isNotEmpty()) { "At least one catalog artifact is required" }
        artifacts.forEach {
            if (!it.isFile) throw IOException("Catalog artifact is missing: ${it.name}")
        }

        val resolver = context.contentResolver
        val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId)
        val exported = artifacts.map { source ->
            val destinationUri = DocumentsContract.createDocument(
                resolver,
                parentDocumentUri,
                "application/json",
                source.name
            ) ?: throw IOException("Provider refused catalog document creation")

            resolver.openOutputStream(destinationUri, "w")?.use { output ->
                source.inputStream().buffered(64 * 1024).use { input ->
                    input.copyTo(output, 64 * 1024)
                    output.flush()
                }
            } ?: throw IOException("Provider refused catalog document write")

            val expectedSha = sha256(source)
            val readbackSha = resolver.openInputStream(destinationUri)?.use { input ->
                sha256(input)
            } ?: throw IOException("Provider refused catalog readback")

            if (!readbackSha.equals(expectedSha, ignoreCase = true)) {
                throw IOException("Catalog export SHA-256 readback mismatch")
            }

            CatalogExportedFile(
                name = source.name,
                bytes = source.length(),
                sha256 = expectedSha,
                verifiedByReadback = true
            )
        }

        val receipt = CatalogTreeExportReceipt(
            destinationProviderAuthority = treeUri.authority ?: "TOKEN_VAZIO_PROVIDER_AUTHORITY",
            exportedArtifacts = exported,
            createdAtEpochMs = createdAtEpochMs
        )

        val receiptDir = File(context.filesDir, "corpus-catalog-receipts")
        if (!receiptDir.exists() && !receiptDir.mkdirs()) {
            throw IOException("Could not create corpus catalog receipt directory")
        }
        val receiptFile = File(receiptDir, "catalog-export-$createdAtEpochMs.receipt.json")
        receiptFile.writeText(gson.toJson(receipt) + "\n", Charsets.UTF_8)

        return CatalogTreeExportResult(
            localReceiptFile = receiptFile,
            destinationProviderAuthority = receipt.destinationProviderAuthority,
            exportedArtifactCount = exported.size
        )
    }

    private fun sha256(file: File): String = file.inputStream().buffered(64 * 1024).use { sha256(it) }

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
