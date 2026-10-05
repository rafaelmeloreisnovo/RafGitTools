package com.rafgittools.ui.screens.rafgitfs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rafgittools.rafgitfs.assurance.CommitEvidenceEnvelope
import com.rafgittools.rafgitfs.assurance.CommitEvidenceEnvelopePolicy
import com.rafgittools.rafgitfs.assurance.EvidenceKind
import com.rafgittools.rafgitfs.assurance.EvidenceProducer
import com.rafgittools.rafgitfs.assurance.EvidenceProducerKind
import com.rafgittools.rafgitfs.assurance.EvidenceProfile
import com.rafgittools.rafgitfs.assurance.EvidenceRecord
import com.rafgittools.rafgitfs.assurance.EvidenceState
import com.rafgittools.rafgitfs.assurance.RiskSeverity
import com.rafgittools.rafgitfs.assurance.UrgencyClass

private data class CommitEvidenceDisplayRow(
    val phase: String,
    val id: String,
    val label: String,
    val status: EvidenceState,
    val evidence: String
)

private val localWorkspaceProducer = EvidenceProducer(
    kind = EvidenceProducerKind.AUTOMATION,
    id = "rafgittools:workspace",
    independenceDomain = "local-workspace"
)

private val localHumanProducer = EvidenceProducer(
    kind = EvidenceProducerKind.HUMAN,
    id = "local:approval-input",
    independenceDomain = "local-human"
)

private val providerProducer = EvidenceProducer(
    kind = EvidenceProducerKind.PROVIDER,
    id = "github:provider",
    independenceDomain = "github-provider"
)

private val unboundHumanSafeguardProducer = EvidenceProducer(
    kind = EvidenceProducerKind.AUTOMATION,
    id = "human-safeguard:unbound",
    independenceDomain = "human-safeguard-unbound"
)

private fun unresolvedHumanSafeguard(
    kind: EvidenceKind,
    risk: RiskSeverity,
    urgency: UrgencyClass,
    falsifier: String,
    mitigation: String
): EvidenceRecord = EvidenceRecord(
    kind = kind,
    state = EvidenceState.TOKEN_VAZIO,
    sourceRef = "TOKEN_VAZIO: traceable human safeguard assessment not connected",
    proofRef = null,
    producer = unboundHumanSafeguardProducer,
    riskSeverity = risk,
    urgency = urgency,
    falsifierRef = falsifier,
    mitigationRef = mitigation,
    claimAllowed = false
)

private fun buildCommitEvidenceEnvelope(
    repositoryFullName: String,
    refName: String,
    state: WorkspaceEditorUiState
): CommitEvidenceEnvelope {
    val plan = state.plan
    val baseSha = plan?.baseCommitSha
    val planHash = plan?.planHash

    val approvalState = when {
        state.expectedApproval == null || planHash == null -> EvidenceState.TOKEN_VAZIO
        state.approvalText.isBlank() -> EvidenceState.TOKEN_VAZIO
        state.approvalText == state.expectedApproval -> EvidenceState.PASS
        else -> EvidenceState.FAIL
    }

    val conflictState = when {
        plan == null -> EvidenceState.TOKEN_VAZIO
        state.conflicts.any { it.resolvedAt == null } -> EvidenceState.FAIL
        else -> EvidenceState.PASS
    }

    val records = listOf(
        unresolvedHumanSafeguard(
            EvidenceKind.HUMAN_DIGNITY,
            RiskSeverity.CRITICAL,
            UrgencyClass.P0,
            "Block promotion when dignity impact is adverse or unassessed",
            "Require traceable human dignity review and reversible mitigation before promotion"
        ),
        unresolvedHumanSafeguard(
            EvidenceKind.CHILD_SAFETY,
            RiskSeverity.CRITICAL,
            UrgencyClass.P0,
            "Block promotion when child-safety relevance or best-interest assessment is unresolved",
            "Require human child-safety scope review and documented protective controls"
        ),
        unresolvedHumanSafeguard(
            EvidenceKind.INCLUSION_NONDISCRIMINATION,
            RiskSeverity.CRITICAL,
            UrgencyClass.P0,
            "Block promotion on discriminatory exclusion or unassessed disparate impact",
            "Require human non-discrimination/inclusion review and corrective controls"
        ),
        unresolvedHumanSafeguard(
            EvidenceKind.ACCESSIBILITY_INCLUSION,
            RiskSeverity.HIGH,
            UrgencyClass.P1,
            "Block promotion while accessibility impact remains unassessed",
            "Require accessibility review, documented limitations and corrective path"
        ),
        unresolvedHumanSafeguard(
            EvidenceKind.SAFE_HEALTHY_WORK,
            RiskSeverity.CRITICAL,
            UrgencyClass.P0,
            "Block promotion on unsafe work practice or unassessed worker-safety impact",
            "Require human work-safety review and mitigation/rollback path"
        ),
        EvidenceRecord(
            kind = EvidenceKind.SOURCE_IDENTITY,
            state = if (repositoryFullName.isNotBlank() && refName.isNotBlank()) EvidenceState.PASS else EvidenceState.FAIL,
            sourceRef = if (repositoryFullName.isNotBlank() && refName.isNotBlank()) "$repositoryFullName@$refName" else "TOKEN_VAZIO",
            producer = providerProducer
        ),
        EvidenceRecord(
            kind = EvidenceKind.PRIVATE_WORKSPACE,
            state = if (state.workspace != null) EvidenceState.PASS else EvidenceState.TOKEN_VAZIO,
            sourceRef = state.workspace?.workspaceId ?: "TOKEN_VAZIO",
            producer = localWorkspaceProducer
        ),
        EvidenceRecord(
            kind = EvidenceKind.BASE_COMMIT,
            state = if (baseSha.isNullOrBlank()) EvidenceState.TOKEN_VAZIO else EvidenceState.PASS,
            sourceRef = baseSha ?: "TOKEN_VAZIO",
            headSha = baseSha,
            producer = providerProducer
        ),
        EvidenceRecord(
            kind = EvidenceKind.CONFLICT_BOUNDARY,
            state = conflictState,
            sourceRef = when (conflictState) {
                EvidenceState.PASS -> "unresolved=0"
                EvidenceState.FAIL -> "unresolved=${state.conflicts.count { it.resolvedAt == null }}"
                else -> "TOKEN_VAZIO"
            },
            producer = localWorkspaceProducer
        ),
        EvidenceRecord(
            kind = EvidenceKind.PLAN_HASH,
            state = if (planHash.isNullOrBlank()) EvidenceState.TOKEN_VAZIO else EvidenceState.PASS,
            sourceRef = planHash ?: "TOKEN_VAZIO",
            subjectDigest = planHash,
            producer = localWorkspaceProducer
        ),
        EvidenceRecord(
            kind = EvidenceKind.EXACT_APPROVAL,
            state = approvalState,
            sourceRef = state.expectedApproval ?: "TOKEN_VAZIO",
            subjectDigest = if (approvalState == EvidenceState.PASS) planHash else null,
            producer = localHumanProducer
        ),
        EvidenceRecord(
            kind = EvidenceKind.NO_DESTRUCTIVE_WRITE,
            state = EvidenceState.PASS,
            sourceRef = "RafGitFS governed-write contract",
            producer = localWorkspaceProducer
        ),
        EvidenceRecord(
            kind = EvidenceKind.EXECUTION_RECEIPT,
            state = EvidenceState.NOT_RUN,
            sourceRef = "No canonical post-execution receipt adapter connected to this screen",
            producer = providerProducer
        ),
        EvidenceRecord(
            kind = EvidenceKind.EXACT_HEAD_CI,
            state = EvidenceState.TOKEN_VAZIO,
            sourceRef = "Provider exact-head CI adapter not connected",
            producer = providerProducer
        ),
        EvidenceRecord(
            kind = EvidenceKind.SERVER_ENFORCEMENT,
            state = EvidenceState.TOKEN_VAZIO,
            sourceRef = "Provider server-enforcement readback not connected",
            producer = providerProducer
        ),
        EvidenceRecord(
            kind = EvidenceKind.TRUSTED_TIME,
            state = EvidenceState.TOKEN_VAZIO,
            sourceRef = "RFC3161-compatible trusted-time adapter not connected",
            producer = EvidenceProducer(EvidenceProducerKind.EXTERNAL_REGISTRY, "trusted-time:unbound", "trusted-time-unbound")
        ),
        EvidenceRecord(
            kind = EvidenceKind.TRANSPARENCY_LOG,
            state = EvidenceState.TOKEN_VAZIO,
            sourceRef = "Transparency inclusion/consistency adapter not connected",
            producer = EvidenceProducer(EvidenceProducerKind.EXTERNAL_REGISTRY, "transparency:unbound", "transparency-unbound")
        ),
        EvidenceRecord(
            kind = EvidenceKind.INDEPENDENT_REVIEW,
            state = EvidenceState.TOKEN_VAZIO,
            sourceRef = "Independent review not observed",
            producer = EvidenceProducer(EvidenceProducerKind.AI_AGENT, "reviewer:unbound", "independent-review-unbound")
        ),
        EvidenceRecord(
            kind = EvidenceKind.PUBLICATION_ANCHOR,
            state = EvidenceState.NOT_APPLICABLE,
            sourceRef = "Optional scientific/publication profile",
            producer = EvidenceProducer(EvidenceProducerKind.EXTERNAL_REGISTRY, "publication:unbound", "publication-unbound")
        )
    )

    return CommitEvidenceEnvelope(
        repositoryFullName = repositoryFullName,
        refName = refName,
        baseCommitSha = baseSha,
        planHash = planHash,
        headSha = null,
        artifactDigest = null,
        profile = EvidenceProfile.SOFTWARE,
        records = records,
        privatePayloadIncluded = false,
        claimAllowed = false
    )
}

private fun displayRows(envelope: CommitEvidenceEnvelope): List<CommitEvidenceDisplayRow> {
    val phaseByKind = mapOf(
        EvidenceKind.HUMAN_DIGNITY to "HUMAN-FIRST",
        EvidenceKind.CHILD_SAFETY to "HUMAN-FIRST",
        EvidenceKind.INCLUSION_NONDISCRIMINATION to "HUMAN-FIRST",
        EvidenceKind.ACCESSIBILITY_INCLUSION to "HUMAN-FIRST",
        EvidenceKind.SAFE_HEALTHY_WORK to "HUMAN-FIRST",
        EvidenceKind.SOURCE_IDENTITY to "PRE",
        EvidenceKind.PRIVATE_WORKSPACE to "PRE",
        EvidenceKind.BASE_COMMIT to "PRE",
        EvidenceKind.CONFLICT_BOUNDARY to "PRE",
        EvidenceKind.PLAN_HASH to "ACT",
        EvidenceKind.EXACT_APPROVAL to "ACT",
        EvidenceKind.NO_DESTRUCTIVE_WRITE to "ACT",
        EvidenceKind.EXECUTION_RECEIPT to "POST",
        EvidenceKind.EXACT_HEAD_CI to "POST",
        EvidenceKind.SERVER_ENFORCEMENT to "POST",
        EvidenceKind.TRUSTED_TIME to "POST",
        EvidenceKind.TRANSPARENCY_LOG to "POST",
        EvidenceKind.INDEPENDENT_REVIEW to "POST",
        EvidenceKind.PUBLICATION_ANCHOR to "POST"
    )

    val rows = envelope.records.map { record ->
        val evidenceBits = mutableListOf<String>()
        record.riskSeverity?.let { evidenceBits += "risk=${it.name}" }
        record.urgency?.let { evidenceBits += "urgency=${it.name}" }
        evidenceBits += "source=${record.sourceRef}"
        CommitEvidenceDisplayRow(
            phase = phaseByKind.getValue(record.kind),
            id = record.kind.name,
            label = record.kind.name.replace('_', ' '),
            status = record.state,
            evidence = evidenceBits.joinToString(" · ")
        )
    }.toMutableList()

    rows += CommitEvidenceDisplayRow(
        phase = "POST",
        id = "HEAD_SHA",
        label = "Exact PR head SHA",
        status = if (envelope.headSha == null) EvidenceState.TOKEN_VAZIO else EvidenceState.PASS,
        evidence = envelope.headSha ?: "TOKEN_VAZIO"
    )
    rows += CommitEvidenceDisplayRow(
        phase = "POST",
        id = "ARTIFACT_DIGEST",
        label = "Canonical artifact/evidence-root digest",
        status = if (envelope.artifactDigest == null) EvidenceState.TOKEN_VAZIO else EvidenceState.PASS,
        evidence = envelope.artifactDigest ?: "TOKEN_VAZIO"
    )
    return rows
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitEvidenceGateScreen(
    repositoryFullName: String,
    refName: String,
    state: WorkspaceEditorUiState,
    onNavigateBack: () -> Unit
) {
    val envelope = buildCommitEvidenceEnvelope(repositoryFullName, refName, state)
    val decision = CommitEvidenceEnvelopePolicy.decide(envelope)
    val rows = displayRows(envelope)

    val reason = when {
        decision.mergeAllowed -> "MERGE evidence boundary satisfied"
        decision.readyAllowed -> "Ready boundary satisfied; merge evidence remains incomplete"
        decision.draftAllowed -> "Draft boundary satisfied; POST evidence remains unresolved"
        decision.blockingCodes.isNotEmpty() -> "Fail-closed: blocking evidence failure present"
        else -> "Fail-closed: required evidence remains TOKEN_VAZIO / NOT_RUN"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Commit Evidence Gate")
                        Text("$repositoryFullName@$refName", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Return to governed workspace")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Human dignity first · fail-closed promotion", style = MaterialTheme.typography.titleMedium)
                        Text(reason)
                        Text("HUMAN_DIGNITY > DELIVERY_SPEED · CHILD_SAFETY > FEATURE_COMPLETION", fontFamily = FontFamily.Monospace)
                        Text("UNKNOWN_HUMAN_IMPACT ≠ SAFE · AI_ASSESSMENT ≠ HUMAN_AUTHORITY", fontFamily = FontFamily.Monospace)
                        Text("SOURCE ≠ ARTIFACT ≠ EXECUTION ≠ EVIDENCE ≠ CLAIM", fontFamily = FontFamily.Monospace)
                        Text("AI_GENERATED ≠ VERIFIED · TOKEN_VAZIO ≠ PASS", fontFamily = FontFamily.Monospace)
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Promotion state", style = MaterialTheme.typography.titleMedium)
                        PromotionLine("Draft PR", decision.draftAllowed)
                        PromotionLine("Ready for review", decision.readyAllowed)
                        PromotionLine("Merge", decision.mergeAllowed)
                        if (decision.blockingCodes.isNotEmpty()) {
                            Text("FAIL: ${decision.blockingCodes.joinToString()}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (decision.tokenVazioCodes.isNotEmpty()) {
                            Text("TOKEN_VAZIO: ${decision.tokenVazioCodes.joinToString()}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            items(rows, key = { "${it.phase}:${it.id}" }) { check ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${check.phase} · ${check.label}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(check.status.name, fontFamily = FontFamily.Monospace)
                        }
                        Text(check.id, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                        Text(check.evidence, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Temporal / publication anchoring", style = MaterialTheme.typography.titleMedium)
                        Text("Independent anchors must bind the same canonical digest. Git timestamps alone are metadata, not sufficient real-world anteriority proof.")
                        Text("Public anchors receive minimized digest/attestation only: PUBLIC_PROOF ≠ PUBLIC_PAYLOAD.")
                        Text("Seed ${envelope.reproducibilitySeed} is for deterministic reconstruction/falsifiers, not security randomness or timestamp authority.")
                    }
                }
            }

            item {
                OutlinedButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Return to HUMAN-FIRST / PRE / ACT governed plan")
                }
            }
        }
    }
}

@Composable
private fun PromotionLine(label: String, allowed: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(if (allowed) "ALLOWED" else "BLOCKED", fontFamily = FontFamily.Monospace)
    }
}
