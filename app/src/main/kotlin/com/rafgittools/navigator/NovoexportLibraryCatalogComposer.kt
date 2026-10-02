package com.rafgittools.navigator

import com.rafgittools.library.LibraryAccessClass
import com.rafgittools.library.LibraryCatalogBundle
import com.rafgittools.library.LibraryCatalogGate
import com.rafgittools.library.LibraryEvidenceState
import com.rafgittools.library.LibraryItemRecord
import com.rafgittools.library.LibrarySourceBinding
import com.rafgittools.library.LibraryTreeNodeKind
import com.rafgittools.library.LibraryTreeNodeRecord
import java.net.URI
import java.security.MessageDigest

/**
 * Makes a bibliographic-safe, metadata-only catalog projection of a complete
 * SAF tree inventory. Source locators and provider document IDs never enter
 * the exported bundle; path topology uses hashed node IDs and parent links.
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
            displayLabel = "Selected SAF source tree (metadata only)",
            readOnly = true,
            accessClass = LibraryAccessClass.TOKEN_VAZIO,
            evidenceState = LibraryEvidenceState.SOURCE_OBSERVED,
            claimAllowed = false
        )

        val rootNodeId = "NODE-ROOT-" + locatorSha.take(24)
        val directories = inventory.allDirectories
            .filter { it.documentId != inventory.rootDocumentId }
            .distinctBy { it.documentId }
        val directoryNodeIds = directories.associate { directory ->
            directory.documentId to "NODE-DIR-" +
                sourceRefSha(inventory.treeUri, directory.documentId).take(24)
        }
        val fileEntries = (inventory.allFiles.ifEmpty { inventory.candidateFiles })
            .distinctBy { it.documentId }
        if (inventory.rootDocumentId.isBlank()) {
            require(inventory.allDirectories.isEmpty() &&
                fileEntries.all { it.parentDocumentId == null }
            ) { "NOVOEXPORT_ROOT_ID_REQUIRED_FOR_TREE" }
        }

        fun parentNodeId(parentDocumentId: String?): String {
            if (parentDocumentId == null || inventory.rootDocumentId.isBlank() ||
                parentDocumentId == inventory.rootDocumentId
            ) {
                return rootNodeId
            }
            return directoryNodeIds[parentDocumentId]
                ?: error("NOVOEXPORT_TREE_PARENT_NOT_IN_INVENTORY")
        }

        val items = fileEntries.map { entry ->
            val sourceRefSha = sourceRefSha(inventory.treeUri, entry.documentId)
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

        val rootNode = LibraryTreeNodeRecord(
            nodeId = rootNodeId,
            sourceId = sourceId,
            parentNodeId = null,
            displayName = "Selected SAF root",
            kind = LibraryTreeNodeKind.DIRECTORY,
            sourceRefSha256 = locatorSha,
            evidenceState = LibraryEvidenceState.SOURCE_OBSERVED,
            claimAllowed = false
        )
        val directoryNodes = directories.map { directory ->
            val sourceRefSha = sourceRefSha(inventory.treeUri, directory.documentId)
            LibraryTreeNodeRecord(
                nodeId = directoryNodeIds.getValue(directory.documentId),
                sourceId = sourceId,
                parentNodeId = parentNodeId(directory.parentDocumentId),
                displayName = directory.name,
                kind = LibraryTreeNodeKind.DIRECTORY,
                sourceRefSha256 = sourceRefSha,
                evidenceState = LibraryEvidenceState.SOURCE_OBSERVED,
                claimAllowed = false
            )
        }
        val fileNodes = fileEntries.mapIndexed { index, entry ->
            val item = items[index]
            LibraryTreeNodeRecord(
                nodeId = item.itemId,
                sourceId = sourceId,
                parentNodeId = parentNodeId(entry.parentDocumentId),
                displayName = entry.name,
                kind = LibraryTreeNodeKind.FILE,
                sourceRefSha256 = item.sourceRefSha256,
                mediaType = item.mediaType,
                sizeBytes = item.sizeBytes,
                evidenceState = LibraryEvidenceState.SOURCE_OBSERVED,
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
                "SOURCE_TREE_METADATA_ONLY"
            ),
            createdAtEpochMs = createdAtEpochMs,
            claimAllowed = false,
            treeNodes = listOf(rootNode) + directoryNodes + fileNodes
        )
        val gate = LibraryCatalogGate.validate(bundle)
        require(gate.allowed) {
            "NOVOEXPORT_CATALOG_REJECTED:" + gate.errors.joinToString(",")
        }
        return bundle
    }

    private fun safeMimeType(value: String): String =
        if (Regex("^[A-Za-z0-9!#$&^_.+-]+/[A-Za-z0-9!#$&^_.+-]+$").matches(value)) {
            value
        } else {
            "application/octet-stream"
        }

    private fun sourceRefSha(treeUri: String, documentId: String): String {
        val identity = treeUri.length.toString() + ":" + treeUri +
            documentId.length.toString() + ":" + documentId
        return sha256(identity)
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
