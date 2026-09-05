package com.rafgittools.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.github.ActionsPermissionsSnapshot
import com.rafgittools.data.github.ActionsWorkflowPermissionsSnapshot
import com.rafgittools.data.github.BranchProtectionRequest
import com.rafgittools.data.github.BranchProtectionSnapshot
import com.rafgittools.data.github.GovernanceAuditInput
import com.rafgittools.data.github.GovernanceAuditReport
import com.rafgittools.data.github.GovernanceControlState
import com.rafgittools.data.github.GovernanceFeatureStatus
import com.rafgittools.data.github.GovernanceRepositoryDetails
import com.rafgittools.data.github.GovernanceRepositorySummary
import com.rafgittools.data.github.RepositoryGovernanceApiService
import com.rafgittools.data.github.RepositoryGovernanceAuditor
import com.rafgittools.data.github.RepositoryGovernanceReceiptStore
import com.rafgittools.data.github.RepositoryRulesetSummary
import com.rafgittools.data.github.UpdateActionsWorkflowPermissionsRequest
import com.rafgittools.data.github.UpdateGovernanceSecurityAndAnalysis
import com.rafgittools.data.github.UpdateRepositoryGovernanceRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

enum class GovernanceEvidenceState {
    OBSERVED, READY, APPLYING, APPLIED, FAILED, TOKEN_VAZIO
}

enum class GovernanceField {
    HAS_ISSUES,
    HAS_PROJECTS,
    HAS_WIKI,
    HAS_DISCUSSIONS,
    ALLOW_MERGE_COMMIT,
    ALLOW_SQUASH_MERGE,
    ALLOW_REBASE_MERGE,
    ALLOW_AUTO_MERGE,
    ALLOW_UPDATE_BRANCH,
    DELETE_BRANCH_ON_MERGE,
    WEB_COMMIT_SIGNOFF_REQUIRED,
    BRANCH_PROTECTION,
    VULNERABILITY_ALERTS,
    AUTOMATED_SECURITY_FIXES,
    PRIVATE_VULNERABILITY_REPORTING,
    ADVANCED_SECURITY,
    SECRET_SCANNING,
    SECRET_SCANNING_PUSH_PROTECTION,
    ACTIONS_READ_ONLY_DEFAULT,
    ACTIONS_CAN_APPROVE_PULL_REQUESTS
}

data class ObservedRepositoryGovernance(
    val details: GovernanceRepositoryDetails,
    val branchProtectionEnabled: Boolean?,
    val branchProtection: BranchProtectionSnapshot?,
    val rulesets: List<RepositoryRulesetSummary>?,
    val actionsPermissions: ActionsPermissionsSnapshot?,
    val workflowPermissions: ActionsWorkflowPermissionsSnapshot?,
    val vulnerabilityAlertsEnabled: Boolean?,
    val automatedSecurityFixesEnabled: Boolean?,
    val privateVulnerabilityReportingEnabled: Boolean?,
    val advancedSecurityEnabled: Boolean?,
    val secretScanningEnabled: Boolean?,
    val secretScanningPushProtectionEnabled: Boolean?
)

data class DesiredRepositoryGovernance(
    val hasIssues: Boolean,
    val hasProjects: Boolean,
    val hasWiki: Boolean,
    val hasDiscussions: Boolean,
    val allowMergeCommit: Boolean,
    val allowSquashMerge: Boolean,
    val allowRebaseMerge: Boolean,
    val allowAutoMerge: Boolean,
    val allowUpdateBranch: Boolean,
    val deleteBranchOnMerge: Boolean,
    val webCommitSignoffRequired: Boolean,
    val branchProtectionEnabled: Boolean,
    val vulnerabilityAlertsEnabled: Boolean,
    val automatedSecurityFixesEnabled: Boolean,
    val privateVulnerabilityReportingEnabled: Boolean,
    val advancedSecurityEnabled: Boolean,
    val secretScanningEnabled: Boolean,
    val secretScanningPushProtectionEnabled: Boolean,
    val actionsReadOnlyDefault: Boolean,
    val actionsCanApprovePullRequests: Boolean
)

data class RepositoryGovernanceUiState(
    val repositories: List<GovernanceRepositorySummary> = emptyList(),
    val selectedRepository: GovernanceRepositorySummary? = null,
    val observed: ObservedRepositoryGovernance? = null,
    val desired: DesiredRepositoryGovernance? = null,
    val dirtyFields: Set<GovernanceField> = emptySet(),
    val auditReport: GovernanceAuditReport? = null,
    val receiptChainStatus: RepositoryGovernanceReceiptStore.GovernanceReceiptChainStatus? = null,
    val evidenceState: GovernanceEvidenceState = GovernanceEvidenceState.TOKEN_VAZIO,
    val isLoadingRepositories: Boolean = false,
    val isLoadingRepository: Boolean = false,
    val isAuditing: Boolean = false,
    val message: String? = null,
    val lastReceiptId: String? = null,
    val receiptPath: String? = null
) {
    val adminAuthorityProven: Boolean
        get() = selectedRepository?.permissions?.admin == true || observed?.details?.permissions?.admin == true

    val canApply: Boolean
        get() = adminAuthorityProven &&
            selectedRepository?.archived != true &&
            dirtyFields.isNotEmpty() &&
            evidenceState != GovernanceEvidenceState.APPLYING
}

@HiltViewModel
class RepositoryGovernanceViewModel @Inject constructor(
    private val api: RepositoryGovernanceApiService,
    private val receiptStore: RepositoryGovernanceReceiptStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepositoryGovernanceUiState())
    val uiState: StateFlow<RepositoryGovernanceUiState> = _uiState.asStateFlow()

    init {
        refreshRepositories()
    }

    fun refreshRepositories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingRepositories = true, message = null)
            runCatching { listAllRepositories() }
                .onSuccess { repositories ->
                    val ordered = repositories.sortedWith(
                        compareByDescending<GovernanceRepositorySummary> { it.permissions?.admin == true }
                            .thenBy { it.fullName.lowercase() }
                    )
                    val currentName = _uiState.value.selectedRepository?.fullName
                    _uiState.value = _uiState.value.copy(
                        repositories = ordered,
                        isLoadingRepositories = false,
                        evidenceState = if (ordered.isEmpty()) GovernanceEvidenceState.TOKEN_VAZIO else GovernanceEvidenceState.OBSERVED,
                        message = if (ordered.isEmpty()) {
                            "TOKEN_VAZIO: no repositories visible to the authenticated provider."
                        } else {
                            "Observed ${ordered.size} repository/repositories across all provider pages."
                        }
                    )
                    val next = currentName?.let { name -> ordered.firstOrNull { it.fullName == name } }
                        ?: ordered.firstOrNull { it.permissions?.admin == true }
                        ?: ordered.firstOrNull()
                    next?.let { selectRepository(it.fullName) }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingRepositories = false,
                        evidenceState = GovernanceEvidenceState.TOKEN_VAZIO,
                        message = "TOKEN_VAZIO: repository inventory unavailable (${providerMessage(error)})."
                    )
                }
        }
    }

    private suspend fun listAllRepositories(): List<GovernanceRepositorySummary> {
        val all = LinkedHashMap<String, GovernanceRepositorySummary>()
        var page = 1
        while (true) {
            val batch = api.listRepositories(page = page, perPage = PAGE_SIZE)
            batch.forEach { all[it.fullName] = it }
            if (batch.size < PAGE_SIZE) break
            page += 1
            check(page <= MAX_REPOSITORY_PAGES) {
                "TOKEN_VAZIO: repository inventory exceeded guarded pagination bound"
            }
        }
        return all.values.toList()
    }

    fun selectRepository(fullName: String) {
        val repository = _uiState.value.repositories.firstOrNull { it.fullName == fullName } ?: return
        _uiState.value = _uiState.value.copy(
            selectedRepository = repository,
            observed = null,
            desired = null,
            dirtyFields = emptySet(),
            auditReport = null,
            isLoadingRepository = true,
            evidenceState = GovernanceEvidenceState.TOKEN_VAZIO,
            message = null
        )
        viewModelScope.launch { probe(repository) }
    }

    fun refreshSelected() {
        val repository = _uiState.value.selectedRepository ?: return
        _uiState.value = _uiState.value.copy(isLoadingRepository = true, message = null)
        viewModelScope.launch { probe(repository, preserveDirty = true) }
    }

    fun runDeepAudit() {
        val repository = _uiState.value.selectedRepository ?: return
        _uiState.value = _uiState.value.copy(isAuditing = true, message = "Running provider-bound governance audit…")
        viewModelScope.launch {
            probe(
                repository = repository,
                preserveDirty = true,
                preserveMessage = false,
                writeAuditReceipt = true,
                auditOperation = "repository_governance_deep_audit"
            )
        }
    }

    fun setField(field: GovernanceField, enabled: Boolean) {
        val state = _uiState.value
        val observed = state.observed ?: return
        val desired = state.desired ?: return
        val updated = desired.withField(field, enabled)
        val dirty = state.dirtyFields.toMutableSet()
        if (matchesObserved(field, enabled, observed)) dirty.remove(field) else dirty.add(field)
        _uiState.value = state.copy(
            desired = updated,
            dirtyFields = dirty,
            evidenceState = if (dirty.isEmpty()) GovernanceEvidenceState.OBSERVED else GovernanceEvidenceState.READY,
            message = if (observedValue(field, observed) == null) {
                "TOKEN_VAZIO: ${field.name} provider pre-state is not proven; mutation will remain blocked."
            } else null
        )
    }

    /**
     * Stages a conservative baseline. Existing protection is preserved rather than rewritten.
     * Rulesets remain audit-only in V2 because a ruleset PUT/POST is replacement-shaped and needs
     * a complete provider fidelity model before safe mutation.
     */
    fun useRecommendedBaseline() {
        val state = _uiState.value
        val observed = state.observed ?: return
        var desired = state.desired ?: return
        val dirty = state.dirtyFields.toMutableSet()
        val gaps = mutableListOf<String>()

        fun stage(field: GovernanceField, enabled: Boolean) {
            desired = desired.withField(field, enabled)
            if (matchesObserved(field, enabled, observed)) dirty.remove(field) else dirty.add(field)
        }

        stage(GovernanceField.DELETE_BRANCH_ON_MERGE, true)
        stage(GovernanceField.WEB_COMMIT_SIGNOFF_REQUIRED, true)
        stage(GovernanceField.ALLOW_UPDATE_BRANCH, true)
        stage(GovernanceField.VULNERABILITY_ALERTS, true)
        stage(GovernanceField.AUTOMATED_SECURITY_FIXES, true)

        when (observed.branchProtectionEnabled) {
            false -> stage(GovernanceField.BRANCH_PROTECTION, true)
            true -> Unit
            null -> gaps += "branch protection"
        }

        if (!observed.details.isPrivate) {
            if (observed.privateVulnerabilityReportingEnabled != null) {
                stage(GovernanceField.PRIVATE_VULNERABILITY_REPORTING, true)
            } else gaps += "private vulnerability reporting"
        }

        if (observed.advancedSecurityEnabled != null) {
            stage(GovernanceField.ADVANCED_SECURITY, true)
        } else gaps += "advanced security"

        if (observed.secretScanningEnabled != null) {
            stage(GovernanceField.SECRET_SCANNING, true)
        } else gaps += "secret scanning"

        if (observed.secretScanningPushProtectionEnabled != null) {
            stage(GovernanceField.SECRET_SCANNING_PUSH_PROTECTION, true)
        } else gaps += "push protection"

        if (observed.workflowPermissions?.defaultWorkflowPermissions != null) {
            stage(GovernanceField.ACTIONS_READ_ONLY_DEFAULT, true)
        } else gaps += "Actions default token permission"

        if (observed.workflowPermissions?.canApprovePullRequestReviews != null) {
            stage(GovernanceField.ACTIONS_CAN_APPROVE_PULL_REQUESTS, false)
        } else gaps += "Actions PR approval authority"

        _uiState.value = state.copy(
            desired = desired,
            dirtyFields = dirty,
            evidenceState = if (dirty.isEmpty()) GovernanceEvidenceState.OBSERVED else GovernanceEvidenceState.READY,
            message = buildString {
                append("Conservative baseline staged; existing rulesets/protection are not destructively rewritten.")
                if (gaps.isNotEmpty()) append(" TOKEN_VAZIO preserved for: ${gaps.joinToString(", ")}.")
            }
        )
    }

    fun discardChanges() {
        val observed = _uiState.value.observed ?: return
        _uiState.value = _uiState.value.copy(
            desired = desiredFromObserved(observed),
            dirtyFields = emptySet(),
            evidenceState = GovernanceEvidenceState.OBSERVED,
            message = "Staged changes discarded; provider state unchanged."
        )
    }

    fun applyChanges() {
        val state = _uiState.value
        val repository = state.selectedRepository ?: return
        val observed = state.observed ?: return
        val desired = state.desired ?: return
        val dirty = state.dirtyFields
        if (dirty.isEmpty()) return

        if (!state.adminAuthorityProven) {
            val receipt = receiptStore.appendDetailed(
                repository = repository.fullName,
                operation = "repository_governance_apply",
                outcome = "TOKEN_VAZIO",
                details = "Provider inventory/details did not prove admin permission; no mutation attempted.",
                beforeSnapshot = providerSnapshot(observed),
                gaps = listOf("ADMIN_AUTHORITY_NOT_PROVEN")
            )
            _uiState.value = state.copy(
                evidenceState = GovernanceEvidenceState.TOKEN_VAZIO,
                message = "TOKEN_VAZIO: admin authority is required for repository governance writes.",
                lastReceiptId = receipt,
                receiptPath = receiptStore.path(),
                receiptChainStatus = receiptStore.verifyChain()
            )
            return
        }
        if (repository.archived) {
            val receipt = receiptStore.appendDetailed(
                repository = repository.fullName,
                operation = "repository_governance_apply",
                outcome = "BLOCKED",
                details = "Archived repository is read-only; no mutation attempted.",
                beforeSnapshot = providerSnapshot(observed),
                gaps = listOf("ARCHIVED_REPOSITORY")
            )
            _uiState.value = state.copy(
                evidenceState = GovernanceEvidenceState.FAILED,
                message = "Archived repositories are read-only; no mutation attempted.",
                lastReceiptId = receipt,
                receiptPath = receiptStore.path(),
                receiptChainStatus = receiptStore.verifyChain()
            )
            return
        }

        _uiState.value = state.copy(
            evidenceState = GovernanceEvidenceState.APPLYING,
            message = "Applying ${dirty.size} staged governance field(s)…"
        )

        viewModelScope.launch {
            val coordinates = repositoryCoordinates(repository.fullName)
            if (coordinates == null) {
                _uiState.value = _uiState.value.copy(
                    evidenceState = GovernanceEvidenceState.FAILED,
                    message = "Invalid repository identity: ${repository.fullName}"
                )
                return@launch
            }
            val (owner, repo) = coordinates
            val succeeded = mutableSetOf<GovernanceField>()
            val failures = mutableListOf<String>()

            val structuralFields = dirty intersect STRUCTURAL_FIELDS
            if (structuralFields.isNotEmpty()) {
                runCatching { api.updateRepository(owner, repo, structuralRequest(desired, structuralFields)) }
                    .onSuccess { succeeded += structuralFields }
                    .onFailure { failures += "structure: ${providerMessage(it)}" }
            }

            val analysisFields = dirty intersect ANALYSIS_FIELDS
            if (analysisFields.isNotEmpty()) {
                runCatching { api.updateRepository(owner, repo, securityAnalysisRequest(desired, analysisFields)) }
                    .onSuccess { succeeded += analysisFields }
                    .onFailure { failures += "security_and_analysis: ${providerMessage(it)}" }
            }

            applyToggleEndpoint(
                field = GovernanceField.VULNERABILITY_ALERTS,
                dirty = dirty,
                desired = desired.vulnerabilityAlertsEnabled,
                observed = observed.vulnerabilityAlertsEnabled,
                enable = { api.enableVulnerabilityAlerts(owner, repo) },
                disable = { api.disableVulnerabilityAlerts(owner, repo) },
                succeeded = succeeded,
                failures = failures,
                label = "vulnerability_alerts"
            )

            applyToggleEndpoint(
                field = GovernanceField.AUTOMATED_SECURITY_FIXES,
                dirty = dirty,
                desired = desired.automatedSecurityFixesEnabled,
                observed = observed.automatedSecurityFixesEnabled,
                enable = { api.enableAutomatedSecurityFixes(owner, repo) },
                disable = { api.disableAutomatedSecurityFixes(owner, repo) },
                succeeded = succeeded,
                failures = failures,
                label = "automated_security_fixes"
            )

            if (GovernanceField.PRIVATE_VULNERABILITY_REPORTING in dirty) {
                if (observed.details.isPrivate) {
                    failures += "private_vulnerability_reporting: NOT_APPLICABLE to private repository"
                } else {
                    applyToggleEndpoint(
                        field = GovernanceField.PRIVATE_VULNERABILITY_REPORTING,
                        dirty = dirty,
                        desired = desired.privateVulnerabilityReportingEnabled,
                        observed = observed.privateVulnerabilityReportingEnabled,
                        enable = { api.enablePrivateVulnerabilityReporting(owner, repo) },
                        disable = { api.disablePrivateVulnerabilityReporting(owner, repo) },
                        succeeded = succeeded,
                        failures = failures,
                        label = "private_vulnerability_reporting"
                    )
                }
            }

            val actionsFields = dirty intersect ACTIONS_FIELDS
            if (actionsFields.isNotEmpty()) {
                val workflow = observed.workflowPermissions
                if (workflow?.defaultWorkflowPermissions == null || workflow.canApprovePullRequestReviews == null) {
                    failures += "actions_workflow_permissions: TOKEN_VAZIO pre-state; replacement blocked"
                } else {
                    runProviderResponse(
                        operation = {
                            api.updateActionsWorkflowPermissions(
                                owner,
                                repo,
                                UpdateActionsWorkflowPermissionsRequest(
                                    defaultWorkflowPermissions = if (desired.actionsReadOnlyDefault) "read" else "write",
                                    canApprovePullRequestReviews = desired.actionsCanApprovePullRequests
                                )
                            )
                        },
                        idempotentNotFound = false
                    ).fold(
                        onSuccess = { succeeded += actionsFields },
                        onFailure = { failures += "actions_workflow_permissions: ${it.message}" }
                    )
                }
            }

            if (GovernanceField.BRANCH_PROTECTION in dirty) {
                when {
                    observed.branchProtectionEnabled == null -> failures +=
                        "branch_protection: TOKEN_VAZIO pre-state; destructive replacement blocked"
                    desired.branchProtectionEnabled == observed.branchProtectionEnabled ->
                        succeeded += GovernanceField.BRANCH_PROTECTION
                    desired.branchProtectionEnabled && observed.branchProtectionEnabled == false -> {
                        runProviderResponse(
                            operation = {
                                api.updateBranchProtection(
                                    owner,
                                    repo,
                                    observed.details.defaultBranch,
                                    BranchProtectionRequest(requiredStatusChecks = null)
                                )
                            },
                            idempotentNotFound = false
                        ).fold(
                            onSuccess = { succeeded += GovernanceField.BRANCH_PROTECTION },
                            onFailure = { failures += "branch_protection: ${it.message}" }
                        )
                    }
                    !desired.branchProtectionEnabled && observed.branchProtectionEnabled == true -> {
                        runProviderResponse(
                            operation = { api.deleteBranchProtection(owner, repo, observed.details.defaultBranch) },
                            idempotentNotFound = true
                        ).fold(
                            onSuccess = { succeeded += GovernanceField.BRANCH_PROTECTION },
                            onFailure = { failures += "branch_protection: ${it.message}" }
                        )
                    }
                }
            }

            val outcome = when {
                failures.isEmpty() -> "PROVIDER_ACCEPTED"
                succeeded.isEmpty() -> "FAILED"
                else -> "PARTIAL"
            }
            val receipt = receiptStore.appendDetailed(
                repository = repository.fullName,
                operation = "repository_governance_apply",
                outcome = outcome,
                details = "dirty=${dirty.sortedBy { it.name }.joinToString(",")}; " +
                    "succeeded=${succeeded.sortedBy { it.name }.joinToString(",")}; " +
                    "failures=${failures.joinToString(" | ")}",
                beforeSnapshot = providerSnapshot(observed),
                afterSnapshot = "AUTHORITATIVE_REPROBE_PENDING",
                gaps = failures
            )

            _uiState.value = _uiState.value.copy(
                dirtyFields = dirty - succeeded,
                evidenceState = when {
                    failures.isEmpty() -> GovernanceEvidenceState.APPLIED
                    succeeded.isEmpty() -> GovernanceEvidenceState.FAILED
                    else -> GovernanceEvidenceState.TOKEN_VAZIO
                },
                message = if (failures.isEmpty()) {
                    "Provider accepted all staged operations. Re-probing authoritative state…"
                } else {
                    "${succeeded.size} field(s) accepted; unresolved: ${failures.joinToString(" | ")}"
                },
                lastReceiptId = receipt,
                receiptPath = receiptStore.path(),
                receiptChainStatus = receiptStore.verifyChain()
            )
            probe(
                repository = repository,
                preserveDirty = true,
                preserveMessage = failures.isNotEmpty(),
                writeAuditReceipt = true,
                auditOperation = "repository_governance_post_apply_reprobe"
            )
        }
    }

    private suspend fun applyToggleEndpoint(
        field: GovernanceField,
        dirty: Set<GovernanceField>,
        desired: Boolean,
        observed: Boolean?,
        enable: suspend () -> Response<Unit>,
        disable: suspend () -> Response<Unit>,
        succeeded: MutableSet<GovernanceField>,
        failures: MutableList<String>,
        label: String
    ) {
        if (field !in dirty) return
        if (observed == null) {
            failures += "$label: TOKEN_VAZIO pre-state; mutation blocked"
            return
        }
        if (desired == observed) {
            succeeded += field
            return
        }
        runProviderResponse(
            operation = if (desired) enable else disable,
            idempotentNotFound = !desired
        ).fold(
            onSuccess = { succeeded += field },
            onFailure = { failures += "$label: ${it.message}" }
        )
    }

    private suspend fun probe(
        repository: GovernanceRepositorySummary,
        preserveDirty: Boolean = false,
        preserveMessage: Boolean = false,
        writeAuditReceipt: Boolean = false,
        auditOperation: String = "repository_governance_probe"
    ) {
        val coordinates = repositoryCoordinates(repository.fullName)
        if (coordinates == null) {
            _uiState.value = _uiState.value.copy(
                isLoadingRepository = false,
                isAuditing = false,
                evidenceState = GovernanceEvidenceState.FAILED,
                message = "Invalid repository identity: ${repository.fullName}"
            )
            return
        }
        val (owner, repo) = coordinates
        runCatching {
            val details = api.getRepository(owner, repo)
            val adminProven = repository.permissions?.admin == true || details.permissions?.admin == true
            val protectionResponse = runCatching { api.getBranchProtection(owner, repo, details.defaultBranch) }.getOrNull()
            val vulnerability = runCatching { api.checkVulnerabilityAlerts(owner, repo) }.getOrNull()
            val automatedFixes = runCatching { api.checkAutomatedSecurityFixes(owner, repo) }.getOrNull()
            val rulesetsResponse = runCatching { api.listRulesets(owner, repo) }.getOrNull()
            val actionsResponse = runCatching { api.getActionsPermissions(owner, repo) }.getOrNull()
            val workflowResponse = runCatching { api.getActionsWorkflowPermissions(owner, repo) }.getOrNull()
            val privateVulnerabilityResponse = if (!details.isPrivate) {
                runCatching { api.checkPrivateVulnerabilityReporting(owner, repo) }.getOrNull()
            } else null

            ObservedRepositoryGovernance(
                details = details,
                branchProtectionEnabled = responseBoolean(protectionResponse, adminProven),
                branchProtection = protectionResponse?.takeIf { it.isSuccessful }?.body(),
                rulesets = rulesetsResponse?.takeIf { it.isSuccessful }?.body(),
                actionsPermissions = actionsResponse?.takeIf { it.isSuccessful }?.body(),
                workflowPermissions = workflowResponse?.takeIf { it.isSuccessful }?.body(),
                vulnerabilityAlertsEnabled = responseBoolean(vulnerability, adminProven),
                automatedSecurityFixesEnabled = responseBoolean(automatedFixes, adminProven),
                privateVulnerabilityReportingEnabled = if (details.isPrivate) null else responseBoolean(privateVulnerabilityResponse, adminProven),
                advancedSecurityEnabled = details.securityAndAnalysis?.advancedSecurity?.isEnabledOrNull(),
                secretScanningEnabled = details.securityAndAnalysis?.secretScanning?.isEnabledOrNull(),
                secretScanningPushProtectionEnabled = details.securityAndAnalysis?.secretScanningPushProtection?.isEnabledOrNull()
            )
        }.onSuccess { observed ->
            val chainBefore = receiptStore.verifyChain()
            val audit = RepositoryGovernanceAuditor.evaluate(
                GovernanceAuditInput(
                    repository = observed.details,
                    branchProtectionEnabled = observed.branchProtectionEnabled,
                    branchProtection = observed.branchProtection,
                    rulesets = observed.rulesets,
                    actionsPermissions = observed.actionsPermissions,
                    workflowPermissions = observed.workflowPermissions,
                    vulnerabilityAlertsEnabled = observed.vulnerabilityAlertsEnabled,
                    automatedSecurityFixesEnabled = observed.automatedSecurityFixesEnabled,
                    privateVulnerabilityReportingEnabled = observed.privateVulnerabilityReportingEnabled,
                    appendOnlyChainValid = chainBefore.valid
                )
            )
            val previous = _uiState.value
            var auditReceipt: String? = null
            if (writeAuditReceipt) {
                val gaps = audit.controls
                    .filter { it.state == GovernanceControlState.FAIL || it.state == GovernanceControlState.TOKEN_VAZIO }
                    .map { "${it.id}:${it.state.name}" }
                auditReceipt = receiptStore.appendDetailed(
                    repository = repository.fullName,
                    operation = auditOperation,
                    outcome = if (audit.failCount == 0 && audit.gapCount == 0) "AUDIT_PASS" else "AUDIT_OPEN_GAPS",
                    details = "profile=${audit.profileId}; score=${audit.scorePercent}; pass=${audit.passCount}; fail=${audit.failCount}; token_vazio=${audit.gapCount}; na=${audit.notApplicableCount}",
                    afterSnapshot = providerSnapshot(observed),
                    gaps = gaps
                )
            }
            val chainAfter = receiptStore.verifyChain()
            _uiState.value = previous.copy(
                observed = observed,
                desired = if (preserveDirty && previous.desired != null) previous.desired else desiredFromObserved(observed),
                dirtyFields = if (preserveDirty) previous.dirtyFields else emptySet(),
                auditReport = audit,
                receiptChainStatus = chainAfter,
                isLoadingRepository = false,
                isAuditing = false,
                evidenceState = if (preserveDirty && previous.dirtyFields.isNotEmpty()) {
                    GovernanceEvidenceState.READY
                } else GovernanceEvidenceState.OBSERVED,
                message = if (preserveMessage) previous.message else evidenceSummary(observed, audit),
                lastReceiptId = auditReceipt ?: previous.lastReceiptId,
                receiptPath = if (auditReceipt != null || previous.receiptPath != null) receiptStore.path() else null
            )
        }.onFailure { error ->
            val receipt = if (writeAuditReceipt) {
                receiptStore.appendDetailed(
                    repository = repository.fullName,
                    operation = auditOperation,
                    outcome = "TOKEN_VAZIO",
                    details = "Provider audit probe failed: ${providerMessage(error)}",
                    gaps = listOf("PROVIDER_PROBE_FAILED")
                )
            } else null
            _uiState.value = _uiState.value.copy(
                isLoadingRepository = false,
                isAuditing = false,
                evidenceState = GovernanceEvidenceState.TOKEN_VAZIO,
                message = "TOKEN_VAZIO: provider probe failed (${providerMessage(error)}).",
                lastReceiptId = receipt ?: _uiState.value.lastReceiptId,
                receiptPath = if (receipt != null) receiptStore.path() else _uiState.value.receiptPath,
                receiptChainStatus = receiptStore.verifyChain()
            )
        }
    }

    private fun DesiredRepositoryGovernance.withField(
        field: GovernanceField,
        enabled: Boolean
    ): DesiredRepositoryGovernance = when (field) {
        GovernanceField.HAS_ISSUES -> copy(hasIssues = enabled)
        GovernanceField.HAS_PROJECTS -> copy(hasProjects = enabled)
        GovernanceField.HAS_WIKI -> copy(hasWiki = enabled)
        GovernanceField.HAS_DISCUSSIONS -> copy(hasDiscussions = enabled)
        GovernanceField.ALLOW_MERGE_COMMIT -> copy(allowMergeCommit = enabled)
        GovernanceField.ALLOW_SQUASH_MERGE -> copy(allowSquashMerge = enabled)
        GovernanceField.ALLOW_REBASE_MERGE -> copy(allowRebaseMerge = enabled)
        GovernanceField.ALLOW_AUTO_MERGE -> copy(allowAutoMerge = enabled)
        GovernanceField.ALLOW_UPDATE_BRANCH -> copy(allowUpdateBranch = enabled)
        GovernanceField.DELETE_BRANCH_ON_MERGE -> copy(deleteBranchOnMerge = enabled)
        GovernanceField.WEB_COMMIT_SIGNOFF_REQUIRED -> copy(webCommitSignoffRequired = enabled)
        GovernanceField.BRANCH_PROTECTION -> copy(branchProtectionEnabled = enabled)
        GovernanceField.VULNERABILITY_ALERTS -> copy(vulnerabilityAlertsEnabled = enabled)
        GovernanceField.AUTOMATED_SECURITY_FIXES -> copy(automatedSecurityFixesEnabled = enabled)
        GovernanceField.PRIVATE_VULNERABILITY_REPORTING -> copy(privateVulnerabilityReportingEnabled = enabled)
        GovernanceField.ADVANCED_SECURITY -> copy(advancedSecurityEnabled = enabled)
        GovernanceField.SECRET_SCANNING -> copy(secretScanningEnabled = enabled)
        GovernanceField.SECRET_SCANNING_PUSH_PROTECTION -> copy(secretScanningPushProtectionEnabled = enabled)
        GovernanceField.ACTIONS_READ_ONLY_DEFAULT -> copy(actionsReadOnlyDefault = enabled)
        GovernanceField.ACTIONS_CAN_APPROVE_PULL_REQUESTS -> copy(actionsCanApprovePullRequests = enabled)
    }

    private fun observedValue(field: GovernanceField, observed: ObservedRepositoryGovernance): Boolean? = when (field) {
        GovernanceField.HAS_ISSUES -> observed.details.hasIssues
        GovernanceField.HAS_PROJECTS -> observed.details.hasProjects
        GovernanceField.HAS_WIKI -> observed.details.hasWiki
        GovernanceField.HAS_DISCUSSIONS -> observed.details.hasDiscussions
        GovernanceField.ALLOW_MERGE_COMMIT -> observed.details.allowMergeCommit
        GovernanceField.ALLOW_SQUASH_MERGE -> observed.details.allowSquashMerge
        GovernanceField.ALLOW_REBASE_MERGE -> observed.details.allowRebaseMerge
        GovernanceField.ALLOW_AUTO_MERGE -> observed.details.allowAutoMerge
        GovernanceField.ALLOW_UPDATE_BRANCH -> observed.details.allowUpdateBranch
        GovernanceField.DELETE_BRANCH_ON_MERGE -> observed.details.deleteBranchOnMerge
        GovernanceField.WEB_COMMIT_SIGNOFF_REQUIRED -> observed.details.webCommitSignoffRequired
        GovernanceField.BRANCH_PROTECTION -> observed.branchProtectionEnabled
        GovernanceField.VULNERABILITY_ALERTS -> observed.vulnerabilityAlertsEnabled
        GovernanceField.AUTOMATED_SECURITY_FIXES -> observed.automatedSecurityFixesEnabled
        GovernanceField.PRIVATE_VULNERABILITY_REPORTING -> observed.privateVulnerabilityReportingEnabled
        GovernanceField.ADVANCED_SECURITY -> observed.advancedSecurityEnabled
        GovernanceField.SECRET_SCANNING -> observed.secretScanningEnabled
        GovernanceField.SECRET_SCANNING_PUSH_PROTECTION -> observed.secretScanningPushProtectionEnabled
        GovernanceField.ACTIONS_READ_ONLY_DEFAULT -> observed.workflowPermissions?.defaultWorkflowPermissions?.let { it == "read" }
        GovernanceField.ACTIONS_CAN_APPROVE_PULL_REQUESTS -> observed.workflowPermissions?.canApprovePullRequestReviews
    }

    private fun matchesObserved(
        field: GovernanceField,
        enabled: Boolean,
        observed: ObservedRepositoryGovernance
    ): Boolean = observedValue(field, observed)?.let { it == enabled } == true

    private fun desiredFromObserved(observed: ObservedRepositoryGovernance): DesiredRepositoryGovernance {
        val details = observed.details
        return DesiredRepositoryGovernance(
            hasIssues = details.hasIssues,
            hasProjects = details.hasProjects,
            hasWiki = details.hasWiki,
            hasDiscussions = details.hasDiscussions,
            allowMergeCommit = details.allowMergeCommit,
            allowSquashMerge = details.allowSquashMerge,
            allowRebaseMerge = details.allowRebaseMerge,
            allowAutoMerge = details.allowAutoMerge,
            allowUpdateBranch = details.allowUpdateBranch,
            deleteBranchOnMerge = details.deleteBranchOnMerge,
            webCommitSignoffRequired = details.webCommitSignoffRequired,
            branchProtectionEnabled = observed.branchProtectionEnabled ?: false,
            vulnerabilityAlertsEnabled = observed.vulnerabilityAlertsEnabled ?: false,
            automatedSecurityFixesEnabled = observed.automatedSecurityFixesEnabled ?: false,
            privateVulnerabilityReportingEnabled = observed.privateVulnerabilityReportingEnabled ?: false,
            advancedSecurityEnabled = observed.advancedSecurityEnabled ?: false,
            secretScanningEnabled = observed.secretScanningEnabled ?: false,
            secretScanningPushProtectionEnabled = observed.secretScanningPushProtectionEnabled ?: false,
            actionsReadOnlyDefault = observed.workflowPermissions?.defaultWorkflowPermissions == "read",
            actionsCanApprovePullRequests = observed.workflowPermissions?.canApprovePullRequestReviews ?: false
        )
    }

    private fun structuralRequest(
        desired: DesiredRepositoryGovernance,
        fields: Set<GovernanceField>
    ) = UpdateRepositoryGovernanceRequest(
        hasIssues = desired.hasIssues.takeIf { GovernanceField.HAS_ISSUES in fields },
        hasProjects = desired.hasProjects.takeIf { GovernanceField.HAS_PROJECTS in fields },
        hasWiki = desired.hasWiki.takeIf { GovernanceField.HAS_WIKI in fields },
        hasDiscussions = desired.hasDiscussions.takeIf { GovernanceField.HAS_DISCUSSIONS in fields },
        allowMergeCommit = desired.allowMergeCommit.takeIf { GovernanceField.ALLOW_MERGE_COMMIT in fields },
        allowSquashMerge = desired.allowSquashMerge.takeIf { GovernanceField.ALLOW_SQUASH_MERGE in fields },
        allowRebaseMerge = desired.allowRebaseMerge.takeIf { GovernanceField.ALLOW_REBASE_MERGE in fields },
        allowAutoMerge = desired.allowAutoMerge.takeIf { GovernanceField.ALLOW_AUTO_MERGE in fields },
        allowUpdateBranch = desired.allowUpdateBranch.takeIf { GovernanceField.ALLOW_UPDATE_BRANCH in fields },
        deleteBranchOnMerge = desired.deleteBranchOnMerge.takeIf { GovernanceField.DELETE_BRANCH_ON_MERGE in fields },
        webCommitSignoffRequired = desired.webCommitSignoffRequired.takeIf { GovernanceField.WEB_COMMIT_SIGNOFF_REQUIRED in fields }
    )

    private fun securityAnalysisRequest(
        desired: DesiredRepositoryGovernance,
        fields: Set<GovernanceField>
    ) = UpdateRepositoryGovernanceRequest(
        securityAndAnalysis = UpdateGovernanceSecurityAndAnalysis(
            advancedSecurity = status(desired.advancedSecurityEnabled).takeIf { GovernanceField.ADVANCED_SECURITY in fields },
            secretScanning = status(desired.secretScanningEnabled).takeIf { GovernanceField.SECRET_SCANNING in fields },
            secretScanningPushProtection = status(desired.secretScanningPushProtectionEnabled)
                .takeIf { GovernanceField.SECRET_SCANNING_PUSH_PROTECTION in fields }
        )
    )

    private fun status(enabled: Boolean) = GovernanceFeatureStatus(if (enabled) "enabled" else "disabled")

    private suspend fun <T> runProviderResponse(
        operation: suspend () -> Response<T>,
        idempotentNotFound: Boolean
    ): Result<Unit> = runCatching {
        val response = operation()
        if (response.isSuccessful || (idempotentNotFound && response.code() == 404)) return@runCatching Unit
        error("HTTP ${response.code()} ${response.message()}")
    }

    private fun responseBoolean(response: Response<*>?, adminProven: Boolean): Boolean? = when {
        response == null -> null
        response.isSuccessful -> true
        response.code() == 404 && adminProven -> false
        else -> null
    }

    private fun evidenceSummary(
        observed: ObservedRepositoryGovernance,
        audit: GovernanceAuditReport
    ): String {
        val gaps = buildList {
            if (observed.branchProtectionEnabled == null) add("branch protection")
            if (observed.rulesets == null) add("rulesets")
            if (observed.actionsPermissions == null) add("Actions policy")
            if (observed.workflowPermissions == null) add("Actions workflow permissions")
            if (observed.vulnerabilityAlertsEnabled == null) add("vulnerability alerts")
            if (observed.automatedSecurityFixesEnabled == null) add("automated security fixes")
            if (!observed.details.isPrivate && observed.privateVulnerabilityReportingEnabled == null) add("private vulnerability reporting")
            if (observed.advancedSecurityEnabled == null) add("advanced security")
            if (observed.secretScanningEnabled == null) add("secret scanning")
            if (observed.secretScanningPushProtectionEnabled == null) add("push protection")
        }
        return buildString {
            append("Audit ${audit.scorePercent}%: ${audit.passCount} PASS, ${audit.failCount} FAIL, ${audit.gapCount} TOKEN_VAZIO.")
            if (gaps.isNotEmpty()) append(" Provider gaps: ${gaps.joinToString(", ")}.")
        }
    }

    private fun providerSnapshot(observed: ObservedRepositoryGovernance): String = buildString {
        append("repo=").append(observed.details.fullName)
        append("; default_branch=").append(observed.details.defaultBranch)
        append("; branch_protection=").append(observed.branchProtectionEnabled ?: "TOKEN_VAZIO")
        append("; rulesets=").append(observed.rulesets?.size ?: "TOKEN_VAZIO")
        append("; vulnerability_alerts=").append(observed.vulnerabilityAlertsEnabled ?: "TOKEN_VAZIO")
        append("; automated_security_fixes=").append(observed.automatedSecurityFixesEnabled ?: "TOKEN_VAZIO")
        append("; private_vulnerability_reporting=").append(observed.privateVulnerabilityReportingEnabled ?: if (observed.details.isPrivate) "NOT_APPLICABLE" else "TOKEN_VAZIO")
        append("; advanced_security=").append(observed.advancedSecurityEnabled ?: "TOKEN_VAZIO")
        append("; secret_scanning=").append(observed.secretScanningEnabled ?: "TOKEN_VAZIO")
        append("; push_protection=").append(observed.secretScanningPushProtectionEnabled ?: "TOKEN_VAZIO")
        append("; actions_default=").append(observed.workflowPermissions?.defaultWorkflowPermissions ?: "TOKEN_VAZIO")
        append("; actions_can_approve_pr=").append(observed.workflowPermissions?.canApprovePullRequestReviews ?: "TOKEN_VAZIO")
    }

    private fun repositoryCoordinates(fullName: String): Pair<String, String>? {
        val slash = fullName.indexOf('/')
        if (slash <= 0 || slash == fullName.lastIndex) return null
        return fullName.substring(0, slash) to fullName.substring(slash + 1)
    }

    private fun providerMessage(error: Throwable): String =
        error.message?.take(240)?.ifBlank { error::class.java.simpleName }
            ?: error::class.java.simpleName

    companion object {
        private const val PAGE_SIZE = 100
        private const val MAX_REPOSITORY_PAGES = 100

        private val STRUCTURAL_FIELDS = setOf(
            GovernanceField.HAS_ISSUES,
            GovernanceField.HAS_PROJECTS,
            GovernanceField.HAS_WIKI,
            GovernanceField.HAS_DISCUSSIONS,
            GovernanceField.ALLOW_MERGE_COMMIT,
            GovernanceField.ALLOW_SQUASH_MERGE,
            GovernanceField.ALLOW_REBASE_MERGE,
            GovernanceField.ALLOW_AUTO_MERGE,
            GovernanceField.ALLOW_UPDATE_BRANCH,
            GovernanceField.DELETE_BRANCH_ON_MERGE,
            GovernanceField.WEB_COMMIT_SIGNOFF_REQUIRED
        )

        private val ANALYSIS_FIELDS = setOf(
            GovernanceField.ADVANCED_SECURITY,
            GovernanceField.SECRET_SCANNING,
            GovernanceField.SECRET_SCANNING_PUSH_PROTECTION
        )

        private val ACTIONS_FIELDS = setOf(
            GovernanceField.ACTIONS_READ_ONLY_DEFAULT,
            GovernanceField.ACTIONS_CAN_APPROVE_PULL_REQUESTS
        )
    }
}
