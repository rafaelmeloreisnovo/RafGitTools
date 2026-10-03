package com.rafgittools.navigator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.file.Files

class ConversationProjectPackBuilderTest {
    private fun inventory(indices: List<Int>): NovoexportSafInventory.Result {
        val entries = indices.map { index ->
            NovoexportSafInventory.Entry(
                uri = "content://private.provider/document/$index",
                documentId = "private-document-$index",
                name = NovoexportConversationCorpus.expectedFileName(index),
                mimeType = "application/json",
                sizeBytes = 32L,
                relativePath = "01_SISTEMA_CORPUS_CUSTODIA/${NovoexportConversationCorpus.expectedFileName(index)}",
                parentDocumentId = "private-custody-folder"
            )
        }
        return NovoexportSafInventory.Result(
            treeUri = "content://private.provider/tree/private-root",
            visitedDocuments = entries.size,
            visitedDirectories = 1,
            candidateFiles = entries,
            knownCandidateBytes = entries.sumOf { it.sizeBytes ?: 0L },
            unknownSizeCandidateFiles = 0,
            rootDocumentId = "private-root",
            allFiles = entries,
            knownTotalBytes = entries.sumOf { it.sizeBytes ?: 0L },
            unknownSizeFiles = 0
        )
    }

    private fun sourceFor(entry: NovoexportSafInventory.Entry): ByteArrayInputStream {
        val index = NovoexportConversationCorpus.canonicalIndex(entry.name) ?: error("not canonical")
        val json = """[{"id":"c$index","title":"conversation $index","mapping":{}}]"""
        return ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))
    }

    @Test
    fun buildsComplete51FilePrivateProjectPack() {
        val root = Files.createTempDirectory("gpt-project-pack-complete").toFile()
        val result = ConversationProjectPackBuilder(root).build(
            inventory = inventory((0..50).toList()),
            openSource = ::sourceFor,
            createdAtEpochMs = 42L
        )

        assertEquals("PACK_COMPLETE_51_OF_51", result.state)
        assertEquals(51, result.sources.size)
        assertTrue(result.coverage.complete)
        assertTrue(result.shards.isNotEmpty())
        assertTrue(result.manifestFile.isFile)
        assertTrue(result.receiptFile.isFile)
        val manifest = result.manifestFile.readText()
        assertTrue(manifest.contains("PRIVATE_DEFAULT_DENY"))
        assertTrue(manifest.contains("NOVOexport/01_SISTEMA_CORPUS_CUSTODIA"))
        assertFalse(manifest.contains("content://private.provider"))
        assertFalse(manifest.contains("private-document-"))
        assertTrue(result.shards.all { File(result.generationDir, it.filename).isFile })
        root.deleteRecursively()
    }

    @Test
    fun missing018BuildsPartialPackWithTypedGap() {
        val root = Files.createTempDirectory("gpt-project-pack-partial").toFile()
        val indices = (0..50).filterNot { it == 18 }
        val result = ConversationProjectPackBuilder(root).build(
            inventory = inventory(indices),
            openSource = ::sourceFor,
            createdAtEpochMs = 43L
        )

        assertEquals("PACK_PARTIAL_TYPED_GAP", result.state)
        assertEquals(50, result.sources.size)
        assertEquals(listOf(18), result.coverage.missingIndices)
        assertTrue(result.manifestFile.readText().contains("CONVERSATIONS_MISSING_018"))
        root.deleteRecursively()
    }

    @Test
    fun duplicateCanonicalIndexIsRejected() {
        val root = Files.createTempDirectory("gpt-project-pack-duplicate").toFile()
        val base = inventory(listOf(0))
        val duplicate = base.allFiles.first().copy(
            uri = "content://private.provider/document/duplicate",
            documentId = "private-document-duplicate"
        )
        val doubled = base.copy(
            visitedDocuments = 2,
            candidateFiles = base.candidateFiles + duplicate,
            allFiles = base.allFiles + duplicate
        )

        assertFalse(runCatching {
            ConversationProjectPackBuilder(root).build(
                inventory = doubled,
                openSource = ::sourceFor,
                createdAtEpochMs = 44L
            )
        }.isSuccess)
        root.deleteRecursively()
    }
}
