package com.rafgittools.rafgitfs.assurance

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommitEvidenceEnvelopeTest {
    private val human = EvidenceProducer(EvidenceProducerKind.HUMAN, "human:rafael", "human-owner")
    private val ai = EvidenceProducer(EvidenceProducerKind.AI_AGENT, "ai:reviewer", "ai-review")
    private val provider = EvidenceProducer(EvidenceProducerKind.PROVIDER, "provider:github", "github")
    private val tsa = EvidenceProducer(EvidenceProducerKind.EXTERNAL_REGISTRY, "tsa:one", "tsa-one")
    private val log = EvidenceProducer(EvidenceProducerKind.EXTERNAL_REGISTRY, "log:one", "transparency-one")

    private fun record(
        kind: EvidenceKind,
        state: EvidenceState = EvidenceState.PASS,
        producer: EvidenceProducer = human,
        head: String? = null,
        digest: String? = null,
        proof: String? = "proof:${kind.name.lowercase()}",
        dateType: String? = null,
        dateValue: String? = null,
        risk: RiskSeverity? = null,
        urgency: UrgencyClass? = null,
        falsifier: String? = null,
        mitigation: String? = null
    ) = EvidenceRecord(
        kind = kind,
        state = state,
        sourceRef = "source:${kind.name.lowercase()}",
        subjectDigest = digest,
        headSha = head,
        observedAtUtc = "2026-10-05T18:00:00Z",
        proofRef = proof,
        dateType = dateType,
        dateValue = dateValue,
        producer = producer,
        riskSeverity = risk,
        urgency = urgency,
        falsifierRef = falsifier,
        mitigationRef = mitigation,
        claimAllowed = false
    )

    private fun safeguard(
        kind: EvidenceKind,
        risk: RiskSeverity,
        urgency: UrgencyClass,
        producer: EvidenceProducer = human,
        state: EvidenceState = EvidenceState.PASS
    ) = record(
        kind = kind,
        state = state,
        producer = producer,
        risk = risk,
        urgency = urgency,
        falsifier = "falsifier:${kind.name.lowercase()}",
        mitigation = "mitigation:${kind.name.lowercase()}"
    )

    private fun complete(profile: EvidenceProfile = EvidenceProfile.SOFTWARE): CommitEvidenceEnvelope {
        val head = "1".repeat(40)
        val artifactDigest = "2".repeat(64)
        val planHash = "a".repeat(64)
        return CommitEvidenceEnvelope(
            repositoryFullName = "owner/repo",
            refName = "refs/heads/rafgitfs/evidence",
            baseCommitSha = "0".repeat(40),
            planHash = planHash,
            headSha = head,
            artifactDigest = artifactDigest,
            profile = profile,
            records = listOf(
                safeguard(EvidenceKind.HUMAN_DIGNITY, RiskSeverity.CRITICAL, UrgencyClass.P0),
                safeguard(EvidenceKind.CHILD_SAFETY, RiskSeverity.CRITICAL, UrgencyClass.P0),
                safeguard(EvidenceKind.INCLUSION_NONDISCRIMINATION, RiskSeverity.CRITICAL, UrgencyClass.P0),
                safeguard(EvidenceKind.ACCESSIBILITY_INCLUSION, RiskSeverity.HIGH, UrgencyClass.P1),
                safeguard(EvidenceKind.SAFE_HEALTHY_WORK, RiskSeverity.CRITICAL, UrgencyClass.P0),
                record(EvidenceKind.SOURCE_IDENTITY, producer = human),
                record(EvidenceKind.PRIVATE_WORKSPACE, producer = human),
                record(EvidenceKind.BASE_COMMIT, producer = provider),
                record(EvidenceKind.CONFLICT_BOUNDARY, producer = human),
                record(EvidenceKind.PLAN_HASH, producer = human),
                record(EvidenceKind.EXACT_APPROVAL, producer = human, digest = planHash),
                record(EvidenceKind.NO_DESTRUCTIVE_WRITE, producer = human),
                record(EvidenceKind.EXECUTION_RECEIPT, producer = provider),
                record(EvidenceKind.EXACT_HEAD_CI, producer = provider, head = head),
                record(EvidenceKind.SERVER_ENFORCEMENT, producer = provider),
                record(EvidenceKind.TRUSTED_TIME, producer = tsa, digest = artifactDigest),
                record(EvidenceKind.TRANSPARENCY_LOG, producer = log, digest = artifactDigest),
                record(EvidenceKind.INDEPENDENT_REVIEW, producer = ai),
                record(
                    EvidenceKind.PUBLICATION_ANCHOR,
                    state = if (profile == EvidenceProfile.SCIENTIFIC_PUBLICATION) EvidenceState.PASS else EvidenceState.NOT_APPLICABLE,
                    producer = EvidenceProducer(EvidenceProducerKind.EXTERNAL_REGISTRY, "doi:registry", "doi-registry"),
                    dateType = if (profile == EvidenceProfile.SCIENTIFIC_PUBLICATION) "registered" else null,
                    dateValue = if (profile == EvidenceProfile.SCIENTIFIC_PUBLICATION) "2026-10-05" else null
                )
            )
        )
    }

    @Test
    fun `complete independent evidence allows merge`() {
        val decision = CommitEvidenceEnvelopePolicy.decide(complete())
        assertTrue(decision.draftAllowed)
        assertTrue(decision.readyAllowed)
        assertTrue(decision.mergeAllowed)
    }

    @Test
    fun `missing human dignity blocks draft ready and merge`() {
        val source = complete()
        val mutated = source.copy(records = source.records.filterNot { it.kind == EvidenceKind.HUMAN_DIGNITY })
        val decision = CommitEvidenceEnvelopePolicy.decide(mutated)
        assertFalse(decision.draftAllowed)
        assertFalse(decision.readyAllowed)
        assertFalse(decision.mergeAllowed)
        assertTrue(decision.tokenVazioCodes.contains("DRAFT-HUMAN_DIGNITY-UNRESOLVED"))
    }

    @Test
    fun `child safety fail is non compensatory`() {
        val source = complete()
        val mutated = source.copy(records = source.records.map {
            if (it.kind == EvidenceKind.CHILD_SAFETY) it.copy(state = EvidenceState.FAIL) else it
        })
        val decision = CommitEvidenceEnvelopePolicy.decide(mutated)
        assertFalse(decision.draftAllowed)
        assertFalse(decision.mergeAllowed)
        assertTrue(decision.blockingCodes.contains("DRAFT-CHILD_SAFETY-FAIL"))
    }

    @Test
    fun `human safeguard cannot bypass with not applicable`() {
        val source = complete()
        val mutated = source.copy(records = source.records.map {
            if (it.kind == EvidenceKind.INCLUSION_NONDISCRIMINATION) it.copy(state = EvidenceState.NOT_APPLICABLE) else it
        })
        val decision = CommitEvidenceEnvelopePolicy.decide(mutated)
        assertFalse(decision.draftAllowed)
        assertTrue(decision.blockingCodes.any { it.contains("INCLUSION_NONDISCRIMINATION") })
    }

    @Test
    fun `AI cannot certify human dignity pass`() {
        val source = complete()
        val mutated = source.copy(records = source.records.map {
            if (it.kind == EvidenceKind.HUMAN_DIGNITY) it.copy(producer = ai) else it
        })
        val decision = CommitEvidenceEnvelopePolicy.decide(mutated)
        assertFalse(decision.draftAllowed)
        assertTrue(decision.blockingCodes.contains("ENV-HUMAN-AUTHORITY-HUMAN_DIGNITY"))
    }

    @Test
    fun `human safeguard pass without risk typing is unresolved`() {
        val source = complete()
        val mutated = source.copy(records = source.records.map {
            if (it.kind == EvidenceKind.SAFE_HEALTHY_WORK) it.copy(riskSeverity = null) else it
        })
        val decision = CommitEvidenceEnvelopePolicy.decide(mutated)
        assertFalse(decision.draftAllowed)
        assertTrue(decision.tokenVazioCodes.contains("ENV-HUMAN-RISK-SAFE_HEALTHY_WORK"))
    }

    @Test
    fun `wrong head CI never allows ready or merge`() {
        val source = complete()
        val mutated = source.copy(records = source.records.map {
            if (it.kind == EvidenceKind.EXACT_HEAD_CI) it.copy(headSha = "f".repeat(40)) else it
        })
        val decision = CommitEvidenceEnvelopePolicy.decide(mutated)
        assertFalse(decision.readyAllowed)
        assertFalse(decision.mergeAllowed)
        assertTrue(decision.blockingCodes.contains("ENV-CI-HEAD-MISMATCH"))
    }

    @Test
    fun `approval for different plan never allows draft`() {
        val source = complete()
        val mutated = source.copy(records = source.records.map {
            if (it.kind == EvidenceKind.EXACT_APPROVAL) it.copy(subjectDigest = "b".repeat(64)) else it
        })
        val decision = CommitEvidenceEnvelopePolicy.decide(mutated)
        assertFalse(decision.draftAllowed)
        assertFalse(decision.mergeAllowed)
        assertTrue(decision.blockingCodes.contains("ENV-APPROVAL-PLAN-MISMATCH"))
    }

    @Test
    fun `token vazio trusted time blocks merge`() {
        val source = complete()
        val mutated = source.copy(records = source.records.map {
            if (it.kind == EvidenceKind.TRUSTED_TIME) it.copy(state = EvidenceState.TOKEN_VAZIO) else it
        })
        val decision = CommitEvidenceEnvelopePolicy.decide(mutated)
        assertTrue(decision.readyAllowed)
        assertFalse(decision.mergeAllowed)
        assertTrue(decision.tokenVazioCodes.any { it.contains("TRUSTED_TIME") })
    }

    @Test
    fun `timestamp for different artifact is rejected`() {
        val source = complete()
        val mutated = source.copy(records = source.records.map {
            if (it.kind == EvidenceKind.TRUSTED_TIME) it.copy(subjectDigest = "3".repeat(64)) else it
        })
        val decision = CommitEvidenceEnvelopePolicy.decide(mutated)
        assertFalse(decision.mergeAllowed)
        assertTrue(decision.blockingCodes.contains("ENV-TIME-SUBJECT-MISMATCH"))
    }

    @Test
    fun `correlated trusted time and transparency witnesses are rejected`() {
        val source = complete()
        val sameDomainLog = EvidenceProducer(EvidenceProducerKind.EXTERNAL_REGISTRY, "log:two", "tsa-one")
        val mutated = source.copy(records = source.records.map {
            if (it.kind == EvidenceKind.TRANSPARENCY_LOG) it.copy(producer = sameDomainLog) else it
        })
        val decision = CommitEvidenceEnvelopePolicy.decide(mutated)
        assertFalse(decision.mergeAllowed)
        assertTrue(decision.blockingCodes.contains("ENV-CORRELATED-TIME-LOG"))
    }

    @Test
    fun `same producer cannot author source and satisfy independent review`() {
        val source = complete()
        val mutated = source.copy(records = source.records.map {
            if (it.kind == EvidenceKind.INDEPENDENT_REVIEW) it.copy(producer = human) else it
        })
        val decision = CommitEvidenceEnvelopePolicy.decide(mutated)
        assertFalse(decision.mergeAllowed)
        assertTrue(decision.blockingCodes.contains("ENV-SELF-REVIEW"))
    }

    @Test
    fun `private payload disclosure is fail closed`() {
        val decision = CommitEvidenceEnvelopePolicy.decide(complete().copy(privatePayloadIncluded = true))
        assertFalse(decision.draftAllowed)
        assertFalse(decision.mergeAllowed)
        assertTrue(decision.blockingCodes.contains("ENV-PRIVACY-007"))
    }

    @Test
    fun `scientific profile requires typed publication date`() {
        val source = complete(EvidenceProfile.SCIENTIFIC_PUBLICATION)
        val mutated = source.copy(records = source.records.map {
            if (it.kind == EvidenceKind.PUBLICATION_ANCHOR) it.copy(dateType = null) else it
        })
        val decision = CommitEvidenceEnvelopePolicy.decide(mutated)
        assertFalse(decision.mergeAllowed)
        assertTrue(decision.tokenVazioCodes.contains("ENV-PUBLICATION-DATE-TYPING"))
    }

    @Test
    fun `AI generation is not verification by identity`() {
        val source = complete()
        val aiSource = source.copy(records = source.records.map {
            when (it.kind) {
                EvidenceKind.SOURCE_IDENTITY -> it.copy(producer = ai)
                EvidenceKind.INDEPENDENT_REVIEW -> it.copy(producer = ai)
                else -> it
            }
        })
        val decision = CommitEvidenceEnvelopePolicy.decide(aiSource)
        assertFalse(decision.mergeAllowed)
        assertTrue(decision.blockingCodes.contains("ENV-SELF-REVIEW"))
    }
}
