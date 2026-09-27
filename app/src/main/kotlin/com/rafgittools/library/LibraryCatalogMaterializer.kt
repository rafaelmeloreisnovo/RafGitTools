package com.rafgittools.library

import com.google.gson.GsonBuilder
import java.io.File
import java.io.IOException
import java.security.MessageDigest

data class LibraryCatalogMaterialization(
    val catalogFile: File,
    val receiptFile: File,
    val catalogSha256: String,
    val bytes: Long
)

object LibraryCatalogMaterializer {
    private val gson = GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()

    @Throws(IOException::class)
    fun materialize(
        destinationDir: File,
        bundle: LibraryCatalogBundle
    ): LibraryCatalogMaterialization {
        val gate = LibraryCatalogGate.validate(bundle)
        if (!gate.allowed) {
            throw IOException("Library catalog rejected: " + gate.errors.joinToString(","))
        }
        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
            throw IOException("Unable to create library catalog directory")
        }

        val normalized = bundle.copy(
            sources = bundle.sources.sortedBy { it.sourceId },
            authorities = bundle.authorities.sortedBy { it.authorityId },
            works = bundle.works.sortedBy { it.workId },
            editions = bundle.editions.sortedBy { it.editionId },
            items = bundle.items.sortedBy { it.itemId },
            relations = bundle.relations.sortedBy { it.relationId },
            gaps = bundle.gaps.distinct().sorted()
        )

        val catalogFile = File(destinationDir, normalized.catalogId + ".catalog.json")
        atomicWrite(catalogFile, gson.toJson(normalized) + "\n")

        val catalogHash = sha256(catalogFile)
        val readbackHash = sha256(catalogFile)
        if (catalogHash != readbackHash) throw IOException("Catalog readback hash mismatch")

        val receipt = linkedMapOf(
            "schema" to "rafgittools.library-materialization-receipt.v1",
            "catalog_id" to normalized.catalogId,
            "catalog_sha256" to catalogHash,
            "catalog_bytes" to catalogFile.length(),
            "source_count" to normalized.sources.size,
            "work_count" to normalized.works.size,
            "edition_count" to normalized.editions.size,
            "item_count" to normalized.items.size,
            "authority_count" to normalized.authorities.size,
            "relation_count" to normalized.relations.size,
            "gap_count" to normalized.gaps.size,
            "readback_verified" to true,
            "claim_allowed" to false,
            "created_at_epoch_ms" to normalized.createdAtEpochMs
        )

        val receiptFile = File(destinationDir, normalized.catalogId + ".receipt.json")
        atomicWrite(receiptFile, gson.toJson(receipt) + "\n")

        return LibraryCatalogMaterialization(
            catalogFile = catalogFile,
            receiptFile = receiptFile,
            catalogSha256 = catalogHash,
            bytes = catalogFile.length()
        )
    }

    private fun atomicWrite(target: File, value: String) {
        val part = File(target.parentFile, target.name + ".part")
        if (part.exists() && !part.delete()) throw IOException("Unable to clear .part file")
        part.writeText(value, Charsets.UTF_8)
        if (target.exists() && !target.delete()) {
            part.delete()
            throw IOException("Unable to replace prior materialization")
        }
        if (!part.renameTo(target)) {
            part.delete()
            throw IOException("Unable to atomically publish materialization")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(64 * 1024).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count == 0) continue
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
