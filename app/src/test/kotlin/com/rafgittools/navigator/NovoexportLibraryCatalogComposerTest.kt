package com.rafgittools.navigator

import com.rafgittools.library.LibraryAccessClass
import com.rafgittools.library.LibraryCatalogGate
import com.rafgittools.library.LibraryCatalogMaterializer
import com.rafgittools.library.LibraryEvidenceState
import com.rafgittools.library.LibraryTreeNodeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class NovoexportLibraryCatalogComposerTest {
    @Test
    fun composesAllFilesAndDirectoryTopologyWithoutSourceIdentifiers() {
        val tree = "content://example.provider/tree/PRIVATE_TREE_TOKEN"
        val rootId = "provider-root-private-id"
        val folderId = "provider-folder-private-id"
        val conversationId = "provider-conversation-private-id"
        val spreadsheetId = "provider-spreadsheet-private-id"
        val conversation = NovoexportSafInventory.Entry(
            uri = "content://example.provider/document/PRIVATE_CONVERSATION_URI",
            documentId = conversationId,
            name = "conversation-export.json",
            mimeType = "application/json",
            sizeBytes = 123L,
            relativePath = "conversation-export.json",
            parentDocumentId = rootId
        )
        val spreadsheet = NovoexportSafInventory.Entry(
            uri = "content://example.provider/document/PRIVATE_SPREADSHEET_URI",
            documentId = spreadsheetId,
            name = "notes.csv",
            mimeType = "text/csv",
            sizeBytes = 654L,
            relativePath = "Sessions/notes.csv",
            parentDocumentId = folderId
        )
        val inventory = NovoexportSafInventory.Result(
            treeUri = tree,
            visitedDocuments = 4,
            visitedDirectories = 2,
            candidateFiles = listOf(conversation),
            knownCandidateBytes = 123L,
            unknownSizeCandidateFiles = 0,
            rootDocumentId = rootId,
            allFiles = listOf(conversation, spreadsheet),
            allDirectories = listOf(
                NovoexportSafInventory.DirectoryEntry(
                    documentId = folderId,
                    parentDocumentId = rootId,
                    name = "Sessions",
                    relativePath = "Sessions"
                )
            ),
            knownTotalBytes = 777L,
            unknownSizeFiles = 0
        )

        val bundle = NovoexportLibraryCatalogComposer.compose(inventory, createdAtEpochMs = 42L)
        val outputDir = Files.createTempDirectory("novoexport-tree-catalog").toFile()
        val materialized = LibraryCatalogMaterializer.materialize(outputDir, bundle)
        val json = materialized.catalogFile.readText()
        val receipt = materialized.receiptFile.readText()
        val spreadsheetItem = bundle.items.single { it.displayName == "notes.csv" }
        val folderNode = bundle.treeNodes.single { it.displayName == "Sessions" }
        val fileNode = bundle.treeNodes.single { it.nodeId == spreadsheetItem.itemId }
        val source = bundle.sources.single()

        assertFalse(bundle.claimAllowed)
        assertEquals(2, bundle.items.size)
        assertEquals(4, bundle.treeNodes.size)
        assertEquals(LibraryTreeNodeKind.DIRECTORY, folderNode.kind)
        assertEquals(folderNode.nodeId, fileNode.parentNodeId)
        assertEquals(LibraryTreeNodeKind.FILE, fileNode.kind)
        assertEquals(LibraryAccessClass.TOKEN_VAZIO, source.accessClass)
        assertTrue(bundle.items.all { it.accessClass == LibraryAccessClass.TOKEN_VAZIO })
        assertTrue(bundle.items.all { it.contentSha256 == null })
        assertTrue(bundle.items.all { !it.claimAllowed })
        assertEquals(LibraryEvidenceState.SOURCE_OBSERVED, spreadsheetItem.evidenceState)
        assertTrue(spreadsheetItem.gapRefs.contains("BIBLIOGRAPHIC_PARENT_TOKEN_VAZIO"))
        assertTrue(bundle.works.isEmpty())
        assertTrue(bundle.relations.isEmpty())
        assertTrue(spreadsheetItem.sourceRefSha256.matches(Regex("^[0-9a-f]{64}$")))
        assertTrue(source.locatorSha256.matches(Regex("^[0-9a-f]{64}$")))
        assertFalse(json.contains(tree))
        assertFalse(json.contains(rootId))
        assertFalse(json.contains(folderId))
        assertFalse(json.contains(conversationId))
        assertFalse(json.contains(spreadsheetId))
        assertFalse(json.contains("PRIVATE_CONVERSATION_URI"))
        assertFalse(json.contains("PRIVATE_SPREADSHEET_URI"))
        assertTrue(json.contains("Sessions"))
        assertTrue(json.contains("notes.csv"))
        assertTrue(receipt.contains("\"tree_node_count\": 4"))
        assertEquals(json.toByteArray(Charsets.UTF_8).size.toLong(), materialized.bytes)
        assertTrue(LibraryCatalogGate.validate(bundle).allowed)
        outputDir.deleteRecursively()
    }

    @Test
    fun fallsBackToCandidateListForLegacyInventoryRecords() {
        val inventory = NovoexportSafInventory.Result(
            treeUri = "content://example.provider/tree/root",
            visitedDocuments = 1,
            visitedDirectories = 1,
            candidateFiles = listOf(
                NovoexportSafInventory.Entry(
                    uri = "content://example.provider/document/a",
                    documentId = "a",
                    name = "conversation-export.json",
                    mimeType = "application/json",
                    sizeBytes = 123L
                )
            ),
            knownCandidateBytes = 123L,
            unknownSizeCandidateFiles = 0
        )
        val bundle = NovoexportLibraryCatalogComposer.compose(inventory, createdAtEpochMs = 42L)
        assertEquals(1, bundle.items.size)
        assertEquals(2, bundle.treeNodes.size)
        assertTrue(LibraryCatalogGate.validate(bundle).allowed)
    }

    @Test
    fun excludesOwnMaterializedCatalogOutputsFromSourceProjection() {
        val tree = "content://example.provider/tree/root"
        val rootId = "root"
        val catalog = NovoexportSafInventory.Entry(
            uri = "content://example.provider/document/catalog",
            documentId = "catalog",
            name = "CAT-NOVO-4df16e6588727f6c0e12-1791352188644.catalog.json",
            mimeType = "application/json",
            sizeBytes = 4098L,
            parentDocumentId = rootId
        )
        val receipt = NovoexportSafInventory.Entry(
            uri = "content://example.provider/document/receipt",
            documentId = "receipt",
            name = "CAT-NOVO-4df16e6588727f6c0e12-1791352188644.receipt.json",
            mimeType = "application/json",
            sizeBytes = 613L,
            parentDocumentId = rootId
        )
        val source = NovoexportSafInventory.Entry(
            uri = "content://example.provider/document/source",
            documentId = "source",
            name = "notes.json",
            mimeType = "application/json",
            sizeBytes = 17L,
            parentDocumentId = rootId
        )
        val inventory = NovoexportSafInventory.Result(
            treeUri = tree,
            visitedDocuments = 3,
            visitedDirectories = 1,
            candidateFiles = emptyList(),
            knownCandidateBytes = 0L,
            unknownSizeCandidateFiles = 0,
            rootDocumentId = rootId,
            allFiles = listOf(catalog, receipt, source),
            knownTotalBytes = 4728L,
            unknownSizeFiles = 0
        )

        val bundle = NovoexportLibraryCatalogComposer.compose(inventory, createdAtEpochMs = 42L)

        assertEquals(listOf("notes.json"), bundle.items.map { it.displayName })
        assertEquals(2, bundle.treeNodes.size)
        assertFalse(bundle.treeNodes.any { it.displayName.startsWith("CAT-NOVO-") })
        assertTrue(bundle.gaps.contains("CONTENT_HASH_NOT_COMPUTED"))
        assertFalse(bundle.claimAllowed)
    }

    @Test
    fun unsupportedInventoryStateIsRejected() {
        val inventory = NovoexportSafInventory.Result(
            treeUri = "content://example.provider/tree/root",
            visitedDocuments = 0,
            visitedDirectories = 1,
            candidateFiles = emptyList(),
            knownCandidateBytes = 0L,
            unknownSizeCandidateFiles = 0,
            state = "INVENTORY_PARTIAL"
        )
        val rejected = runCatching {
            NovoexportLibraryCatalogComposer.compose(inventory, createdAtEpochMs = 42L)
        }
        assertTrue(rejected.isFailure)
    }
}
