package com.rafgittools.library

object LibraryCatalogGate {
    private val stableId = Regex("^[A-Za-z0-9._:-]{4,160}$")
    private val sha256 = Regex("^[0-9a-f]{64}$")
    private val languageTag = Regex("^[A-Za-z0-9-]{2,35}$")
    private val credentialMarkers = listOf(
        "ghp_",
        "github_pat_",
        "authorization: bearer ",
        "-----begin private key-----",
        "-----begin openssh private key-----",
        "password=",
        "senha=",
        "seed phrase",
        "mnemonic="
    )

    data class Result(val allowed: Boolean, val errors: List<String>)

    fun validate(bundle: LibraryCatalogBundle): Result {
        val errors = mutableListOf<String>()

        if (bundle.schemaVersion != "1.0.0") errors += "SCHEMA_VERSION"
        if (!stableId.matches(bundle.catalogId)) errors += "CATALOG_ID"
        if (bundle.claimAllowed) errors += "CLAIM_ALLOWED_BLOCKED"

        uniqueIds("source", bundle.sources.map { it.sourceId }, errors)
        uniqueIds("authority", bundle.authorities.map { it.authorityId }, errors)
        uniqueIds("work", bundle.works.map { it.workId }, errors)
        uniqueIds("expression", bundle.expressions.map { it.expressionId }, errors)
        uniqueIds("manifestation", bundle.manifestations.map { it.manifestationId }, errors)
        uniqueIds("edition", bundle.editions.map { it.editionId }, errors)
        uniqueIds("item", bundle.items.map { it.itemId }, errors)
        uniqueIds("preservation_event", bundle.preservationEvents.map { it.eventId }, errors)
        uniqueIds("rights", bundle.rights.map { it.rightsId }, errors)
        uniqueIds("relation", bundle.relations.map { it.relationId }, errors)

        val sourceIds = bundle.sources.map { it.sourceId }.toSet()
        val authorityIds = bundle.authorities.map { it.authorityId }.toSet()
        val workIds = bundle.works.map { it.workId }.toSet()
        val expressionIds = bundle.expressions.map { it.expressionId }.toSet()
        val manifestationIds = bundle.manifestations.map { it.manifestationId }.toSet()
        val editionIds = bundle.editions.map { it.editionId }.toSet()
        val itemIds = bundle.items.map { it.itemId }.toSet()
        val preservationEventIds = bundle.preservationEvents.map { it.eventId }.toSet()
        val rightsIds = bundle.rights.map { it.rightsId }.toSet()
        val allObjectIds = sourceIds + authorityIds + workIds + expressionIds +
            manifestationIds + editionIds + itemIds + preservationEventIds + rightsIds

        bundle.sources.forEach { source ->
            if (!stableId.matches(source.sourceId)) errors += "SOURCE_ID:" + source.sourceId
            if (!sha256.matches(source.locatorSha256)) errors += "SOURCE_LOCATOR_HASH:" + source.sourceId
            if (!source.readOnly) errors += "SOURCE_NOT_READ_ONLY:" + source.sourceId
            if (source.claimAllowed) errors += "SOURCE_CLAIM_ALLOWED:" + source.sourceId
            scanCredentials(source.displayLabel, "SOURCE_LABEL:" + source.sourceId, errors)
            scanCredentials(source.providerAuthority, "SOURCE_PROVIDER:" + source.sourceId, errors)
        }

        bundle.authorities.forEach { authority ->
            if (!stableId.matches(authority.authorityId)) errors += "AUTHORITY_ID:" + authority.authorityId
            if (authority.preferredLabel.isBlank()) errors += "AUTHORITY_LABEL:" + authority.authorityId
            if (authority.claimAllowed) errors += "AUTHORITY_CLAIM_ALLOWED:" + authority.authorityId
            scanCredentials(authority.preferredLabel, "AUTHORITY_LABEL:" + authority.authorityId, errors)
            authority.aliases.forEach { alias ->
                scanCredentials(alias, "AUTHORITY_ALIAS:" + authority.authorityId, errors)
            }
        }

        bundle.works.forEach { work ->
            if (!stableId.matches(work.workId)) errors += "WORK_ID:" + work.workId
            if (work.preferredTitle.isBlank()) errors += "WORK_TITLE:" + work.workId
            if (work.claimAllowed) errors += "WORK_CLAIM_ALLOWED:" + work.workId
            work.authorityRefs.filterNot(authorityIds::contains)
                .forEach { errors += "WORK_UNKNOWN_AUTHORITY:" + work.workId + ":" + it }
            work.languageTags.filterNot(languageTag::matches)
                .forEach { errors += "WORK_LANGUAGE_TAG:" + work.workId + ":" + it }
            scanCredentials(work.preferredTitle, "WORK_TITLE:" + work.workId, errors)
        }

        bundle.expressions.forEach { expression ->
            if (!stableId.matches(expression.expressionId)) {
                errors += "EXPRESSION_ID:" + expression.expressionId
            }
            if (expression.workId !in workIds) {
                errors += "EXPRESSION_UNKNOWN_WORK:" + expression.expressionId
            }
            expression.languageTags.filterNot(languageTag::matches)
                .forEach { errors += "EXPRESSION_LANGUAGE_TAG:" + expression.expressionId + ":" + it }
            if (expression.claimAllowed) {
                errors += "EXPRESSION_CLAIM_ALLOWED:" + expression.expressionId
            }
        }

        bundle.manifestations.forEach { manifestation ->
            if (!stableId.matches(manifestation.manifestationId)) {
                errors += "MANIFESTATION_ID:" + manifestation.manifestationId
            }
            if (manifestation.expressionRefs.isEmpty()) {
                errors += "MANIFESTATION_EXPRESSION_REQUIRED:" + manifestation.manifestationId
            }
            manifestation.expressionRefs.filterNot(expressionIds::contains)
                .forEach {
                    errors += "MANIFESTATION_UNKNOWN_EXPRESSION:" +
                        manifestation.manifestationId + ":" + it
                }
            if (manifestation.predecessorManifestationId != null &&
                manifestation.predecessorManifestationId !in manifestationIds
            ) {
                errors += "MANIFESTATION_UNKNOWN_PREDECESSOR:" + manifestation.manifestationId
            }
            if (manifestation.claimAllowed) {
                errors += "MANIFESTATION_CLAIM_ALLOWED:" + manifestation.manifestationId
            }
        }

        bundle.editions.forEach { edition ->
            if (!stableId.matches(edition.editionId)) errors += "EDITION_ID:" + edition.editionId
            if (edition.workId !in workIds) errors += "EDITION_UNKNOWN_WORK:" + edition.editionId
            if (edition.predecessorEditionId != null && edition.predecessorEditionId !in editionIds) {
                errors += "EDITION_UNKNOWN_PREDECESSOR:" + edition.editionId
            }
            if (edition.claimAllowed) errors += "EDITION_CLAIM_ALLOWED:" + edition.editionId
        }

        bundle.items.forEach { item ->
            if (!stableId.matches(item.itemId)) errors += "ITEM_ID:" + item.itemId
            if (item.editionId == null && item.manifestationId == null) {
                if ("BIBLIOGRAPHIC_PARENT_TOKEN_VAZIO" !in item.gapRefs) {
                    errors += "ITEM_BIBLIOGRAPHIC_PARENT_REQUIRED:" + item.itemId
                }
                if (item.claimAllowed) {
                    errors += "ITEM_UNLINKED_CLAIM_ALLOWED:" + item.itemId
                }
            }
            if (item.editionId != null && item.editionId !in editionIds) {
                errors += "ITEM_UNKNOWN_EDITION:" + item.itemId
            }
            if (item.manifestationId != null && item.manifestationId !in manifestationIds) {
                errors += "ITEM_UNKNOWN_MANIFESTATION:" + item.itemId
            }
            if (item.sourceId !in sourceIds) errors += "ITEM_UNKNOWN_SOURCE:" + item.itemId
            if (!sha256.matches(item.sourceRefSha256)) errors += "ITEM_SOURCE_REF_HASH:" + item.itemId
            if (item.contentSha256 != null && !sha256.matches(item.contentSha256)) {
                errors += "ITEM_CONTENT_HASH:" + item.itemId
            }
            if (item.sizeBytes != null && item.sizeBytes < 0L) errors += "ITEM_SIZE:" + item.itemId
            if (item.claimAllowed) errors += "ITEM_CLAIM_ALLOWED:" + item.itemId
            scanCredentials(item.displayName, "ITEM_DISPLAY_NAME:" + item.itemId, errors)
        }

        bundle.preservationEvents.forEach { event ->
            if (!stableId.matches(event.eventId)) errors += "PRESERVATION_EVENT_ID:" + event.eventId
            if (event.objectRef !in allObjectIds) {
                errors += "PRESERVATION_EVENT_UNKNOWN_OBJECT:" + event.eventId
            }
            event.agentRefs.filterNot(authorityIds::contains).forEach {
                errors += "PRESERVATION_EVENT_UNKNOWN_AGENT:" + event.eventId + ":" + it
            }
            if (event.eventType.isBlank()) errors += "PRESERVATION_EVENT_TYPE:" + event.eventId
            if (event.occurredAt.isBlank()) errors += "PRESERVATION_EVENT_TIME:" + event.eventId
            if (event.outcome.isBlank()) errors += "PRESERVATION_EVENT_OUTCOME:" + event.eventId
            if (event.claimAllowed) errors += "PRESERVATION_EVENT_CLAIM_ALLOWED:" + event.eventId
        }

        bundle.rights.forEach { rights ->
            if (!stableId.matches(rights.rightsId)) errors += "RIGHTS_ID:" + rights.rightsId
            if (rights.objectRef !in allObjectIds) errors += "RIGHTS_UNKNOWN_OBJECT:" + rights.rightsId
            if (rights.basis.isBlank()) errors += "RIGHTS_BASIS:" + rights.rightsId
            if (rights.accessStatement.isBlank()) errors += "RIGHTS_ACCESS:" + rights.rightsId
            if (rights.retentionRule.isBlank()) errors += "RIGHTS_RETENTION:" + rights.rightsId
            if (rights.claimAllowed) errors += "RIGHTS_CLAIM_ALLOWED:" + rights.rightsId
        }

        bundle.relations.forEach { relation ->
            if (!stableId.matches(relation.relationId)) errors += "RELATION_ID:" + relation.relationId
            if (relation.sourceId !in allObjectIds) errors += "RELATION_UNKNOWN_SOURCE:" + relation.relationId
            if (relation.targetId !in allObjectIds) errors += "RELATION_UNKNOWN_TARGET:" + relation.relationId
            if (relation.claimAllowed) errors += "RELATION_CLAIM_ALLOWED:" + relation.relationId
            if (relation.relation == LibraryRelationType.COPY_OF &&
                relation.evidenceState != LibraryEvidenceState.HASH_VERIFIED
            ) {
                errors += "COPY_OF_REQUIRES_HASH_VERIFIED:" + relation.relationId
            }
        }

        bundle.gaps.forEach { scanCredentials(it, "GAP", errors) }

        return Result(
            allowed = errors.isEmpty(),
            errors = errors.distinct().sorted()
        )
    }

    private fun uniqueIds(kind: String, ids: List<String>, errors: MutableList<String>) {
        ids.groupingBy { it }.eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { errors += "DUPLICATE_" + kind.uppercase() + "_ID:" + it }
    }

    private fun scanCredentials(value: String, field: String, errors: MutableList<String>) {
        val lower = value.lowercase()
        if (credentialMarkers.any(lower::contains)) {
            errors += "CREDENTIAL_MARKER:" + field
        }
    }
}
