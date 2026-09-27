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
        val edition = LibraryEditionRecord(
            editionId = "ED-001",
            workId = work.workId,
            versionLabel = "V1",
            formatLabel = "TEXT"
        )
        val item = LibraryItemRecord(
            itemId = "ITEM-001",
            editionId = edition.editionId,
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
}
