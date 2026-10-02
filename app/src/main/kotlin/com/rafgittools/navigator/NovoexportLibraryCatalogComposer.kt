package com.rafgittools.navigator

import com.rafgittools.library.LibraryAccessClass
import com.rafgittools.library.LibraryCatalogBundle
import com.rafgittools.library.LibraryCatalogGate
import com.rafgittools.library.LibraryEvidenceState
import com.rafgittools.library.LibraryItemRecord
import com.rafgittools.library.LibrarySourceBinding
import java.net.URI
import java.security.MessageDigest

/**
 * Makes a bibliographic-safe, metadata-only catalog projection of a bounded
 * NOVOexport SAF inventory. Source locators and document IDs never enter the
 * exported bundle; they are represented by SHA-256 fingerprints.
 */
object NovoexportLibraryCatalogComposer {
    private const val BIBLIOGRAPHIC_GAP = "BIBLIOGRAPHIC_PARENT_TOKEN_VAZIO"

    fun compose(
        inventory: NovoexportSafInventory.Result,
        createdAtEpochMs: Long
    ): LibraryCatalogBundle {
        require(inventory.state == "INVENTORY_COMPLETE_METADATA_ONLY") {
            "NOVOEXPORT_INVENTORY_NOT_COMPLETE"
        }
        require(createdAtEpochMs >= 0L) { "CATALOG_TIMESTAMP_INVALID" }

        val locatorSha = sha256(inventory.treeUri)
        val sourceId = "SRC-NOVO-" + locatorSha.take(24)
        val providerAuthority = runCatching {
            URI(inventory.treeUri).rawAuthority
        }.getOrNull()?.takeIf { it.isNotBlank() && !it.contains('@') }
            ?: "TOKEN_VAZIO_PROVIDER_AUTHORITY"
        val source = LibrarySourceBinding(
            sourceId = sourceId,
            sourceSurface = "ANDROID_SAF_TREE",
            sourceSlot = "NOVOEXPORT_SELECTED_ROOT",
            providerAuthority = providerAuthority,
            locatorSha256 = locatorSha,
            displayLabel = "NOVOexport source tree (metadata only)",
            readOnly = true,
            accessClass = LibraryAccessClass.TOKEN_VAZIO,
            evidenceState = LibraryEvidenceState.SOURCE_OBSERVED,
            claimAllowed = false
        )

        val items = inventory.candidateFiles
            .distinctBy { it.documentId }
            .map { entry ->
                val identity = inventory.treeUri.length.toString() + ":" + inventory.treeUri +
                    entry.documentId.length.toString() + ":" + entry.documentId
                val sourceRefSha = sha256(identity)
                LibraryItemRecord(
                    itemId = "ITEM-NOVO-" + sourceRefSha.take(24),
                    editionId = null,
                    manifestationId = null,
                    sourceId = sourceId,
                    sourceRefSha256 = sourceRefSha,
                    displayName = entry.name,
                    mediaType = safeMimeType(entry.mimeType),
                    sizeBytes = entry.sizeBytes,
                    contentSha256 = null,
                    modifiedTime = null,
                    accessClass = LibraryAccessClass.TOKEN_VAZIO,
                    evidenceState = LibraryEvidenceState.SOURCE_OBSERVED,
                    evidenceRefs = emptyList(),
                    gapRefs = listOf(BIBLIOGRAPHIC_GAP, "CONTENT_HASH_NOT_COMPUTED"),
                    claimAllowed = false
                )
            }

        val bundle = LibraryCatalogBundle(
            catalogId = "CAT-NOVO-" + locatorSha.take(20) + "-" + createdAtEpochMs,
            sources = listOf(source),
            authorities = emptyList(),
            works = emptyList(),
            expressions = emptyList(),
            manifestations = emptyList(),
            editions = emptyList(),
            items = items,
            preservationEvents = emptyList(),
            rights = emptyList(),
            relations = emptyList(),
            gaps = listOf(
                "ACCESS_CLASS_TOKEN_VAZIO",
                "BIBLIOGRAPHIC_PARENT_NOT_ASSERTED",
                "CONTENT_HASH_NOT_COMPUTED",
                "SEMANTIC_EXTRACTION_NOT_RUN",
                "SOURCE_SCOPE_CONVERSATION_CODEX_JSON_NAME_FILTER"
            ),
            createdAtEpochMs = createdAtEpochMs,
            claimAllowed = false
        )
        val gate = LibraryCatalogGate.validate(bundle)
        require(gate.allowed) {
            "NOVOEXPORT_CATALOG_REJECTED:" + gate.errors.joinToString(",")
        }
        return bundle
    }

    private fun safeMimeType(value: String): String {
        return if (Regex("^[A-Za-z0-9!#$&^_.+-]+/[A-Za-z0-9!#$&^_.+-]+$").matches(value)) {
            value
        } else {
            "application/octet-stream"
        }
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
