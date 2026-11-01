package com.rafgittools.setupwizard

import com.google.gson.JsonParser
import com.rafgittools.navigator.NovoexportSafInventory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DriveMetadataIndexGateTest {
    private fun sample(path: String = "Private/conversations.json",
                       size: Long? = null,
                       documents: Int = 2): NovoexportSafInventory.Result {
        val file = NovoexportSafInventory.Entry(
            uri = "content://private-account-secret/doc-1",
            documentId = "PRIVATE_DOC_ID",
            name = "conversations.json",
            mimeType = "application/json",
            sizeBytes = size,
            relativePath = path,
            parentDocumentId = "PRIVATE_PARENT_ID",
            semanticTokens = listOf("Private", "conversations", "json"),
            semanticPassMask = 0x1f,
            semanticKindFlags = 2,
            pathComponents = 2
        )
        val folder = NovoexportSafInventory.DirectoryEntry(
            documentId = "PRIVATE_PARENT_ID",
            parentDocumentId = null,
            name = "Private",
            relativePath = "Private",
            semanticTokens = listOf("Private"),
            semanticPassMask = 0x1f,
            semanticKindFlags = 1,
            pathComponents = 1
        )
        return NovoexportSafInventory.Result(
            treeUri = "content://private-account-secret/root",
            visitedDocuments = documents,
            visitedDirectories = 1,
            candidateFiles = listOf(file),
            knownCandidateBytes = size ?: 0L,
            unknownSizeCandidateFiles = if (size == null) 1 else 0,
            allFiles = listOf(file),
            allDirectories = listOf(folder),
            knownTotalBytes = size ?: 0L,
            unknownSizeFiles = if (size == null) 1 else 0
        )
    }

    @Test fun selectedTreeMetadataOnlyAndParentEdges() {
        val json = String(DriveMetadataIndexGate.build(sample()), Charsets.UTF_8)
        val root = JsonParser.parseString(json).asJsonObject
        assertEquals("USER_SELECTED_SAF_TREE_ONLY", root.get("source_scope").asString)
        assertFalse(root.get("claim_allowed").asBoolean)
        assertFalse(root.get("content_indexed").asBoolean)
        assertFalse(root.get("source_bytes_read").asBoolean)
        assertEquals("rafgittools.drive-saf-metadata-index.v2",
            root.get("schema").asString)
        assertEquals("conversations",
            root.get("semantic_token_paths").asJsonArray[1]
                .asJsonObject.get("tokens").asJsonArray[1].asString)
        assertEquals(0x1f,
            root.get("semantic_token_paths").asJsonArray[1]
                .asJsonObject.get("semantic_pass_mask").asInt)
        assertTrue(root.get("files").asJsonArray[0].asJsonObject.get("size_bytes").isJsonNull)
        assertEquals("Private",
            root.get("relations").asJsonArray[1].asJsonObject.get("from").asString)
        assertEquals("Private/conversations.json",
            root.get("relations").asJsonArray[1].asJsonObject.get("to").asString)
        assertFalse(json.contains("PRIVATE_DOC_ID"))
        assertFalse(json.contains("PRIVATE_PARENT_ID"))
        assertFalse(json.contains("private-account-secret"))
        assertFalse(json.contains("content://"))
        assertEquals("TOKEN_VAZIO_NOT_ATTESTED", root.get("global_drive_coverage").asString)
    }

    @Test fun deterministicBytesAndHash() {
        val a = DriveMetadataIndexGate.build(sample(size = 138L))
        val b = DriveMetadataIndexGate.build(sample(size = 138L))
        assertTrue(a.contentEquals(b))
        assertEquals(64, DriveMetadataIndexGate.digest(a).length)
        assertEquals(DriveMetadataIndexGate.digest(a), DriveMetadataIndexGate.digest(b))
    }

    @Test fun refusesPathTraversal() {
        reject { DriveMetadataIndexGate.build(sample("../conversations.json")) }
        reject { DriveMetadataIndexGate.build(sample("Private/./conversations.json")) }
        reject { DriveMetadataIndexGate.build(sample("Private//conversations.json")) }
        reject { DriveMetadataIndexGate.build(sample("Private/\nsecret")) }
    }

    @Test fun boundedCompleteInventoryNotClaimedFromPartial() {
        reject { DriveMetadataIndexGate.build(sample(documents = 5001)) }
        reject { DriveMetadataIndexGate.build(sample(size = -1L)) }
    }

    private inline fun reject(operation: () -> Unit) {
        try {
            operation()
            fail("Malformed index accepted")
        } catch (_: IllegalArgumentException) {
            // Fail closed.
        }
    }
}
