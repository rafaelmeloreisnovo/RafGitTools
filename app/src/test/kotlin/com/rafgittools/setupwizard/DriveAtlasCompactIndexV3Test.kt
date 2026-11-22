package com.rafgittools.setupwizard

import com.google.gson.JsonParser
import com.rafgittools.navigator.NovoexportSafInventory
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

class DriveAtlasCompactIndexV3Test {
    private fun sha(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun fixture(files: Int = 14): NovoexportSafInventory.Result {
        val entries = (0 until files).map { i ->
            val name = when (i) {
                0 -> "BackReal.zipraf"
                1 -> "snapshot.zip.t0.zip.004"
                else -> "source-%04d.txt".format(java.util.Locale.ROOT, i)
            }
            NovoexportSafInventory.Entry(
                uri = "content://SECRET_TOKEN/source$i",
                documentId = "SECRET_DOC_$i",
                name = name,
                mimeType = if (i == 0) "application/x-zip" else "application/octet-stream",
                sizeBytes = when (i) {
                    0 -> 4_060_826_692L
                    1 -> null
                    else -> 0L
                },
                relativePath = "Zip/$name",
                parentDocumentId = "SECRET_PARENT",
                semanticTokens = listOf("zip", name),
                semanticPassMask = 0x1f,
                semanticKindFlags = 2,
                pathComponents = 2
            )
        }
        val dir = NovoexportSafInventory.DirectoryEntry(
            documentId = "SECRET_PARENT",
            parentDocumentId = null,
            name = "Zip",
            relativePath = "Zip",
            semanticTokens = listOf("Zip"),
            semanticPassMask = 0x1f,
            semanticKindFlags = 1,
            pathComponents = 1
        )
        return NovoexportSafInventory.Result(
            treeUri = "content://SECRET_TOKEN/root",
            visitedDocuments = files + 1,
            visitedDirectories = 1,
            candidateFiles = emptyList(),
            knownCandidateBytes = 0,
            unknownSizeCandidateFiles = 0,
            allFiles = entries,
            allDirectories = listOf(dir),
            knownTotalBytes = 4_060_826_692L,
            unknownSizeFiles = 1
        )
    }

    @Test fun createsReconstructibleShardsAndHashesWithoutZipDownloads() {
        val written = linkedMapOf<String, ByteArray>()
        val manifest = DriveAtlasCompactIndexV3.build(fixture(18), 512) { name, bytes ->
            written[name] = bytes
            sha(bytes)
        }
        val root = JsonParser.parseString(String(manifest)).asJsonObject
        assertEquals("rafgittools.hyperatlas.saf-compact/v3", root.get("schema").asString)
        assertEquals(19, root.get("records").asInt)
        assertTrue(root.getAsJsonArray("parts").size() > 1)
        assertEquals(2, root.get("archive_candidates").asInt)
        assertEquals(1, root.get("unknown_size_files").asInt)
        assertEquals(4_060_826_692L, root.get("known_file_bytes").asLong)
        assertFalse(root.get("source_bytes_read").asBoolean)
        assertFalse(root.get("archive_members_read").asBoolean)
        assertFalse(root.get("claim_allowed").asBoolean)
        val merged = ByteArrayOutputStream()
        val decoded = ArrayList<String>()
        for (part in root.getAsJsonArray("parts")) {
            val obj = part.asJsonObject
            val bytes = written.getValue(obj.get("name").asString)
            assertEquals(sha(bytes), obj.get("sha256").asString)
            assertTrue(bytes.size <= 512)
            merged.write(bytes)
            decoded += String(bytes).lineSequence().filter { it.isNotBlank() }.toList()
        }
        assertEquals(19, decoded.size)
        assertEquals(19, decoded.map {
            JsonParser.parseString(it).asJsonObject.get("path").asString
        }.distinct().size)
        assertEquals(sha(merged.toByteArray()), root.get("index_sha256").asString)
        val all = decoded.joinToString("\n")
        assertFalse(all.contains("SECRET_"))
        assertFalse(all.contains("content://"))
        assertTrue(all.contains("ZIPRAF_CANDIDATE_UNVERIFIED_FORMAT"))
        assertTrue(all.contains("SPLIT_ARCHIVE_PART"))
    }

    @Test fun stopsManifestOnReadbackMismatch() {
        val error = runCatching {
            DriveAtlasCompactIndexV3.build(fixture(2), 512) { _, _ -> "0".repeat(64) }
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(error!!.message.orEmpty().contains("SHARD_READBACK_MISMATCH"))
    }

    @Test fun refusesUnsafeAndDuplicatePaths() {
        val source = fixture(2)
        assertTrue(runCatching {
            DriveAtlasCompactIndexV3.build(
                source.copy(allFiles = listOf(source.allFiles.first().copy(relativePath = "../escape"))),
                512
            ) { _, bytes -> sha(bytes) }
        }.isFailure)
        assertTrue(runCatching {
            DriveAtlasCompactIndexV3.build(
                source.copy(allFiles = listOf(source.allFiles.first(), source.allFiles.first())),
                512
            ) { _, bytes -> sha(bytes) }
        }.exceptionOrNull()?.message.orEmpty().contains("DUPLICATE_PATH"))
    }

    @Test fun rejectsOversizedRecordsAndTooSmallBudget() {
        assertTrue(runCatching {
            DriveAtlasCompactIndexV3.build(fixture(2), 128) { _, bytes -> sha(bytes) }
        }.isFailure)
        val source = fixture(1)
        val oversized = source.allFiles.first().copy(
            semanticTokens = listOf("x".repeat(2000))
        )
        assertTrue(runCatching {
            DriveAtlasCompactIndexV3.build(
                source.copy(allFiles = listOf(oversized)), 512
            ) { _, bytes -> sha(bytes) }
        }.exceptionOrNull()?.message.orEmpty().contains("ROW_EXCEEDS_SHARD_LIMIT"))
    }

    @Test fun staysDeterministicAcrossRepeatedBuilds() {
        fun execute(): Pair<String, String> {
            val received = linkedMapOf<String, String>()
            val manifest = DriveAtlasCompactIndexV3.build(fixture(5), 1024) { name, bytes ->
                sha(bytes).also { received[name] = it }
            }
            return String(manifest) to received.toString()
        }
        assertEquals(execute(), execute())
    }
}
