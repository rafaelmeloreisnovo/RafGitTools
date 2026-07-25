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
    private val log = RafGitFsSanitizedLog()

    suspend fun createPlan(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        baseCommitSha: String?,
        observed: List<RafGitFsObservedFile>,
        requestedAction: RafGitFsPlannedAction,
        requestId: String = UUID.randomUUID().toString()
    ): RafGitFsSyncPlan {
        require(profileId.isNotBlank())
        require(repositoryFullName.contains('/'))
        require(refName.isNotBlank())
        val now = System.currentTimeMillis()
        val jobId = UUID.randomUUID().toString()
        jobDao.upsert(
            TransferJobEntity(
                jobId = jobId,
                profileId = profileId,
                requestId = requestId,
                operationType = requestedAction.name,
                phase = RafGitFsSyncPhase.SCAN.name,
                syncState = RafGitFsJobState.SCANNING.name,
                repositoryFullName = repositoryFullName,
                refName = refName,
                path = null,
                bytesTotal = observed.size.toLong(),
                bytesCompleted = 0L,
                retryCount = 0,
                maxRetries = 3,
                lastErrorCode = null,
                createdAt = now,
                updatedAt = now,
                claimAllowed = false
            )
        )
        log(jobId, RafGitFsSyncPhase.SCAN, RafGitFsJobState.SCANNING, "SCAN_BEGIN", "items=${observed.size}")

        val diffs = RafGitFsDiffPlanner.diff(observed)
        jobDao.updateState(
            jobId, RafGitFsSyncPhase.DIFF.name, RafGitFsJobState.DIFF_READY.name,
            retryCount = 0, lastErrorCode = null
        )
        val plan = RafGitFsDiffPlanner.plan(
            requestId, profileId, repositoryFullName, refName,
            baseCommitSha, diffs, requestedAction, now
        )
        persistPlan(jobId, plan)
        val state = if (plan.conflicts.isNotEmpty() || plan.requiresApproval) {
            RafGitFsJobState.APPROVAL_REQUIRED
        } else {
            RafGitFsJobState.PLAN_READY
        }
        jobDao.updateState(
            jobId, RafGitFsSyncPhase.PLAN.name, state.name,
            retryCount = 0,
            lastErrorCode = if (plan.conflicts.isNotEmpty()) "CONFLICTS_PRESENT" else null
        )
        log(jobId, RafGitFsSyncPhase.PLAN, state, "PLAN_READY", "hash=${plan.planHash.take(16)}")
        return plan
    }

    suspend fun dryRun(plan: RafGitFsSyncPlan): List<RafGitFsExecutionOutcome> {
        val job = jobDao.getByRequestId(plan.requestId)
            ?: return listOf(tokenVazioOutcome(plan, "JOB_NOT_FOUND"))
        if (!validateStoredPlan(job.jobId, plan)) {
            return listOf(tokenVazioOutcome(plan, "PLAN_HASH_MISMATCH"))
        }
        jobDao.updateState(
            job.jobId, RafGitFsSyncPhase.DRY_RUN.name,
            if (plan.requiresApproval) RafGitFsJobState.APPROVAL_REQUIRED.name else RafGitFsJobState.PLAN_READY.name,
            job.retryCount, null
        )
        return plan.steps.map { step ->
            RafGitFsExecutionOutcome(
                step = step,
                result = if (step.executableNow) "WOULD_EXECUTE" else "WOULD_BLOCK",
                evidenceState = if (step.executableNow) "OBSERVED" else "TOKEN_VAZIO",
                observedSha = step.observedSha,
                retryable = false,
                detail = step.reason
            )
        }
    }

    suspend fun approve(plan: RafGitFsSyncPlan, approval: RafGitFsApproval): Boolean {
        val job = jobDao.getByRequestId(plan.requestId) ?: return false
        if (!validateApproval(plan, approval)) return false
        val changed = jobDao.compareAndSetState(
            jobId = job.jobId,
            expectedState = RafGitFsJobState.APPROVAL_REQUIRED.name,
            phase = RafGitFsSyncPhase.APPROVE.name,
            newState = RafGitFsJobState.APPROVED.name,
            retryCount = job.retryCount,
            lastErrorCode = null
        )
        if (changed == 1) {
            log(job.jobId, RafGitFsSyncPhase.APPROVE, RafGitFsJobState.APPROVED, "APPROVED", approval.scope)
        }
        return changed == 1
    }

    suspend fun execute(
        plan: RafGitFsSyncPlan,
        approval: RafGitFsApproval? = null
    ): List<RafGitFsExecutionOutcome> {
        val job = jobDao.getByRequestId(plan.requestId)
            ?: return listOf(tokenVazioOutcome(plan, "JOB_NOT_FOUND"))
        if (!validateStoredPlan(job.jobId, plan)) {
            return finalizeBlocked(job, plan, "PLAN_HASH_MISMATCH")
        }
        if (plan.conflicts.isNotEmpty()) {
            return finalizeBlocked(job, plan, "UNRESOLVED_CONFLICTS")
        }
        if (plan.requiresApproval) {
            val current = jobDao.getById(job.jobId) ?: job
            val approved = current.syncState == RafGitFsJobState.APPROVED.name ||
                (approval != null && validateApproval(plan, approval) && approve(plan, approval))
            if (!approved) return finalizeBlocked(job, plan, "APPROVAL_REQUIRED")
        }

        val currentJob = jobDao.getById(job.jobId) ?: job
        jobDao.updateState(
            job.jobId, RafGitFsSyncPhase.EXECUTE.name, RafGitFsJobState.EXECUTING.name,
            currentJob.retryCount, null
        )
        val outcomes = mutableListOf<RafGitFsExecutionOutcome>()
        for ((index, step) in plan.steps.sortedBy { it.order }.withIndex()) {
            val observedJob = jobDao.getById(job.jobId) ?: break
            if (observedJob.syncState == RafGitFsJobState.CANCELLED.name) break
            if (observedJob.syncState == RafGitFsJobState.PAUSED.name) break
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
            log(
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
        if (changed) {
            log(job.jobId, RafGitFsSyncPhase.RECEIPT, RafGitFsJobState.CANCELLED, "CANCELLED", reason)
        }
        return changed
    }

    suspend fun resume(plan: RafGitFsSyncPlan, approval: RafGitFsApproval? = null): List<RafGitFsExecutionOutcome> {
        val job = jobDao.getByRequestId(plan.requestId)
            ?: return listOf(tokenVazioOutcome(plan, "JOB_NOT_FOUND"))
        if (job.syncState != RafGitFsJobState.PAUSED.name && job.syncState != RafGitFsJobState.TOKEN_VAZIO.name) {
            return listOf(tokenVazioOutcome(plan, "JOB_NOT_RESUMABLE:${job.syncState}"))
        }
        if (job.retryCount >= job.maxRetries) {
            return finalizeBlocked(job, plan, "RETRY_LIMIT_REACHED")
        }
        jobDao.updateState(
            job.jobId, RafGitFsSyncPhase.EXECUTE.name, RafGitFsJobState.APPROVED.name,
            job.retryCount + 1, null
        )
        return execute(plan, approval)
    }

    fun logs(): List<RafGitFsLogEvent> = log.snapshot()

    private suspend fun persistPlan(jobId: String, plan: RafGitFsSyncPlan) {
        stagedDao.upsertAll(plan.steps.map { step ->
            StagedOperationEntity(
                operationId = "$jobId:${step.order}",
                jobId = jobId,
                workspaceId = null,
                operationType = step.action.name,
                repositoryFullName = plan.repositoryFullName,
                refName = plan.refName,
                path = step.path,
                baseSha = step.baseSha,
                localSha = step.observedSha,
                payloadHash = plan.planHash,
                state = if (step.executableNow) "PLANNED" else "BLOCKED_CAPABILITY",
                createdAt = plan.generatedAt
            )
        })
        conflictDao.upsertAll(plan.conflicts.map { conflict ->
            SyncConflictEntity(
                conflictId = "$jobId:${RafGitFsCanonical.sha256(conflict.path).take(16)}",
                jobId = jobId,
                workspaceId = null,
                repositoryFullName = plan.repositoryFullName,
                refName = plan.refName,
                path = conflict.path,
                conflictState = conflict.kind.name,
                localSha = conflict.localSha,
                remoteSha = conflict.remoteSha,
                resolution = null,
                detectedAt = plan.generatedAt,
                resolvedAt = null
            )
        })
    }

    private suspend fun validateStoredPlan(jobId: String, plan: RafGitFsSyncPlan): Boolean {
        val operations = stagedDao.listForJob(jobId)
        if (operations.size != plan.steps.size) return false
        return operations.all { it.payloadHash == plan.planHash } && !plan.claimAllowed
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
        jobDao.updateState(
            job.jobId, RafGitFsSyncPhase.RECEIPT.name, state.name,
            job.retryCount, if (hasGap) "EVIDENCE_GAP" else null
        )
        val receipt = RafGitFsReceiptFactory.create(
            plan = plan,
            outcomes = outcomes,
            finalPhase = RafGitFsSyncPhase.RECEIPT,
            allowed = !hasError && plan.conflicts.isEmpty(),
            result = state.name
        )
        runCatching { receiptDao.append(receipt) }
        log(job.jobId, RafGitFsSyncPhase.RECEIPT, state, "RECEIPT", receipt.receiptHash.take(16))
        return outcomes
    }

    private suspend fun finalizeBlocked(
        job: TransferJobEntity,
        plan: RafGitFsSyncPlan,
        code: String
    ): List<RafGitFsExecutionOutcome> {
        val outcome = tokenVazioOutcome(plan, code)
        jobDao.updateState(
            job.jobId, RafGitFsSyncPhase.RECEIPT.name, RafGitFsJobState.TOKEN_VAZIO.name,
            job.retryCount, code
        )
        val receipt = RafGitFsReceiptFactory.create(
            plan, listOf(outcome), RafGitFsSyncPhase.RECEIPT,
            allowed = false, result = "BLOCKED:$code"
        )
        runCatching { receiptDao.append(receipt) }
        return listOf(outcome)
    }

    private fun tokenVazioOutcome(plan: RafGitFsSyncPlan, code: String): RafGitFsExecutionOutcome {
        val step = plan.steps.firstOrNull() ?: RafGitFsPlanStep(
            0, RafGitFsPlannedAction.NO_OP, null, RafGitFsOperationRisk.READ_ONLY,
            null, null, false, false, code
        )
        return RafGitFsExecutionOutcome(step, "BLOCKED", "TOKEN_VAZIO", retryable = false, detail = code)
    }

    private fun log(
        jobId: String,
        phase: RafGitFsSyncPhase,
        state: RafGitFsJobState,
        code: String,
        detail: String?
    ) {
        log.append(RafGitFsLogEvent(jobId, phase.name, state.name, code, detail, System.currentTimeMillis()))
    }
}
