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

enum class CommitEvidenceStatus {
    PASS,
    FAIL,
    TOKEN_VAZIO,
    NOT_RUN,
    NOT_APPLICABLE
}

data class CommitEvidenceCheck(
    val phase: String,
    val id: String,
    val label: String,
    val status: CommitEvidenceStatus,
    val evidence: String
)

data class CommitEvidenceGateDecision(
    val draftAllowed: Boolean,
    val readyAllowed: Boolean,
    val mergeAllowed: Boolean,
    val reason: String
)

private fun deriveCommitEvidenceChecks(
    repositoryFullName: String,
    refName: String,
    state: WorkspaceEditorUiState
): List<CommitEvidenceCheck> {
    val plan = state.plan
    val approvalStatus = when {
        state.expectedApproval == null -> CommitEvidenceStatus.TOKEN_VAZIO
        state.approvalText.isBlank() -> CommitEvidenceStatus.TOKEN_VAZIO
        state.approvalText == state.expectedApproval -> CommitEvidenceStatus.PASS
        else -> CommitEvidenceStatus.FAIL
    }
    val conflictStatus = when {
        plan == null -> CommitEvidenceStatus.TOKEN_VAZIO
        state.conflicts.any { it.resolvedAt == null } -> CommitEvidenceStatus.FAIL
        else -> CommitEvidenceStatus.PASS
    }
    val executionStatus = when {
        state.dryRun.isEmpty() -> CommitEvidenceStatus.NOT_RUN
        state.dryRun.all { it.evidenceState == "OBSERVED" } -> CommitEvidenceStatus.PASS
        else -> CommitEvidenceStatus.TOKEN_VAZIO
    }

    return listOf(
        CommitEvidenceCheck(
            "PRE",
            "SOURCE_IDENTITY",
            "Repository + ref identity",
            if (repositoryFullName.isNotBlank() && refName.isNotBlank()) CommitEvidenceStatus.PASS else CommitEvidenceStatus.FAIL,
            if (repositoryFullName.isNotBlank() && refName.isNotBlank()) "$repositoryFullName@$refName" else "TOKEN_VAZIO"
        ),
        CommitEvidenceCheck(
            "PRE",
            "PRIVATE_WORKSPACE",
            "Private workspace contract",
            if (state.workspace != null) CommitEvidenceStatus.PASS else CommitEvidenceStatus.TOKEN_VAZIO,
            state.workspace?.workspaceId?.take(12) ?: "TOKEN_VAZIO"
        ),
        CommitEvidenceCheck(
            "PRE",
            "BASE_COMMIT",
            "Observed base commit",
            if (plan?.baseCommitSha.isNullOrBlank()) CommitEvidenceStatus.TOKEN_VAZIO else CommitEvidenceStatus.PASS,
            plan?.baseCommitSha ?: "TOKEN_VAZIO"
        ),
        CommitEvidenceCheck(
            "PRE",
            "CONFLICT_BOUNDARY",
            "Three-way conflicts resolved",
            conflictStatus,
            when (conflictStatus) {
                CommitEvidenceStatus.PASS -> "No unresolved conflict"
                CommitEvidenceStatus.FAIL -> "${state.conflicts.count { it.resolvedAt == null }} unresolved conflict(s)"
                else -> "Plan not materialized"
            }
        ),
        CommitEvidenceCheck(
            "ACT",
            "PLAN_HASH",
            "Canonical plan identity",
            if (plan == null) CommitEvidenceStatus.TOKEN_VAZIO else CommitEvidenceStatus.PASS,
            plan?.planHash ?: "TOKEN_VAZIO"
        ),
        CommitEvidenceCheck(
            "ACT",
            "EXACT_APPROVAL",
            "Exact human approval bound to planHash",
            approvalStatus,
            state.expectedApproval ?: "TOKEN_VAZIO"
        ),
        CommitEvidenceCheck(
            "ACT",
            "NO_DESTRUCTIVE_WRITE",
            "No direct main write / force push / remote delete",
            CommitEvidenceStatus.PASS,
            "RafGitFS governed-write contract"
        ),
        CommitEvidenceCheck(
            "POST",
            "EXECUTION_RECEIPT",
            "Observed governed execution outcomes",
            executionStatus,
            if (state.dryRun.isEmpty()) "NOT_RUN" else state.dryRun.joinToString(" | ") { "${it.step.action}:${it.evidenceState}" }.take(280)
        ),
        CommitEvidenceCheck(
            "POST",
            "EXACT_HEAD_CI",
            "CI bound to exact PR head",
            CommitEvidenceStatus.TOKEN_VAZIO,
            "Provider evidence adapter not connected to this screen yet"
        ),
        CommitEvidenceCheck(
            "POST",
            "SERVER_ENFORCEMENT",
            "Server-side branch/ruleset enforcement",
            CommitEvidenceStatus.TOKEN_VAZIO,
            "Provider readback required; local policy is not sufficient"
        ),
        CommitEvidenceCheck(
            "POST",
            "TRUSTED_TIME",
            "Independent signed time evidence",
            CommitEvidenceStatus.TOKEN_VAZIO,
            "RFC3161/TSA or equivalent trusted-time proof not attached"
        ),
        CommitEvidenceCheck(
            "POST",
            "TRANSPARENCY_LOG",
            "Append-only transparency inclusion proof",
            CommitEvidenceStatus.TOKEN_VAZIO,
            "Transparency-log inclusion/consistency proof not attached"
        ),
        CommitEvidenceCheck(
            "POST",
            "PUBLICATION_ANCHOR",
            "DOI/publication/date anchors when scientifically applicable",
            CommitEvidenceStatus.NOT_APPLICABLE,
            "Optional profile: scientific/publication provenance"
        )
    )
}

private fun deriveCommitEvidenceDecision(checks: List<CommitEvidenceCheck>): CommitEvidenceGateDecision {
    fun status(id: String) = checks.first { it.id == id }.status

    val draftAllowed = listOf(
        "SOURCE_IDENTITY",
        "PRIVATE_WORKSPACE",
        "BASE_COMMIT",
        "CONFLICT_BOUNDARY",
        "PLAN_HASH",
        "EXACT_APPROVAL",
        "NO_DESTRUCTIVE_WRITE"
    ).all { status(it) == CommitEvidenceStatus.PASS }

    val readyAllowed = draftAllowed &&
        status("EXECUTION_RECEIPT") == CommitEvidenceStatus.PASS &&
        status("EXACT_HEAD_CI") == CommitEvidenceStatus.PASS

    val mergeAllowed = readyAllowed && listOf(
        "SERVER_ENFORCEMENT",
        "TRUSTED_TIME",
        "TRANSPARENCY_LOG"
    ).all { status(it) == CommitEvidenceStatus.PASS }

    val reason = when {
        mergeAllowed -> "MERGE evidence boundary satisfied"
        readyAllowed -> "Ready-for-review boundary satisfied; merge evidence still incomplete"
        draftAllowed -> "Draft publication boundary satisfied; post-publication evidence is still required"
        else -> "Fail-closed: one or more required PRE/ACT checks are FAIL or TOKEN_VAZIO"
    }
    return CommitEvidenceGateDecision(draftAllowed, readyAllowed, mergeAllowed, reason)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitEvidenceGateScreen(
    repositoryFullName: String,
    refName: String,
    state: WorkspaceEditorUiState,
    onNavigateBack: () -> Unit
) {
    val checks = deriveCommitEvidenceChecks(repositoryFullName, refName, state)
    val decision = deriveCommitEvidenceDecision(checks)

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
                        Text("Fail-closed promotion boundary", style = MaterialTheme.typography.titleMedium)
                        Text(decision.reason)
                        Text("SOURCE ≠ ARTIFACT ≠ EXECUTION ≠ EVIDENCE ≠ CLAIM", fontFamily = FontFamily.Monospace)
                        Text("TOKEN_VAZIO is evidence of an unresolved boundary; it is never coerced to PASS.")
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
                    }
                }
            }

            items(checks, key = { "${it.phase}:${it.id}" }) { check ->
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
                        Text("Use multiple independent anchors. Git timestamps alone are not proof of real-world time. A future adapter may bind artifact digest + commit/tree + signed TSA timestamp + transparency-log proof + DOI/publication metadata when applicable.")
                        Text("Do not publish private payloads: external anchors should receive only minimized digests/attestations needed for verification.")
                    }
                }
            }

            item {
                OutlinedButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Return to PRE / ACT governed plan")
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
