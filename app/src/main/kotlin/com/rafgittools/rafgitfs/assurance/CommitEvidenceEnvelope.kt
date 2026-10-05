package com.rafgittools.rafgitfs.assurance

enum class EvidenceState {
    PASS,
    FAIL,
    TOKEN_VAZIO,
    NOT_RUN,
    NOT_APPLICABLE
}

enum class EvidenceKind {
    SOURCE_IDENTITY,
    PRIVATE_WORKSPACE,
    BASE_COMMIT,
    CONFLICT_BOUNDARY,
    PLAN_HASH,
    EXACT_APPROVAL,
    NO_DESTRUCTIVE_WRITE,
    EXECUTION_RECEIPT,
    EXACT_HEAD_CI,
    SERVER_ENFORCEMENT,
    TRUSTED_TIME,
    TRANSPARENCY_LOG,
    INDEPENDENT_REVIEW,
    PUBLICATION_ANCHOR
}

enum class EvidenceProducerKind {
    HUMAN,
    AI_AGENT,
    AUTOMATION,
    PROVIDER,
    EXTERNAL_REGISTRY
}

enum class EvidenceProfile {
    SOFTWARE,
    SCIENTIFIC_PUBLICATION
}

data class EvidenceProducer(
    val kind: EvidenceProducerKind,
    val id: String,
    val independenceDomain: String
)

data class EvidenceRecord(
    val kind: EvidenceKind,
    val state: EvidenceState,
    val sourceRef: String,
    val subjectDigest: String? = null,
    val headSha: String? = null,
    val observedAtUtc: String? = null,
    val proofRef: String? = null,
    val dateType: String? = null,
    val dateValue: String? = null,
    val producer: EvidenceProducer,
    val claimAllowed: Boolean = false
)

data class CommitEvidenceEnvelope(
    val schema: String = "RAFGITTOOLS_COMMIT_EVIDENCE_ENVELOPE_V1",
    val repositoryFullName: String,
    val refName: String,
    val baseCommitSha: String?,
    val planHash: String?,
    val headSha: String?,
    val artifactDigest: String?,
    val profile: EvidenceProfile = EvidenceProfile.SOFTWARE,
    val reproducibilitySeed: Long = 1748365262L,
    val seedProvenanceRef: String = "RAFAELIA_SEED_1748365262_a6a1f608-b889-4803-8f59-d57ce00dba1b",
    val records: List<EvidenceRecord>,
    val privatePayloadIncluded: Boolean = false,
    val claimAllowed: Boolean = false
)

data class EvidenceValidation(
    val valid: Boolean,
    val blockingCodes: List<String>,
    val tokenVazioCodes: List<String>
)

data class CommitPromotionDecision(
    val draftAllowed: Boolean,
    val readyAllowed: Boolean,
    val mergeAllowed: Boolean,
    val blockingCodes: List<String>,
    val tokenVazioCodes: List<String>
)

object CommitEvidenceEnvelopePolicy {
    private val sha40 = Regex("^[0-9a-f]{40}$")
    private val sha64 = Regex("^[0-9a-f]{64}$")

    private val draftRequired = setOf(
        EvidenceKind.SOURCE_IDENTITY,
        EvidenceKind.PRIVATE_WORKSPACE,
        EvidenceKind.BASE_COMMIT,
        EvidenceKind.CONFLICT_BOUNDARY,
        EvidenceKind.PLAN_HASH,
        EvidenceKind.EXACT_APPROVAL,
        EvidenceKind.NO_DESTRUCTIVE_WRITE
    )

    private val readyRequired = draftRequired + setOf(
        EvidenceKind.EXECUTION_RECEIPT,
        EvidenceKind.EXACT_HEAD_CI
    )

    private val mergeRequired = readyRequired + setOf(
        EvidenceKind.SERVER_ENFORCEMENT,
        EvidenceKind.TRUSTED_TIME,
        EvidenceKind.TRANSPARENCY_LOG,
        EvidenceKind.INDEPENDENT_REVIEW
    )

    fun validate(envelope: CommitEvidenceEnvelope): EvidenceValidation {
        val blocking = mutableListOf<String>()
        val tokenVazio = mutableListOf<String>()

        if (envelope.schema != "RAFGITTOOLS_COMMIT_EVIDENCE_ENVELOPE_V1") blocking += "ENV-SCHEMA-001"
        if (envelope.repositoryFullName.isBlank() || !envelope.repositoryFullName.contains('/')) blocking += "ENV-REPO-002"
        if (envelope.refName.isBlank()) blocking += "ENV-REF-003"

        when {
            envelope.baseCommitSha == null -> tokenVazio += "ENV-BASE-004"
            !sha40.matches(envelope.baseCommitSha) -> blocking += "ENV-BASE-004"
        }
        when {
            envelope.planHash == null -> tokenVazio += "ENV-PLAN-005"
            !sha64.matches(envelope.planHash) -> blocking += "ENV-PLAN-005"
        }
        when {
            envelope.headSha == null -> tokenVazio += "ENV-HEAD-006"
            !sha40.matches(envelope.headSha) -> blocking += "ENV-HEAD-006"
        }
        when {
            envelope.artifactDigest == null -> tokenVazio += "ENV-ARTIFACT-DIGEST-011"
            !sha64.matches(envelope.artifactDigest) -> blocking += "ENV-ARTIFACT-DIGEST-011"
        }

        if (envelope.privatePayloadIncluded) blocking += "ENV-PRIVACY-007"
        if (envelope.claimAllowed) blocking += "ENV-CLAIM-008"
        if (envelope.reproducibilitySeed != 1748365262L) blocking += "ENV-SEED-009"
        if (!envelope.seedProvenanceRef.startsWith("RAFAELIA_SEED_1748365262_")) blocking += "ENV-SEED-010"

        val byKind = envelope.records.groupBy { it.kind }
        for ((kind, records) in byKind) {
            if (records.size > 1 && kind != EvidenceKind.PUBLICATION_ANCHOR) {
                blocking += "ENV-DUP-${kind.name}"
            }
        }

        envelope.records.forEach { record ->
            if (record.claimAllowed) blocking += "ENV-RECORD-CLAIM-${record.kind.name}"
            if (record.sourceRef.isBlank()) blocking += "ENV-SOURCE-${record.kind.name}"
            if (record.producer.id.isBlank() || record.producer.independenceDomain.isBlank()) {
                blocking += "ENV-PRODUCER-${record.kind.name}"
            }
            if (record.kind == EvidenceKind.EXACT_APPROVAL && record.state == EvidenceState.PASS) {
                if (envelope.planHash == null || record.subjectDigest != envelope.planHash) {
                    blocking += "ENV-APPROVAL-PLAN-MISMATCH"
                }
            }
            if (record.kind == EvidenceKind.EXACT_HEAD_CI && record.state == EvidenceState.PASS) {
                if (envelope.headSha == null || record.headSha != envelope.headSha) blocking += "ENV-CI-HEAD-MISMATCH"
                if (record.proofRef.isNullOrBlank()) tokenVazio += "ENV-CI-PROOF"
            }
            if (record.kind == EvidenceKind.TRUSTED_TIME && record.state == EvidenceState.PASS) {
                if (record.subjectDigest.isNullOrBlank() || !sha64.matches(record.subjectDigest)) {
                    blocking += "ENV-TIME-DIGEST"
                } else if (envelope.artifactDigest == null || record.subjectDigest != envelope.artifactDigest) {
                    blocking += "ENV-TIME-SUBJECT-MISMATCH"
                }
                if (record.proofRef.isNullOrBlank() || record.observedAtUtc.isNullOrBlank()) {
                    tokenVazio += "ENV-TIME-PROOF"
                }
            }
            if (record.kind == EvidenceKind.TRANSPARENCY_LOG && record.state == EvidenceState.PASS) {
                if (record.subjectDigest.isNullOrBlank() || !sha64.matches(record.subjectDigest)) {
                    blocking += "ENV-LOG-DIGEST"
                } else if (envelope.artifactDigest == null || record.subjectDigest != envelope.artifactDigest) {
                    blocking += "ENV-LOG-SUBJECT-MISMATCH"
                }
                if (record.proofRef.isNullOrBlank()) tokenVazio += "ENV-LOG-PROOF"
            }
            if (record.kind == EvidenceKind.PUBLICATION_ANCHOR && record.state == EvidenceState.PASS) {
                if (record.dateType.isNullOrBlank() || record.dateValue.isNullOrBlank()) {
                    tokenVazio += "ENV-PUBLICATION-DATE-TYPING"
                }
            }
        }

        val time = envelope.records.singleOrNull { it.kind == EvidenceKind.TRUSTED_TIME && it.state == EvidenceState.PASS }
        val log = envelope.records.singleOrNull { it.kind == EvidenceKind.TRANSPARENCY_LOG && it.state == EvidenceState.PASS }
        if (time != null && log != null && time.producer.independenceDomain == log.producer.independenceDomain) {
            blocking += "ENV-CORRELATED-TIME-LOG"
        }
        if (time != null && log != null && time.subjectDigest != log.subjectDigest) {
            blocking += "ENV-ANCHOR-SUBJECT-DIVERGENCE"
        }

        val review = envelope.records.singleOrNull { it.kind == EvidenceKind.INDEPENDENT_REVIEW && it.state == EvidenceState.PASS }
        val source = envelope.records.singleOrNull { it.kind == EvidenceKind.SOURCE_IDENTITY }
        if (review != null && source != null && review.producer.id == source.producer.id) {
            blocking += "ENV-SELF-REVIEW"
        }

        if (envelope.profile == EvidenceProfile.SCIENTIFIC_PUBLICATION) {
            val publications = envelope.records.filter { it.kind == EvidenceKind.PUBLICATION_ANCHOR }
            if (publications.none { it.state == EvidenceState.PASS }) tokenVazio += "ENV-PUBLICATION-REQUIRED"
        }

        return EvidenceValidation(
            valid = blocking.isEmpty() && tokenVazio.isEmpty(),
            blockingCodes = blocking.distinct().sorted(),
            tokenVazioCodes = tokenVazio.distinct().sorted()
        )
    }

    fun decide(envelope: CommitEvidenceEnvelope): CommitPromotionDecision {
        val validation = validate(envelope)
        val blocking = validation.blockingCodes.toMutableList()
        val tokenVazio = validation.tokenVazioCodes.toMutableList()
        val records = envelope.records.associateBy { it.kind }

        fun requiredPass(kinds: Set<EvidenceKind>, stage: String): Boolean {
            var pass = true
            for (kind in kinds) {
                val record = records[kind]
                when (record?.state) {
                    EvidenceState.PASS -> Unit
                    EvidenceState.FAIL -> {
                        blocking += "$stage-${kind.name}-FAIL"
                        pass = false
                    }
                    EvidenceState.NOT_APPLICABLE -> {
                        blocking += "$stage-${kind.name}-N/A"
                        pass = false
                    }
                    EvidenceState.TOKEN_VAZIO, EvidenceState.NOT_RUN, null -> {
                        tokenVazio += "$stage-${kind.name}-UNRESOLVED"
                        pass = false
                    }
                }
            }
            return pass
        }

        val draftBaseIdentityKnown = envelope.baseCommitSha != null && envelope.planHash != null
        val draft = blocking.isEmpty() && draftBaseIdentityKnown && requiredPass(draftRequired, "DRAFT")

        val readyIdentityKnown = envelope.headSha != null
        val ready = draft && readyIdentityKnown && blocking.isEmpty() && requiredPass(readyRequired, "READY")

        val mergeArtifactKnown = envelope.artifactDigest != null
        var merge = ready && mergeArtifactKnown && blocking.isEmpty() && requiredPass(mergeRequired, "MERGE")

        if (envelope.profile == EvidenceProfile.SCIENTIFIC_PUBLICATION) {
            val publicationPass = envelope.records.any {
                it.kind == EvidenceKind.PUBLICATION_ANCHOR && it.state == EvidenceState.PASS &&
                    !it.dateType.isNullOrBlank() && !it.dateValue.isNullOrBlank()
            }
            if (!publicationPass) {
                tokenVazio += "MERGE-PUBLICATION-ANCHOR-UNRESOLVED"
                merge = false
            }
        }

        return CommitPromotionDecision(
            draftAllowed = draft,
            readyAllowed = ready,
            mergeAllowed = merge && blocking.isEmpty() && tokenVazio.isEmpty(),
            blockingCodes = blocking.distinct().sorted(),
            tokenVazioCodes = tokenVazio.distinct().sorted()
        )
    }
}

interface CommitEvidenceAdapter {
    val kind: EvidenceKind
    fun observe(envelope: CommitEvidenceEnvelope): EvidenceRecord
}

class UnboundEvidenceAdapter(
    override val kind: EvidenceKind,
    private val sourceRef: String,
    private val producer: EvidenceProducer
) : CommitEvidenceAdapter {
    override fun observe(envelope: CommitEvidenceEnvelope): EvidenceRecord = EvidenceRecord(
        kind = kind,
        state = EvidenceState.TOKEN_VAZIO,
        sourceRef = sourceRef,
        headSha = envelope.headSha,
        producer = producer,
        claimAllowed = false
    )
}
