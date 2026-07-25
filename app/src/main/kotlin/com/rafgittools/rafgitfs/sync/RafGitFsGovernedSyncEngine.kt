package com.rafgittools.rafgitfs.sync

import com.rafgittools.rafgitfs.data.OperationReceiptDao
import com.rafgittools.rafgitfs.data.StagedOperationDao
import com.rafgittools.rafgitfs.data.StagedOperationEntity
import com.rafgittools.rafgitfs.data.SyncConflictDao
import com.rafgittools.rafgitfs.data.SyncConflictEntity
import com.rafgittools.rafgitfs.data.TransferJobDao
import com.rafgittools.rafgitfs.data.TransferJobEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RafGitFsGovernedSyncEngine @Inject constructor(
    private val jobDao: TransferJobDao,
    private val stagedDao: StagedOperationDao,
    private val conflictDao: SyncConflictDao,
    private val receiptDao: OperationReceiptDao,
    private val executor: RafGitFsStepExecutor
) {
    private val eventLog = RafGitFsSanitizedLog()

    suspend fun createPlan(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        baseCommitSha: String?,
        observed: List<RafGitFsObservedFile>,
        requestedAction: RafGitFsPlannedAction,
        requestId: String = UUID.randomUUID().toString(),
        workspaceId: String? = null
    ): RafGitFsSyncPlan {
        require(profileId.isNotBlank())
        require(repositoryFullName.contains('/'))
        require(refName.isNotBlank())
        val now = System.currentTimeMillis()
        val jobId = UUID.randomUUID().toString()
        jobDao.upsert(
            TransferJobEntity(
                jobId, profileId, requestId, requestedAction.name,
                RafGitFsSyncPhase.SCAN.name, RafGitFsJobState.SCANNING.name,
                repositoryFullName, refName, workspaceId,
                observed.size.toLong(), 0L, 0, 3, null, now, now, false
            )
        )
        event(jobId, RafGitFsSyncPhase.SCAN, RafGitFsJobState.SCANNING, "SCAN_BEGIN", "items=${observed.size}")
        val diffs = RafGitFsDiffPlanner.diff(observed)
        jobDao.updateState(jobId, "DIFF", "DIFF_READY", 0, null)
        val plan = RafGitFsDiffPlanner.plan(
            requestId, profileId, repositoryFullName, refName, baseCommitSha,
            diffs, requestedAction, now, workspaceId
        )
        persistPlan(jobId, plan)
        val state = if (plan.conflicts.isNotEmpty() || plan.requiresApproval) {
            RafGitFsJobState.APPROVAL_REQUIRED
        } else RafGitFsJobState.PLAN_READY
        jobDao.updateState(
            jobId, RafGitFsSyncPhase.PLAN.name, state.name, 0,
            if (plan.conflicts.isNotEmpty()) "CONFLICTS_PRESENT" else null
        )
        event(jobId, RafGitFsSyncPhase.PLAN, state, "PLAN_READY", plan.planHash.take(16))
        return plan
    }

    suspend fun dryRun(plan: RafGitFsSyncPlan): List<RafGitFsExecutionOutcome> {
        val job = jobDao.getByRequestId(plan.requestId)
            ?: return listOf(gap(plan, "JOB_NOT_FOUND"))
        if (!validateStoredPlan(job.jobId, plan)) return listOf(gap(plan, "PLAN_HASH_MISMATCH"))
        jobDao.updateState(
            job.jobId, "DRY_RUN",
            if (plan.requiresApproval) "APPROVAL_REQUIRED" else "PLAN_READY",
            job.retryCount, null
        )
        return plan.steps.map {
            RafGitFsExecutionOutcome(
                it,
                if (it.executableNow) "WOULD_EXECUTE" else "WOULD_BLOCK",
                if (it.executableNow) "OBSERVED" else "TOKEN_VAZIO",
                it.observedSha,
                false,
                it.reason
            )
        }
    }

    suspend fun approve(plan: RafGitFsSyncPlan, approval: RafGitFsApproval): Boolean {
        val job = jobDao.getByRequestId(plan.requestId) ?: return false
        if (!validateApproval(plan, approval)) return false
        val changed = jobDao.compareAndSetState(
            job.jobId, "APPROVAL_REQUIRED", "APPROVE", "APPROVED",
            job.retryCount, null
        ) == 1
        if (changed) event(job.jobId, RafGitFsSyncPhase.APPROVE, RafGitFsJobState.APPROVED, "APPROVED", approval.scope)
        return changed
    }

    suspend fun execute(
        plan: RafGitFsSyncPlan,
        approval: RafGitFsApproval? = null
    ): List<RafGitFsExecutionOutcome> {
        val job = jobDao.getByRequestId(plan.requestId)
            ?: return listOf(gap(plan, "JOB_NOT_FOUND"))
        if (!validateStoredPlan(job.jobId, plan)) return finalizeBlocked(job, plan, "PLAN_HASH_MISMATCH")
        if (plan.conflicts.isNotEmpty()) return finalizeBlocked(job, plan, "UNRESOLVED_CONFLICTS")
        if (plan.requiresApproval) {
            val current = jobDao.getById(job.jobId) ?: job
            val approved = current.syncState == "APPROVED" ||
                (approval != null && validateApproval(plan, approval) && approve(plan, approval))
            if (!approved) return finalizeBlocked(job, plan, "APPROVAL_REQUIRED")
        }
        val current = jobDao.getById(job.jobId) ?: job
        jobDao.updateState(job.jobId, "EXECUTE", "EXECUTING", current.retryCount, null)
        val outcomes = mutableListOf<RafGitFsExecutionOutcome>()
        for ((index, step) in plan.steps.sortedBy { it.order }.withIndex()) {
            val state = jobDao.getById(job.jobId)?.syncState ?: break
            if (state in setOf("CANCELLED", "PAUSED")) break
            val outcome = try {
                executor.execute(plan, step)
            } catch (error: Exception) {
                RafGitFsExecutionOutcome(
                    step, "FAILED", "ERROR", retryable = true,
                    detail = RafGitFsCanonical.sanitize(error.message ?: error::class.java.simpleName)
                )
            }
            outcomes += outcome
            jobDao.updateProgress(job.jobId, (index + 1).toLong(), plan.steps.size.toLong())
            event(
                job.jobId, RafGitFsSyncPhase.EXECUTE,
                if (outcome.evidenceState == "OBSERVED") RafGitFsJobState.EXECUTING else RafGitFsJobState.TOKEN_VAZIO,
                outcome.result, outcome.detail
            )
            if (outcome.evidenceState == "ERROR" && !outcome.retryable) break
        }
        return finalize(job, plan, outcomes)
    }

    suspend fun pause(requestId: String, reason: String): Boolean {
        val job = jobDao.getByRequestId(requestId) ?: return false
        return jobDao.pause(job.jobId, RafGitFsCanonical.sanitize(reason).orEmpty()) == 1
    }

    suspend fun cancel(requestId: String, reason: String): Boolean {
        val job = jobDao.getByRequestId(requestId) ?: return false
        val changed = jobDao.cancel(job.jobId, RafGitFsCanonical.sanitize(reason).orEmpty()) == 1
        if (changed) event(job.jobId, RafGitFsSyncPhase.RECEIPT, RafGitFsJobState.CANCELLED, "CANCELLED", reason)
        return changed
    }

    suspend fun resume(
        plan: RafGitFsSyncPlan,
        approval: RafGitFsApproval? = null
    ): List<RafGitFsExecutionOutcome> {
        val job = jobDao.getByRequestId(plan.requestId)
            ?: return listOf(gap(plan, "JOB_NOT_FOUND"))
        if (job.syncState !in setOf("PAUSED", "TOKEN_VAZIO")) {
            return listOf(gap(plan, "JOB_NOT_RESUMABLE:${job.syncState}"))
        }
        if (job.retryCount >= job.maxRetries) return finalizeBlocked(job, plan, "RETRY_LIMIT_REACHED")
        jobDao.updateState(job.jobId, "EXECUTE", "APPROVED", job.retryCount + 1, null)
        return execute(plan, approval)
    }

    fun logs(): List<RafGitFsLogEvent> = eventLog.snapshot()

    private suspend fun persistPlan(jobId: String, plan: RafGitFsSyncPlan) {
        stagedDao.upsertAll(plan.steps.map { step ->
            StagedOperationEntity(
                "$jobId:${step.order}", jobId, plan.workspaceId, step.action.name,
                plan.repositoryFullName, plan.refName, step.path, step.baseSha,
                step.observedSha, plan.planHash,
                if (step.executableNow) "PLANNED" else "BLOCKED_CAPABILITY",
                plan.generatedAt
            )
        })
        conflictDao.upsertAll(plan.conflicts.map { conflict ->
            SyncConflictEntity(
                "$jobId:${RafGitFsCanonical.sha256(conflict.path).take(16)}",
                jobId, plan.workspaceId, plan.repositoryFullName, plan.refName,
                conflict.path, conflict.kind.name, conflict.localSha, conflict.remoteSha,
                null, plan.generatedAt, null
            )
        })
    }

    private suspend fun validateStoredPlan(jobId: String, plan: RafGitFsSyncPlan): Boolean {
        val operations = stagedDao.listForJob(jobId)
        if (operations.size != plan.steps.size) return false
        return operations.all {
            it.payloadHash == plan.planHash && it.workspaceId == plan.workspaceId
        } && !plan.claimAllowed
    }

    private fun validateApproval(plan: RafGitFsSyncPlan, approval: RafGitFsApproval): Boolean =
        approval.requestId == plan.requestId &&
            approval.planHash == plan.planHash &&
            approval.approvedBy.isNotBlank() &&
            approval.scope == "EXACT_PLAN" &&
            approval.confirmation == "APPROVE ${plan.planHash.take(12)}"

    private suspend fun finalize(
        job: TransferJobEntity,
        plan: RafGitFsSyncPlan,
        outcomes: List<RafGitFsExecutionOutcome>
    ): List<RafGitFsExecutionOutcome> {
        val hasError = outcomes.any { it.evidenceState == "ERROR" }
        val hasGap = outcomes.any { it.evidenceState != "OBSERVED" }
        val state = when {
            hasError -> RafGitFsJobState.FAILED
            hasGap -> RafGitFsJobState.TOKEN_VAZIO
            else -> RafGitFsJobState.COMPLETE
        }
        jobDao.updateState(job.jobId, "RECEIPT", state.name, job.retryCount, if (hasGap) "EVIDENCE_GAP" else null)
        val receipt = RafGitFsReceiptFactory.create(
            plan, outcomes, RafGitFsSyncPhase.RECEIPT,
            allowed = !hasError && plan.conflicts.isEmpty(), result = state.name
        )
        runCatching { receiptDao.append(receipt) }
        event(job.jobId, RafGitFsSyncPhase.RECEIPT, state, "RECEIPT", receipt.receiptHash.take(16))
        return outcomes
    }

    private suspend fun finalizeBlocked(
        job: TransferJobEntity,
        plan: RafGitFsSyncPlan,
        code: String
    ): List<RafGitFsExecutionOutcome> {
        val outcome = gap(plan, code)
        jobDao.updateState(job.jobId, "RECEIPT", "TOKEN_VAZIO", job.retryCount, code)
        runCatching {
            receiptDao.append(
                RafGitFsReceiptFactory.create(
                    plan, listOf(outcome), RafGitFsSyncPhase.RECEIPT,
                    allowed = false, result = "BLOCKED:$code"
                )
            )
        }
        return listOf(outcome)
    }

    private fun gap(plan: RafGitFsSyncPlan, code: String): RafGitFsExecutionOutcome {
        val step = plan.steps.firstOrNull() ?: RafGitFsPlanStep(
            0, RafGitFsPlannedAction.NO_OP, null, RafGitFsOperationRisk.READ_ONLY,
            null, null, false, false, code
        )
        return RafGitFsExecutionOutcome(
            step = step,
            result = "BLOCKED",
            evidenceState = "TOKEN_VAZIO",
            observedSha = null,
            retryable = false,
            detail = code
        )
    }

    private fun event(
        jobId: String,
        phase: RafGitFsSyncPhase,
        state: RafGitFsJobState,
        code: String,
        detail: String?
    ) = eventLog.append(
        RafGitFsLogEvent(jobId, phase.name, state.name, code, detail, System.currentTimeMillis())
    )
}
