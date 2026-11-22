package com.rafgittools.ui.screens.actions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.github.ActionsControlPolicy
import com.rafgittools.data.github.ActionsControlReceipt
import com.rafgittools.data.github.ActionsRun
import com.rafgittools.data.github.ActionsWorkflow
import com.rafgittools.data.github.GithubApiService
import com.rafgittools.data.github.WorkflowDispatchRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response
import javax.inject.Inject

data class ActionsControlUiState(
    val repository: String = "RafGitTools",
    val reference: String = "",
    val workflows: List<ActionsWorkflow> = emptyList(),
    val runs: List<ActionsRun> = emptyList(),
    val busy: Boolean = false,
    val authRequired: Boolean = false,
    val message: String = "Not loaded",
    val lastReceipt: ActionsControlReceipt? = null,
    val receiptDurable: Boolean = false
)

enum class ActionsMutation { DISPATCH, CANCEL, RERUN, RERUN_FAILED }

@HiltViewModel
class ActionsControlViewModel @Inject constructor(
    private val api: GithubApiService,
    private val auth: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(ActionsControlUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun setRepository(repository: String) {
        if (!ActionsControlPolicy.repositoryAllowed(repository) || _state.value.busy) return
        _state.value = ActionsControlUiState(repository = repository)
        refresh()
    }

    fun setReference(reference: String) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(reference = reference.take(128))
    }

    fun refresh() {
        if (_state.value.busy) return
        val requested = _state.value.repository
        _state.value = _state.value.copy(busy = true, message = "Reading GitHub provider state…")
        viewModelScope.launch {
            try {
                if (!auth.isAuthenticated() || auth.isOfflineMode()) {
                    _state.value = _state.value.copy(
                        busy = false, authRequired = true, message = "GitHub sign-in required"
                    )
                    return@launch
                }
                val repository = api.getRepository(ActionsControlPolicy.OWNER, requested)
                val workflows = api.listActionsWorkflows(ActionsControlPolicy.OWNER, requested)
                val runs = api.listActionsRuns(ActionsControlPolicy.OWNER, requested)
                if (!workflows.isSuccessful || !runs.isSuccessful) {
                    val code = if (!workflows.isSuccessful) workflows.code() else runs.code()
                    _state.value = _state.value.copy(
                        busy = false, workflows = emptyList(), runs = emptyList(),
                        message = "PROVIDER_DENIED_HTTP_$code — check token Actions permissions"
                    )
                    return@launch
                }
                val old = _state.value
                _state.value = old.copy(
                    busy = false, authRequired = false,
                    reference = if (old.reference.isBlank()) repository.defaultBranch else old.reference,
                    workflows = workflows.body()?.workflows.orEmpty().filter { it.id > 0 },
                    runs = runs.body()?.workflow_runs.orEmpty().filter { it.id > 0 },
                    message = "PROVIDER_OBSERVED — page 1 only; no execution claim"
                )
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    busy = false, message = "PROVIDER_UNAVAILABLE — no mutation performed"
                )
            }
        }
    }

    /** Called only after explicit UI phrase confirmation; never on screen load. */
    fun mutate(operation: ActionsMutation, targetId: Long, inputsJson: String = "{}") {
        val snapshot = _state.value
        if (snapshot.busy || targetId <= 0 || !ActionsControlPolicy.repositoryAllowed(snapshot.repository)) return
        val workflow = snapshot.workflows.firstOrNull { it.id == targetId }
        val run = snapshot.runs.firstOrNull { it.id == targetId }
        if (operation == ActionsMutation.DISPATCH &&
            (workflow == null || !ActionsControlPolicy.workflowAllowed(workflow) ||
                !ActionsControlPolicy.isSafeRef(snapshot.reference))) {
            _state.value = snapshot.copy(message = "BLOCKED — invalid workflow or ref")
            return
        }
        if (operation == ActionsMutation.CANCEL && (run == null ||
                !ActionsControlPolicy.runCanCancel(run))) return
        if (operation == ActionsMutation.RERUN && (run == null ||
                !ActionsControlPolicy.runCanRerun(run))) return
        if (operation == ActionsMutation.RERUN_FAILED && (run == null ||
                !ActionsControlPolicy.runCanRerunFailedJobs(run))) return
        val inputs = if (operation == ActionsMutation.DISPATCH) {
            parseInputs(inputsJson) ?: run {
                _state.value = snapshot.copy(message = "BLOCKED — inputs must be bounded string JSON")
                return
            }
        } else emptyMap()

        _state.value = snapshot.copy(busy = true, message = "Checking live target before mutation…")
        viewModelScope.launch {
            try {
                if (!auth.isAuthenticated() || auth.isOfflineMode() ||
                    api.getAuthenticatedUser().login != ActionsControlPolicy.OWNER) {
                    _state.value = _state.value.copy(
                        busy = false, message = "BLOCKED — owner GitHub session required"
                    )
                    return@launch
                }
                val owner = ActionsControlPolicy.OWNER
                val repository = snapshot.repository
                val response: Response<Unit> = if (operation == ActionsMutation.DISPATCH) {
                    val fresh = api.listActionsWorkflows(owner, repository)
                    if (!fresh.isSuccessful || fresh.body()?.workflows.orEmpty().none {
                            it.id == targetId && ActionsControlPolicy.workflowAllowed(it)
                        }) {
                        _state.value = _state.value.copy(
                            busy = false, message = "BLOCKED — workflow no longer eligible"
                        )
                        return@launch
                    }
                    if (!prepareIntent(repository, operation, targetId,
                            null, snapshot.reference)) return@launch
                    api.dispatchActionsWorkflow(
                        owner, repository, targetId, WorkflowDispatchRequest(snapshot.reference, inputs)
                    )
                } else {
                    // Live readback prevents a stale card from cancelling/rerunning a new run.
                    val latestResponse = api.getActionsRun(owner, repository, targetId)
                    val latest = latestResponse.body()
                    if (!latestResponse.isSuccessful || latest == null ||
                        latest.id != targetId || latest.headSha != run?.headSha ||
                        !operationAllowed(operation, latest)) {
                        _state.value = _state.value.copy(
                            busy = false, message = "BLOCKED — run changed; refresh"
                        )
                        return@launch
                    }
                    if (!prepareIntent(repository, operation, targetId,
                            latest.headSha, latest.headBranch ?: "TOKEN_VAZIO")) return@launch
                    when (operation) {
                        ActionsMutation.CANCEL -> api.cancelActionsRun(owner, repository, targetId)
                        ActionsMutation.RERUN -> api.rerunActionsRun(owner, repository, targetId)
                        ActionsMutation.RERUN_FAILED -> api.rerunFailedActionsJobs(owner, repository, targetId)
                        ActionsMutation.DISPATCH -> error("Unreachable")
                    }
                }
                recordResponse(repository, operation, targetId, run?.headSha,
                    if (operation == ActionsMutation.DISPATCH) snapshot.reference
                    else run?.headBranch ?: "TOKEN_VAZIO", response)
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    busy = false, message = "PROVIDER_UNAVAILABLE — outcome unknown; refresh before retry"
                )
            }
        }
    }

    private fun operationAllowed(operation: ActionsMutation, run: ActionsRun) = when (operation) {
        ActionsMutation.DISPATCH -> false
        ActionsMutation.CANCEL -> ActionsControlPolicy.runCanCancel(run)
        ActionsMutation.RERUN -> ActionsControlPolicy.runCanRerun(run)
        ActionsMutation.RERUN_FAILED -> ActionsControlPolicy.runCanRerunFailedJobs(run)
    }

    private fun parseInputs(raw: String): Map<String, String>? {
        if (raw.length > 4096) return null
        return try {
            val objectValue = JSONObject(raw)
            if (objectValue.length() > 16) return null
            val result = linkedMapOf<String, String>()
            val keys = objectValue.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (!Regex("^[a-zA-Z_][a-zA-Z0-9_-]{0,63}$").matches(key)) return null
                val value = objectValue.opt(key)
                if (value !is String || value.length > 512) return null
                result[key] = value
            }
            result
        } catch (_: Exception) { null }
    }

    /** A durable pre-operation intent is mandatory: no journal, no mutation. */
    private suspend fun prepareIntent(
        repository: String, operation: ActionsMutation, targetId: Long,
        sourceSha: String?, targetRef: String
    ): Boolean {
        val intent = ActionsControlReceipt(
            repository = "${ActionsControlPolicy.OWNER}/$repository",
            operation = operation.name,
            targetId = targetId,
            sourceSha = sourceSha ?: "TOKEN_VAZIO",
            state = "INTENT_PREPARED",
            providerHttpCode = 0,
            targetRef = targetRef
        )
        if (persistReceipt(intent)) return true
        _state.value = _state.value.copy(
            busy = false,
            lastReceipt = intent,
            receiptDurable = false,
            message = "BLOCKED — cannot fsync local pre-operation receipt"
        )
        return false
    }

    private suspend fun recordResponse(
        repository: String, operation: ActionsMutation, targetId: Long,
        sourceSha: String?, targetRef: String, response: Response<Unit>
    ) {
        val disposition = if (response.isSuccessful) "REQUEST_ACCEPTED" else "PROVIDER_DENIED"
        val receipt = ActionsControlReceipt(
            repository = "${ActionsControlPolicy.OWNER}/$repository",
            operation = operation.name,
            targetId = targetId,
            sourceSha = sourceSha ?: "TOKEN_VAZIO",
            state = disposition,
            providerHttpCode = response.code(),
            targetRef = targetRef
        )
        val durable = persistReceipt(receipt)
        _state.value = _state.value.copy(
            busy = false, lastReceipt = receipt, receiptDurable = durable,
            message = disposition + "_HTTP_" + response.code() +
                (if (durable) " · RECEIPT_DURABLE" else " · RECEIPT_TOKEN_VAZIO")
        )
        // A successful 204 acknowledges a request, not the completed workflow.
    }

    private suspend fun persistReceipt(receipt: ActionsControlReceipt): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val record = JSONObject().apply {
                    put("schema", "rafgittools.actions-control-request/v1")
                    put("repository", receipt.repository)
                    put("operation", receipt.operation)
                    put("target_id", receipt.targetId)
                    put("target_ref", receipt.targetRef)
                    put("source_sha", receipt.sourceSha)
                    put("state", receipt.state)
                    put("provider_http_code", receipt.providerHttpCode)
                    put("observed_at_epoch_ms", System.currentTimeMillis())
                    put("run_completed", false)
                    put("claim_allowed", false)
                }.toString() + "\n"
                context.openFileOutput("actions-control-receipts.jsonl", Context.MODE_APPEND).use {
                    it.write(record.toByteArray(Charsets.UTF_8))
                    it.fd.sync()
                }
                true
            } catch (_: Exception) { false }
        }
}
