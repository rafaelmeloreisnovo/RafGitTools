package com.rafgittools.bridge

import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.security.MessageDigest
import java.util.UUID

data class JsonStructuralVector(
    val objects: Long = 0,
    val arrays: Long = 0,
    val names: Long = 0,
    val strings: Long = 0,
    val numbers: Long = 0,
    val booleans: Long = 0,
    val nulls: Long = 0,
    val maxDepth: Int = 0
)

data class CorpusIntakeManifest(
    val schemaVersion: String = "1.0.0",
    val intakeId: String,
    val publicRiskHandle: String,
    val sourceKind: String = "ANDROID_SAF_DOCUMENT",
    val sourceProviderAuthority: String,
    val sourceDisplayName: String,
    val stagedFileName: String,
    val stagedBytes: Long,
    val sourceSha256: String,
    val contentKind: String,
    val structuralVectorKind: String,
    val structuralVector: JsonStructuralVector?,
    val custodyEnvelopeSchema: String = "rmr-zipraf-evidence-envelope-v1",
    val custodyEnvelopeState: String = "TOKEN_VAZIO_NOT_SEALED",
    val custodyEnvelopeAuthority: String = "rafaelmeloreisnovo/papers",
    val semanticVectorProvider: String = "TOKEN_VAZIO_EXPLICIT_PROVIDER_REQUIRED",
    val privateDriveCatalogDestination: String = "TOKEN_VAZIO_EXPLICIT_USER_BINDING_REQUIRED",
    val privacyState: String = "PRIVATE_LOCAL_ONLY",
    val publicProjectionContainsRawHash: Boolean = false,
    val publicProjectionContainsSourceName: Boolean = false,
    val riskClasses: List<String>,
    val claimAllowed: Boolean = false,
    val createdAtEpochMs: Long
)

data class PublicRiskProjection(
    val schemaVersion: String = "1.0.0",
    val riskHandle: String,
    val intakeState: String,
    val riskClasses: List<String>,
    val mitigationState: String,
    val rawCorpusExposed: Boolean = false,
    val sourceNameExposed: Boolean = false,
    val sourceHashExposed: Boolean = false,
    val privateEvidencePointer: String = "PRIVATE_CUSTODY_RECEIPT_REQUIRED",
    val claimAllowed: Boolean = false
)

data class CorpusIntakeResult(
    val manifestFile: File,
    val publicProjectionFile: File,
    val intakeId: String,
    val publicRiskHandle: String,
    val contentKind: String,
    val structuralVector: JsonStructuralVector?
)

/**
 * Local/private corpus catalog gate.
 *
 * This gate never publishes corpus bytes, JSON strings, JSON field names, source names or source
 * hashes. For JSON it computes only streaming structural counts. Semantic embeddings remain an
 * explicit TOKEN_VAZIO until a separately-authorized vector provider is bound.
 */
object CorpusIntakeGate {
    private val gson = GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()

    @Throws(IOException::class)
    fun catalog(
        stagedFile: File,
        sourceProviderAuthority: String?,
        sourceDisplayName: String,
        expectedSha256: String,
        createdAtEpochMs: Long = System.currentTimeMillis()
    ): CorpusIntakeResult {
        if (!stagedFile.isFile) throw IOException("Staged corpus file is missing")

        val readbackSha256 = sha256(stagedFile)
        if (!readbackSha256.equals(expectedSha256, ignoreCase = true)) {
            throw IOException("Corpus SHA-256 changed after staging")
        }

        val contentKind = classify(stagedFile)
        val structuralVector = when (contentKind) {
            "JSON" -> scanJsonStructure(stagedFile)
            else -> null
        }

        val riskHandle = "RISK-" + UUID.randomUUID().toString()
        val intakeId = "INTAKE-" + readbackSha256.take(24)
        val risks = buildList {
            add("RAW_CORPUS_PRIVATE")
            add("PUBLICATION_REQUIRES_SANITIZED_PROJECTION")
            add("SEMANTIC_VECTOR_PROVIDER_UNBOUND")
            if (contentKind == "ZIP") add("ARCHIVE_MEMBER_SCAN_NOT_RUN")
            if (contentKind != "JSON") add("STRUCTURAL_JSON_VECTOR_NOT_APPLICABLE")
        }

        val manifest = CorpusIntakeManifest(
            intakeId = intakeId,
            publicRiskHandle = riskHandle,
            sourceProviderAuthority = sourceProviderAuthority
                ?.takeIf { it.isNotBlank() }
                ?: "TOKEN_VAZIO_PROVIDER_AUTHORITY",
            sourceDisplayName = sourceDisplayName,
            stagedFileName = stagedFile.name,
            stagedBytes = stagedFile.length(),
            sourceSha256 = readbackSha256,
            contentKind = contentKind,
            structuralVectorKind = if (contentKind == "JSON") {
                "JSON_TOKEN_COUNTS_V1"
            } else {
                "TOKEN_VAZIO_STRUCTURAL_VECTOR_NOT_RUN"
            },
            structuralVector = structuralVector,
            riskClasses = risks,
            createdAtEpochMs = createdAtEpochMs
        )

        val projection = PublicRiskProjection(
            riskHandle = riskHandle,
            intakeState = "PRIVATE_CATALOGED",
            riskClasses = risks,
            mitigationState = "REVIEW_REQUIRED"
        )

        val catalogDir = File(
            stagedFile.parentFile ?: throw IOException("Staging parent is missing"),
            "corpus-catalog"
        )
        if (!catalogDir.exists() && !catalogDir.mkdirs()) {
            throw IOException("Could not create private corpus catalog directory")
        }

        val manifestFile = File(catalogDir, "$intakeId.private-manifest.json")
        val publicProjectionFile = File(catalogDir, "$riskHandle.public-risk.json")
        atomicWriteJson(manifestFile, manifest)
        atomicWriteJson(publicProjectionFile, projection)

        return CorpusIntakeResult(
            manifestFile = manifestFile,
            publicProjectionFile = publicProjectionFile,
            intakeId = intakeId,
            publicRiskHandle = riskHandle,
            contentKind = contentKind,
            structuralVector = structuralVector
        )
    }

    private fun classify(file: File): String = when (file.extension.lowercase()) {
        "json" -> "JSON"
        "zip" -> "ZIP"
        "txt", "md", "csv", "tsv" -> "TEXT"
        else -> "BINARY_OR_UNKNOWN"
    }

    private data class MutableJsonStats(
        var objects: Long = 0,
        var arrays: Long = 0,
        var names: Long = 0,
        var strings: Long = 0,
        var numbers: Long = 0,
        var booleans: Long = 0,
        var nulls: Long = 0,
        var maxDepth: Int = 0
    )

    @Throws(IOException::class)
    private fun scanJsonStructure(file: File): JsonStructuralVector {
        val stats = MutableJsonStats()
        FileInputStream(file).use { input ->
            JsonReader(InputStreamReader(input, Charsets.UTF_8).buffered(64 * 1024)).use { reader ->
                consumeJsonValue(reader, stats, 0)
                if (reader.peek() != JsonToken.END_DOCUMENT) {
                    throw IOException("Trailing JSON content after top-level value")
                }
            }
        }
        return JsonStructuralVector(
            objects = stats.objects,
            arrays = stats.arrays,
            names = stats.names,
            strings = stats.strings,
            numbers = stats.numbers,
            booleans = stats.booleans,
            nulls = stats.nulls,
            maxDepth = stats.maxDepth
        )
    }

    @Throws(IOException::class)
    private fun consumeJsonValue(reader: JsonReader, stats: MutableJsonStats, depth: Int) {
        if (depth > 512) throw IOException("JSON nesting exceeds bounded depth 512")
        if (depth > stats.maxDepth) stats.maxDepth = depth

        when (reader.peek()) {
            JsonToken.BEGIN_OBJECT -> {
                stats.objects++
                reader.beginObject()
                while (reader.hasNext()) {
                    reader.nextName()
                    stats.names++
                    consumeJsonValue(reader, stats, depth + 1)
                }
                reader.endObject()
            }
            JsonToken.BEGIN_ARRAY -> {
                stats.arrays++
                reader.beginArray()
                while (reader.hasNext()) {
                    consumeJsonValue(reader, stats, depth + 1)
                }
                reader.endArray()
            }
            JsonToken.STRING -> {
                stats.strings++
                reader.skipValue()
            }
            JsonToken.NUMBER -> {
                stats.numbers++
                reader.skipValue()
            }
            JsonToken.BOOLEAN -> {
                stats.booleans++
                reader.nextBoolean()
            }
            JsonToken.NULL -> {
                stats.nulls++
                reader.nextNull()
            }
            else -> throw IOException("Unexpected JSON token: ${reader.peek()}")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).buffered(64 * 1024).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @Throws(IOException::class)
    private fun atomicWriteJson(target: File, value: Any) {
        val part = File(target.parentFile, target.name + ".part")
        if (part.exists() && !part.delete()) throw IOException("Could not clear prior .part")
        part.writeText(gson.toJson(value) + "\n", Charsets.UTF_8)
        if (target.exists() && !target.delete()) {
            part.delete()
            throw IOException("Could not replace prior catalog artifact")
        }
        if (!part.renameTo(target)) {
            part.delete()
            throw IOException("Could not atomically promote catalog artifact")
        }
    }
}
