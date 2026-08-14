package com.rafgittools.ui.screens.rafgitfs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.rafgitfs.data.StagedOperationDao
import com.rafgittools.rafgitfs.data.StagedOperationEntity
import com.rafgittools.rafgitfs.data.SyncConflictDao
import com.rafgittools.rafgitfs.data.SyncConflictEntity
import com.rafgittools.rafgitfs.data.WorkspaceEntity
import com.rafgittools.rafgitfs.remote.RafGitFsGithubApiService
import com.rafgittools.rafgitfs.sync.RafGitFsApproval
import com.rafgittools.rafgitfs.sync.RafGitFsExecutionOutcome
import com.rafgittools.rafgitfs.sync.RafGitFsGovernedSyncEngine
import com.rafgittools.rafgitfs.sync.RafGitFsObservedFile
import com.rafgittools.rafgitfs.sync.RafGitFsPlannedAction
import com.rafgittools.rafgitfs.sync.RafGitFsSyncPlan
import com.rafgittools.rafgitfs.write.RafGitFsGithubBranchWriter
import com.rafgittools.rafgitfs.write.RafGitFsWorkspaceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkspaceEditorUiState(
    val workspace: WorkspaceEntity? = null,
    val path: String = "",
    val content: String = "",
    val baseBlobSha: String = "",
    val stagedFiles: List<StagedOperationEntity> = emptyList(),
    val conflicts: List<SyncConflictEntity> = emptyList(),
    val plan: RafGitFsSyncPlan? = null,
    val dryRun: List<RafGitFsExecutionOutcome> = emptyList(),
    val approvalText: String = "",
    val rollbackText: String = "",
    val busy: Boolean = false,
    val status: RafGitFsUiStatus = RafGitFsUiStatus(
        RafGitFsUiEvidence.LOADING,
        "Creating private workspace"
    )
) {
    val expectedApproval: String?
        get() = plan?.let { "APPROVE ${it.planHash.take(12)}" }
    val expectedRollback: String?
        get() = plan?.let { "ROLLBACK ${it.planHash.take(12)}" }
}

@HiltViewModel
class WorkspaceEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workspaceStore: RafGitFsWorkspaceStore,
    private val stagedDao: StagedOperationDao,
    private val conflictDao: SyncConflictDao,
    private val readApi: RafGitFsGithubApiService,
    private val syncEngine: RafGitFsGovernedSyncEngine,
    private val branchWriter: RafGitFsGithubBranchWriter
) : ViewModel() {
    val profileId: String = decode(savedStateHandle.get<String>("profileId"))
    val repositoryFullName: String = decode(savedStateHandle.get<String>("repositoryFullName"))
    val refName: String = decode(savedStateHandle.get<String>("refName"))

    private val _state = MutableStateFlow(WorkspaceEditorUiState())
    val state: StateFlow<WorkspaceEditorUiState> = _state.asStateFlow()

    init {
        createWorkspace()
    }

    fun setPath(value: String) = update { copy(path = value) }
    fun setContent(value: String) = update { copy(content = value) }
    fun setBaseBlobSha(value: String) = update { copy(baseBlobSha = value.trim()) }
    fun setApprovalText(value: String) = update { copy(approvalText = value) }
    fun setRollbackText(value: String) = update { copy(rollbackText = value) }

    fun stageFile() {
        val workspace = _state.value.workspace ?: return setGap("WORKSPACE_NOT_READY")
        val current = _state.value
        if (current.path.isBlank()) return setGap("PATH_REQUIRED")
        viewModelScope.launch {
            setBusy("Writing private staged file")
            runCatching {
                workspaceStore.stageText(
                    workspace.workspaceId,
                    current.path,
                    current.content,
                    current.baseBlobSha.ifBlank { null }
                )
            }.onSuccess {
                refreshWorkspaceLists(workspace.workspaceId)
                _state.value = _state.value.copy(
                    path = "",
                    content = "",
                    baseBlobSha = "",
                    plan = null,
                    dryRun = emptyList(),
                    approvalText = "",
                    status = RafGitFsUiStatus(
                        RafGitFsUiEvidence.OBSERVED,
                        "File staged locally",
                        "No GitHub write occurred"
                    ),
                    busy = false
                )
            }.onFailure { error ->
                setError("STAGE_FAILED", error.message)
            }
        }
    }

    fun removeStaged(operationId: String) {
        val workspaceId = _state.value.workspace?.workspaceId ?: return
        viewModelScope.launch {
            val removed = workspaceStore.rollbackFile(workspaceId, operationId)
            refreshWorkspaceLists(workspaceId)
            _state.value = _state.value.copy(
                plan = null,
                dryRun = emptyList(),
                approvalText = "",
                busy = false,
                status = RafGitFsUiStatus(
                    if (removed) RafGitFsUiEvidence.OBSERVED else RafGitFsUiEvidence.TOKEN_VAZIO,
                    if (removed) "Staged file removed" else "TOKEN_VAZIO",
                    if (removed) "Local rollback only" else "STAGED_OPERATION_NOT_FOUND"
                )
            )
        }
    }

    fun preparePlan() {
        val workspace = _state.value.workspace ?: return setGap("WORKSPACE_NOT_READY")
        viewModelScope.launch {
            setBusy("Resolving base ref and building dry-run")
            val repo = splitRepo(repositoryFullName) ?: return@launch setError("INVALID_REPOSITORY", repositoryFullName)
            val baseResponse = runCatching { readApi.resolveCommit(repo.first, repo.second, refName) }
                .getOrElse { return@launch setError("BASE_REF_READ_FAILED", it.message) }
            val base = baseResponse.body()
            if (!baseResponse.isSuccessful || base == null) {
                return@launch setGap("BASE_REF_UNOBSERVED:HTTP_${baseResponse.code()}")
            }
            val staged = stagedDao.listForWorkspace(workspace.workspaceId)
                .filter { it.state == "STAGED" && it.operationType.startsWith("UPSERT_FILE:") }
            if (staged.isEmpty()) return@launch setGap("NO_STAGED_FILES")
            val observed = staged.map { operation ->
                val baseBlob = operation.baseSha
                RafGitFsObservedFile(
                    path = operation.path.orEmpty(),
                    remoteSha = baseBlob,
                    localSha = operation.localSha,
                    localExists = true,
                    remoteExists = baseBlob != null,
                    baseSha = baseBlob
                )
            }
            val plan = syncEngine.createPlan(
                profileId = profileId,
                repositoryFullName = repositoryFullName,
                refName = refName,
                baseCommitSha = base.sha,
                observed = observed,
                requestedAction = RafGitFsPlannedAction.OPEN_PULL_REQUEST,
                workspaceId = workspace.workspaceId
            )
            val dryRun = syncEngine.dryRun(plan)
            refreshWorkspaceLists(workspace.workspaceId)
            _state.value = _state.value.copy(
                plan = plan,
                dryRun = dryRun,
                approvalText = "",
                rollbackText = "",
                busy = false,
                status = RafGitFsUiStatus(
                    if (plan.conflicts.isEmpty()) RafGitFsUiEvidence.OBSERVED else RafGitFsUiEvidence.TOKEN_VAZIO,
                    if (plan.conflicts.isEmpty()) "Dry-run ready" else "Conflicts block publication",
                    "plan ${plan.planHash.take(12)} · ${plan.steps.size} governed steps"
                )
            )
        }
    }

    fun approveAndPublish() {
        val plan = _state.value.plan ?: return setGap("PLAN_REQUIRED")
        val confirmation = _state.value.approvalText
        val expected = _state.value.expectedApproval
        if (confirmation != expected) return setGap("APPROVAL_CONFIRMATION_MISMATCH")
        viewModelScope.launch {
            setBusy("Executing approved branch and draft PR sequence")
            val approval = RafGitFsApproval(
                requestId = plan.requestId,
                planHash = plan.planHash,
                approvedAt = System.currentTimeMillis(),
                approvedBy = "LOCAL_AUTHENTICATED_USER",
                scope = "EXACT_PLAN",
                confirmation = confirmation
            )
            val outcomes = syncEngine.execute(plan, approval)
            refreshWorkspaceLists(plan.workspaceId.orEmpty())
            val allObserved = outcomes.isNotEmpty() && outcomes.all { it.evidenceState == "OBSERVED" }
            _state.value = _state.value.copy(
                dryRun = outcomes,
                busy = false,
                status = RafGitFsUiStatus(
                    if (allObserved) RafGitFsUiEvidence.OBSERVED else RafGitFsUiEvidence.TOKEN_VAZIO,
                    if (allObserved) "Draft pull request sequence completed" else "Execution incomplete",
                    outcomes.joinToString(" · ") { it.result }.take(240)
                )
            )
        }
    }

    fun rollbackPublishedBranch() {
        val plan = _state.value.plan ?: return setGap("PLAN_REQUIRED")
        val confirmation = _state.value.rollbackText
        val expected = _state.value.expectedRollback
        if (confirmation != expected) return setGap("ROLLBACK_CONFIRMATION_MISMATCH")
        viewModelScope.launch {
            setBusy("Creating governed rollback commit")
            val outcome = branchWriter.rollbackToBase(plan, confirmation)
            _state.value = _state.value.copy(
                dryRun = listOf(outcome),
                busy = false,
                status = RafGitFsUiStatus(
                    if (outcome.evidenceState == "OBSERVED") RafGitFsUiEvidence.OBSERVED else RafGitFsUiEvidence.TOKEN_VAZIO,
                    outcome.result,
                    outcome.detail
                )
            )
        }
    }

    private fun createWorkspace() {
        if (profileId.isBlank() || repositoryFullName.isBlank() || refName.isBlank()) {
            setGap("WORKSPACE_ROUTE_INCOMPLETE")
            return
        }
        viewModelScope.launch {
            runCatching { workspaceStore.create(profileId, repositoryFullName, refName) }
                .onSuccess { workspace ->
                    _state.value = WorkspaceEditorUiState(
                        workspace = workspace,
                        status = RafGitFsUiStatus(
                            RafGitFsUiEvidence.OBSERVED,
                            "Private workspace ready",
                            workspace.workspaceId.take(12)
                        )
                    )
                }
                .onFailure { error -> setError("WORKSPACE_CREATE_FAILED", error.message) }
        }
    }

    private suspend fun refreshWorkspaceLists(workspaceId: String) {
        if (workspaceId.isBlank()) return
        val staged = stagedDao.listForWorkspace(workspaceId)
            .filter { it.operationType.startsWith("UPSERT_FILE:") }
        val conflicts = conflictDao.getByWorkspace(workspaceId)
        _state.value = _state.value.copy(stagedFiles = staged, conflicts = conflicts)
    }

    private fun setBusy(title: String) {
        _state.value = _state.value.copy(
            busy = true,
            status = RafGitFsUiStatus(RafGitFsUiEvidence.LOADING, title)
        )
    }

    private fun setGap(reason: String) {
        _state.value = _state.value.copy(
            busy = false,
            status = RafGitFsUiStatus(RafGitFsUiEvidence.TOKEN_VAZIO, "TOKEN_VAZIO", reason)
        )
    }

    private fun setError(code: String, detail: String?) {
        _state.value = _state.value.copy(
            busy = false,
            status = RafGitFsUiStatus(RafGitFsUiEvidence.ERROR, code, detail)
        )
    }

    private fun update(transform: WorkspaceEditorUiState.() -> WorkspaceEditorUiState) {
        _state.value = _state.value.transform()
    }

    private fun splitRepo(fullName: String): Pair<String, String>? {
        val parts = fullName.split('/', limit = 2)
        return if (parts.size == 2 && parts.all { it.isNotBlank() }) parts[0] to parts[1] else null
    }

    private fun decode(value: String?): String = value
        ?.let { URLDecoder.decode(it, "UTF-8") }
        .orEmpty()
}
