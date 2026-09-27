package com.rafgittools.library

enum class LibraryAccessClass { PUBLIC, INTERNAL, PRIVATE, RESTRICTED, TOKEN_VAZIO }

enum class LibraryEvidenceState {
    SOURCE_OBSERVED,
    HASH_VERIFIED,
    CATALOGED,
    RELATION_VERIFIED,
    TOKEN_VAZIO,
    REJECTED,
    QUARANTINED
}

enum class LibraryRelationType {
    ALIAS_OF,
    PART_OF,
    DERIVED_FROM,
    VERSION_OF,
    COPY_OF,
    SUPERSEDES,
    REFERENCES,
    EVIDENCES,
    CONTRADICTS,
    ROUTES_TO,
    CO_OCCURS_WITH
}

enum class AuthorityKind {
    PERSON,
    ORGANIZATION,
    PROJECT,
    REPOSITORY,
    SOFTWARE,
    DATASET,
    TERM,
    STANDARD,
    TOKEN_VAZIO
}

data class LibrarySourceBinding(
    val sourceId: String,
    val sourceSurface: String,
    val sourceSlot: String,
    val providerAuthority: String,
    val locatorSha256: String,
    val displayLabel: String,
    val readOnly: Boolean = true,
    val accessClass: LibraryAccessClass,
    val evidenceState: LibraryEvidenceState,
    val claimAllowed: Boolean = false
)

data class LibraryAuthorityRecord(
    val authorityId: String,
    val kind: AuthorityKind,
    val preferredLabel: String,
    val aliases: List<String> = emptyList(),
    val sourceRefs: List<String> = emptyList(),
    val evidenceState: LibraryEvidenceState,
    val claimAllowed: Boolean = false
)

data class LibraryWorkRecord(
    val workId: String,
    val preferredTitle: String,
    val workType: String,
    val authorityRefs: List<String> = emptyList(),
    val subjectRefs: List<String> = emptyList(),
    val languageTags: List<String> = emptyList(),
    val evidenceRefs: List<String> = emptyList(),
    val gapRefs: List<String> = emptyList(),
    val claimAllowed: Boolean = false
)

data class LibraryEditionRecord(
    val editionId: String,
    val workId: String,
    val versionLabel: String,
    val publicationOrBuildDate: String? = null,
    val formatLabel: String,
    val predecessorEditionId: String? = null,
    val evidenceRefs: List<String> = emptyList(),
    val claimAllowed: Boolean = false
)

data class LibraryItemRecord(
    val itemId: String,
    val editionId: String,
    val sourceId: String,
    val sourceRefSha256: String,
    val displayName: String,
    val mediaType: String,
    val sizeBytes: Long?,
    val contentSha256: String?,
    val modifiedTime: String?,
    val collectionRefs: List<String> = emptyList(),
    val shelfRef: String? = null,
    val sectionRefs: List<String> = emptyList(),
    val sessionRefs: List<String> = emptyList(),
    val accessClass: LibraryAccessClass,
    val evidenceState: LibraryEvidenceState,
    val evidenceRefs: List<String> = emptyList(),
    val gapRefs: List<String> = emptyList(),
    val claimAllowed: Boolean = false
)

data class LibraryRelationRecord(
    val relationId: String,
    val sourceId: String,
    val relation: LibraryRelationType,
    val targetId: String,
    val evidenceRefs: List<String> = emptyList(),
    val evidenceState: LibraryEvidenceState,
    val claimAllowed: Boolean = false
)

data class LibraryCatalogBundle(
    val schemaVersion: String = "1.0.0",
    val catalogId: String,
    val sources: List<LibrarySourceBinding>,
    val authorities: List<LibraryAuthorityRecord>,
    val works: List<LibraryWorkRecord>,
    val editions: List<LibraryEditionRecord>,
    val items: List<LibraryItemRecord>,
    val relations: List<LibraryRelationRecord>,
    val gaps: List<String>,
    val createdAtEpochMs: Long,
    val claimAllowed: Boolean = false
)
