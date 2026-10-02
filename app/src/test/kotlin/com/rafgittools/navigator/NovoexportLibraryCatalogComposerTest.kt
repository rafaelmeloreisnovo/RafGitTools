package com.rafgittools.navigator

import com.google.gson.GsonBuilder
import com.rafgittools.library.LibraryAccessClass
import com.rafgittools.library.LibraryCatalogGate
import com.rafgittools.library.LibraryEvidenceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NovoexportLibraryCatalogComposerTest {
    @Test
    fun composesOnlyMetadataAndKeepsSourceIdentifiersOutOfBundle() {
        val tree = "content://example.provider/tree/PRIVATE_TREE_TOKEN"
        val documentId = "drive/private/document/PRIVATE_DOCUMENT_TOKEN"
        val inventory = NovoexportSafInventory.Result(
            treeUri = tree,
            visitedDocuments = 2,
            visitedDirectories = 1,
            candidateFiles = listOf(
                NovoexportSafInventory.Entry(
                    uri = "content://example.provider/tree/PRIVATE_TREE_TOKEN/document/PRIVATE_DOCUMENT_TOKEN",
                    documentId = documentId,
                    name = "conversation-export.json",
                    mimeType = "application/json",
                    sizeBytes = 123L
                )
            ),
            knownCandidateBytes = 123L,
            unknownSizeCandidateFiles = 0
        )

        val bundle = NovoexportLibraryCatalogComposer.compose(inventory, createdAtEpochMs = 42L)
        val json = GsonBuilder().create().toJson(bundle)
        val item = bundle.items.single()
        val source = bundle.sources.single()

        assertFalse(bundle.claimAllowed)
        assertFalse(item.claimAllowed)
        assertEquals(LibraryAccessClass.TOKEN_VAZIO, source.accessClass)
        assertEquals(LibraryAccessClass.TOKEN_VAZIO, item.accessClass)
        assertEquals(LibraryEvidenceState.SOURCE_OBSERVED, item.evidenceState)
        assertNull(item.contentSha256)
        assertTrue(item.gapRefs.contains("BIBLIOGRAPHIC_PARENT_TOKEN_VAZIO"))
        assertTrue(bundle.works.isEmpty())
        assertTrue(bundle.relations.isEmpty())
        assertTrue(item.sourceRefSha256.matches(Regex("^[0-9a-f]{64}$")))
        assertTrue(source.locatorSha256.matches(Regex("^[0-9a-f]{64}$")))
        assertFalse(json.contains(tree))
        assertFalse(json.contains(documentId))
        assertFalse(json.contains("PRIVATE_TREE_TOKEN"))
        assertFalse(json.contains("PRIVATE_DOCUMENT_TOKEN"))
        assertTrue(LibraryCatalogGate.validate(bundle).allowed)
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
