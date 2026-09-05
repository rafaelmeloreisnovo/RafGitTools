package com.rafgittools.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.github.BranchProtectionRequest
import com.rafgittools.data.github.GovernanceFeatureStatus
import com.rafgittools.data.github.GovernanceRepositoryDetails
import com.rafgittools.data.github.GovernanceRepositorySummary
import com.rafgittools.data.github.RepositoryGovernanceApiService
import com.rafgittools.data.github.RepositoryGovernanceReceiptStore
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
    DELETE_BRANCH_ON_MERGE,
    WEB_COMMIT_SIGNOFF_REQUIRED,
    BRANCH_PROTECTION,
    VULNERABILITY_ALERTS,
    AUTOMATED_SECURITY_FIXES,
    ADVANCED_SECURITY,
    SECRET_SCANNING,
    SECRET_SCANNING_PUSH_PROTECTION
}

data class ObservedRepositoryGovernance(
    val details: GovernanceRepositoryDetails,
    val branchProtectionEnabled: Boolean?,
    val vulnerabilityAlertsEnabled: Boolean?,
    val automatedSecurityFixesEnabled: Boolean?,
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
    val deleteBranchOnMerge: Boolean,
    val webCommitSignoffRequired: Boolean,
    val branchProtectionEnabled: Boolean,
    val vulnerabilityAlertsEnabled: Boolean,
    val automatedSecurityFixesEnabled: Boolean,
    val advancedSecurityEnabled: Boolean,
    val secretScanningEnabled: Boolean,
    val secretScanningPushProtectionEnabled: Boolean
)

data class RepositoryGovernanceUiState(
    val repositories: List<GovernanceRepositorySummary> = emptyList(),
    val selectedRepository: GovernanceRepositorySummary? = null,
    val observed: ObservedRepositoryGovernance? = null,
    val desired: DesiredRepositoryGovernance? = null,
    val dirtyFields: Set<GovernanceField> = emptySet(),
    val evidenceState: GovernanceEvidenceState = GovernanceEvidenceState.TOKEN_VAZIO,
    val isLoadingRepositories: Boolean = false,
    val isLoadingRepository: Boolean = false,
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
                "TOKEN_VAZIO: ${field.name} provider pre-state is not proven; apply will remain blocked for destructive replacement paths."
            } else null
        )
    }

    /**
     * Stages only changes that are known to be additive/non-destructive relative to observed state.
     * Existing branch protection is never PUT again because GitHub's branch-protection endpoint is
     * replacement-shaped; rewriting it without a full fidelity model could erase checks/restrictions.
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
        stage(GovernanceField.VULNERABILITY_ALERTS, true)
        stage(GovernanceField.AUTOMATED_SECURITY_FIXES, true)

        when (observed.branchProtectionEnabled) {
            false -> stage(GovernanceField.BRANCH_PROTECTION, true)
            true -> Unit // preserve the exact existing provider policy
            null -> gaps += "branch protection"
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

        _uiState.value = state.copy(
            desired = desired,
            dirtyFields = dirty,
            evidenceState = if (dirty.isEmpty()) GovernanceEvidenceState.OBSERVED else GovernanceEvidenceState.READY,
            message = buildString {
                append("Recommended baseline staged without rewriting existing branch-protection policy.")
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
            val receipt = receiptStore.append(
                repository.fullName,
                "repository_governance_apply",
                "TOKEN_VAZIO",
                "Provider inventory/details did not prove admin permission; no mutation attempted."
            )
            _uiState.value = state.copy(
                evidenceState = GovernanceEvidenceState.TOKEN_VAZIO,
                message = "TOKEN_VAZIO: admin authority is required for repository governance writes.",
                lastReceiptId = receipt,
                receiptPath = receiptStore.path()
            )
            return
        }
        if (repository.archived) {
            _uiState.value = state.copy(
                evidenceState = GovernanceEvidenceState.FAILED,
                message = "Archived repositories are read-only; no mutation attempted."
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
                                    BranchProtectionRequest()
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
                failures.isEmpty() -> "APPLIED"
                succeeded.isEmpty() -> "FAILED"
                else -> "PARTIAL"
            }
            val receipt = receiptStore.append(
                repository.fullName,
                "repository_governance_apply",
                outcome,
                "dirty=${dirty.sortedBy { it.name }.joinToString(",")}; " +
                    "succeeded=${succeeded.sortedBy { it.name }.joinToString(",")}; " +
                    "failures=${failures.joinToString(" | ")}; " +
                    "prestate_branch_protection=${observed.branchProtectionEnabled ?: "TOKEN_VAZIO"}"
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
                receiptPath = receiptStore.path()
            )
            probe(repository, preserveDirty = true, preserveMessage = failures.isNotEmpty())
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
        preserveMessage: Boolean = false
    ) {
        val coordinates = repositoryCoordinates(repository.fullName)
        if (coordinates == null) {
            _uiState.value = _uiState.value.copy(
                isLoadingRepository = false,
                evidenceState = GovernanceEvidenceState.FAILED,
                message = "Invalid repository identity: ${repository.fullName}"
            )
            return
        }
        val (owner, repo) = coordinates
        runCatching {
            val details = api.getRepository(owner, repo)
            val adminProven = repository.permissions?.admin == true || details.permissions?.admin == true
            val protection = runCatching { api.getBranchProtection(owner, repo, details.defaultBranch) }.getOrNull()
            val vulnerability = runCatching { api.checkVulnerabilityAlerts(owner, repo) }.getOrNull()
            val automatedFixes = runCatching { api.checkAutomatedSecurityFixes(owner, repo) }.getOrNull()

            ObservedRepositoryGovernance(
                details = details,
                branchProtectionEnabled = responseBoolean(protection, adminProven),
                vulnerabilityAlertsEnabled = responseBoolean(vulnerability, adminProven),
                automatedSecurityFixesEnabled = responseBoolean(automatedFixes, adminProven),
                advancedSecurityEnabled = details.securityAndAnalysis?.advancedSecurity?.isEnabledOrNull(),
                secretScanningEnabled = details.securityAndAnalysis?.secretScanning?.isEnabledOrNull(),
                secretScanningPushProtectionEnabled = details.securityAndAnalysis?.secretScanningPushProtection?.isEnabledOrNull()
            )
        }.onSuccess { observed ->
            val previous = _uiState.value
            _uiState.value = previous.copy(
                observed = observed,
                desired = if (preserveDirty && previous.desired != null) previous.desired else desiredFromObserved(observed),
                dirtyFields = if (preserveDirty) previous.dirtyFields else emptySet(),
                isLoadingRepository = false,
                evidenceState = if (preserveDirty && previous.dirtyFields.isNotEmpty()) {
                    GovernanceEvidenceState.READY
                } else GovernanceEvidenceState.OBSERVED,
                message = if (preserveMessage) previous.message else evidenceSummary(observed)
            )
        }.onFailure { error ->
            _uiState.value = _uiState.value.copy(
                isLoadingRepository = false,
                evidenceState = GovernanceEvidenceState.TOKEN_VAZIO,
                message = "TOKEN_VAZIO: provider probe failed (${providerMessage(error)})."
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
        GovernanceField.DELETE_BRANCH_ON_MERGE -> copy(deleteBranchOnMerge = enabled)
        GovernanceField.WEB_COMMIT_SIGNOFF_REQUIRED -> copy(webCommitSignoffRequired = enabled)
        GovernanceField.BRANCH_PROTECTION -> copy(branchProtectionEnabled = enabled)
        GovernanceField.VULNERABILITY_ALERTS -> copy(vulnerabilityAlertsEnabled = enabled)
        GovernanceField.AUTOMATED_SECURITY_FIXES -> copy(automatedSecurityFixesEnabled = enabled)
        GovernanceField.ADVANCED_SECURITY -> copy(advancedSecurityEnabled = enabled)
        GovernanceField.SECRET_SCANNING -> copy(secretScanningEnabled = enabled)
        GovernanceField.SECRET_SCANNING_PUSH_PROTECTION -> copy(secretScanningPushProtectionEnabled = enabled)
    }

    private fun observedValue(field: GovernanceField, observed: ObservedRepositoryGovernance): Boolean? = when (field) {
        GovernanceField.HAS_ISSUES -> observed.details.hasIssues
        GovernanceField.HAS_PROJECTS -> observed.details.hasProjects
        GovernanceField.HAS_WIKI -> observed.details.hasWiki
        GovernanceField.HAS_DISCUSSIONS -> observed.details.hasDiscussions
        GovernanceField.ALLOW_MERGE_COMMIT -> observed.details.allowMergeCommit
        GovernanceField.ALLOW_SQUASH_MERGE -> observed.details.allowSquashMerge
        GovernanceField.ALLOW_REBASE_MERGE -> observed.details.allowRebaseMerge
        GovernanceField.DELETE_BRANCH_ON_MERGE -> observed.details.deleteBranchOnMerge
        GovernanceField.WEB_COMMIT_SIGNOFF_REQUIRED -> observed.details.webCommitSignoffRequired
        GovernanceField.BRANCH_PROTECTION -> observed.branchProtectionEnabled
        GovernanceField.VULNERABILITY_ALERTS -> observed.vulnerabilityAlertsEnabled
        GovernanceField.AUTOMATED_SECURITY_FIXES -> observed.automatedSecurityFixesEnabled
        GovernanceField.ADVANCED_SECURITY -> observed.advancedSecurityEnabled
        GovernanceField.SECRET_SCANNING -> observed.secretScanningEnabled
        GovernanceField.SECRET_SCANNING_PUSH_PROTECTION -> observed.secretScanningPushProtectionEnabled
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
            deleteBranchOnMerge = details.deleteBranchOnMerge,
            webCommitSignoffRequired = details.webCommitSignoffRequired,
            branchProtectionEnabled = observed.branchProtectionEnabled ?: false,
            vulnerabilityAlertsEnabled = observed.vulnerabilityAlertsEnabled ?: false,
            automatedSecurityFixesEnabled = observed.automatedSecurityFixesEnabled ?: false,
            advancedSecurityEnabled = observed.advancedSecurityEnabled ?: false,
            secretScanningEnabled = observed.secretScanningEnabled ?: false,
            secretScanningPushProtectionEnabled = observed.secretScanningPushProtectionEnabled ?: false
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

    private fun evidenceSummary(observed: ObservedRepositoryGovernance): String {
        val gaps = buildList {
            if (observed.branchProtectionEnabled == null) add("branch protection")
            if (observed.vulnerabilityAlertsEnabled == null) add("vulnerability alerts")
            if (observed.automatedSecurityFixesEnabled == null) add("automated security fixes")
            if (observed.advancedSecurityEnabled == null) add("advanced security")
            if (observed.secretScanningEnabled == null) add("secret scanning")
            if (observed.secretScanningPushProtectionEnabled == null) add("push protection")
        }
        return if (gaps.isEmpty()) {
            "Provider state observed. No staged mutation."
        } else {
            "Provider state partially observed; TOKEN_VAZIO: ${gaps.joinToString(", ")}."
        }
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
            GovernanceField.DELETE_BRANCH_ON_MERGE,
            GovernanceField.WEB_COMMIT_SIGNOFF_REQUIRED
        )

        private val ANALYSIS_FIELDS = setOf(
            GovernanceField.ADVANCED_SECURITY,
            GovernanceField.SECRET_SCANNING,
            GovernanceField.SECRET_SCANNING_PUSH_PROTECTION
        )
    }
}
