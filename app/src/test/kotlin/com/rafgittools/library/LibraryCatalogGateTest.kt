package com.rafgittools.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryCatalogGateTest {
    private fun bundle(
        itemHash: String? = null,
        relationType: LibraryRelationType = LibraryRelationType.PART_OF,
        relationEvidence: LibraryEvidenceState = LibraryEvidenceState.CATALOGED
    ): LibraryCatalogBundle {
        val source = LibrarySourceBinding(
            sourceId = "SRC-DRIVE-A",
            sourceSurface = "ANDROID_SAF_TREE",
            sourceSlot = "DRIVE_A",
            providerAuthority = "com.google.android.apps.docs.storage",
            locatorSha256 = "0".repeat(64),
            displayLabel = "Primary Drive library root",
            accessClass = LibraryAccessClass.PRIVATE,
            evidenceState = LibraryEvidenceState.SOURCE_OBSERVED
        )
        val authority = LibraryAuthorityRecord(
            authorityId = "AUTH-PROJECT-001",
            kind = AuthorityKind.PROJECT,
            preferredLabel = "RAFAELIA",
            evidenceState = LibraryEvidenceState.CATALOGED
        )
        val work = LibraryWorkRecord(
            workId = "WORK-001",
            preferredTitle = "Synthetic Work",
            workType = "DOCUMENT",
            authorityRefs = listOf(authority.authorityId),
            languageTags = listOf("pt-BR")
        )
        val expression = LibraryExpressionRecord(
            expressionId = "EXP-001",
            workId = work.workId,
            expressionType = "TEXT",
            languageTags = listOf("pt-BR")
        )
        val manifestation = LibraryManifestationRecord(
            manifestationId = "MAN-001",
            expressionRefs = listOf(expression.expressionId),
            formatLabel = "DIGITAL_FILE",
            mediaType = "text/plain",
            versionLabel = "V1"
        )
        val edition = LibraryEditionRecord(
            editionId = "ED-001",
            workId = work.workId,
            versionLabel = "V1",
            formatLabel = "TEXT"
        )
        val item = LibraryItemRecord(
            itemId = "ITEM-001",
            editionId = edition.editionId,
            manifestationId = manifestation.manifestationId,
            sourceId = source.sourceId,
            sourceRefSha256 = "1".repeat(64),
            displayName = "synthetic.txt",
            mediaType = "text/plain",
            sizeBytes = 10,
            contentSha256 = itemHash,
            modifiedTime = null,
            accessClass = LibraryAccessClass.PRIVATE,
            evidenceState = if (itemHash == null) LibraryEvidenceState.CATALOGED
                else LibraryEvidenceState.HASH_VERIFIED
        )
        val relation = LibraryRelationRecord(
            relationId = "REL-001",
            sourceId = item.itemId,
            relation = relationType,
            targetId = work.workId,
            evidenceState = relationEvidence
        )
        return LibraryCatalogBundle(
            catalogId = "CATALOG-001",
            sources = listOf(source),
            authorities = listOf(authority),
            works = listOf(work),
            expressions = listOf(expression),
            manifestations = listOf(manifestation),
            editions = listOf(edition),
            items = listOf(item),
            relations = listOf(relation),
            gaps = listOf("SECOND_DRIVE_BINDING_TOKEN_VAZIO"),
            createdAtEpochMs = 1L
        )
    }

    @Test
    fun validCatalogPasses() {
        val result = LibraryCatalogGate.validate(bundle())
        assertTrue(result.errors.toString(), result.allowed)
    }

    @Test
    fun copyOfRequiresHashVerifiedEvidence() {
        val result = LibraryCatalogGate.validate(
            bundle(
                relationType = LibraryRelationType.COPY_OF,
                relationEvidence = LibraryEvidenceState.CATALOGED
            )
        )
        assertFalse(result.allowed)
        assertTrue(result.errors.any { it.startsWith("COPY_OF_REQUIRES_HASH_VERIFIED") })
    }

    @Test
    fun copyOfCanPassWhenEvidenceIsHashVerified() {
        val result = LibraryCatalogGate.validate(
            bundle(
                itemHash = "2".repeat(64),
                relationType = LibraryRelationType.COPY_OF,
                relationEvidence = LibraryEvidenceState.HASH_VERIFIED
            )
        )
        assertTrue(result.errors.toString(), result.allowed)
    }

    @Test
    fun credentialMarkersFailClosed() {
        val base = bundle()
        val poisoned = base.copy(
            sources = listOf(
                base.sources.first().copy(displayLabel = "authorization: bearer hidden")
            )
        )
        val result = LibraryCatalogGate.validate(poisoned)
        assertFalse(result.allowed)
        assertTrue(result.errors.any { it.startsWith("CREDENTIAL_MARKER") })
    }
    @Test
    fun unlinkedInventoryItemNeedsExplicitBibliographicGap() {
        val base = bundle()
        val unlinked = base.items.single().copy(editionId = null, manifestationId = null)
        val missingGap = base.copy(items = listOf(unlinked), relations = emptyList())
        val rejected = LibraryCatalogGate.validate(missingGap)
        assertFalse(rejected.allowed)
        assertTrue(rejected.errors.any { it.startsWith("ITEM_BIBLIOGRAPHIC_PARENT_REQUIRED") })

        val marked = missingGap.copy(
            items = listOf(unlinked.copy(gapRefs = listOf("BIBLIOGRAPHIC_PARENT_TOKEN_VAZIO")))
        )
        val accepted = LibraryCatalogGate.validate(marked)
        assertTrue(accepted.errors.toString(), accepted.allowed)
    }

    @Test
    fun treeNodesMustPreserveDirectoryParentAndFileItemIdentity() {
        val base = bundle()
        val source = base.sources.single()
        val item = base.items.single()
        val root = LibraryTreeNodeRecord(
            nodeId = "NODE-ROOT-001",
            sourceId = source.sourceId,
            parentNodeId = null,
            displayName = "Selected SAF root",
            kind = LibraryTreeNodeKind.DIRECTORY,
            sourceRefSha256 = source.locatorSha256
        )
        val directory = LibraryTreeNodeRecord(
            nodeId = "NODE-DIR-001",
            sourceId = source.sourceId,
            parentNodeId = root.nodeId,
            displayName = "folder",
            kind = LibraryTreeNodeKind.DIRECTORY,
            sourceRefSha256 = "2".repeat(64)
        )
        val file = LibraryTreeNodeRecord(
            nodeId = item.itemId,
            sourceId = item.sourceId,
            parentNodeId = directory.nodeId,
            displayName = item.displayName,
            kind = LibraryTreeNodeKind.FILE,
            sourceRefSha256 = item.sourceRefSha256,
            mediaType = item.mediaType,
            sizeBytes = item.sizeBytes
        )
        val valid = base.copy(treeNodes = listOf(root, directory, file))
        val accepted = LibraryCatalogGate.validate(valid)
        assertTrue(accepted.errors.toString(), accepted.allowed)

        val disconnected = valid.copy(
            treeNodes = listOf(root, directory, file.copy(parentNodeId = "NODE-MISSING"))
        )
        val rejected = LibraryCatalogGate.validate(disconnected)
        assertFalse(rejected.allowed)
        assertTrue(rejected.errors.any { it.startsWith("TREE_NODE_UNKNOWN_PARENT") })
    }

}
