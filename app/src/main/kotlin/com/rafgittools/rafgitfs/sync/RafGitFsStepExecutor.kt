package com.rafgittools.rafgitfs.sync

import com.rafgittools.rafgitfs.assurance.RafGitFsRuntimeSecurityGate
import com.rafgittools.rafgitfs.assurance.RafGitFsSecurityDecision
import com.rafgittools.rafgitfs.cache.RafGitFsCacheResult
import com.rafgittools.rafgitfs.cache.RafGitFsOfflineCacheManager
import javax.inject.Inject
import javax.inject.Singleton

interface RafGitFsRemoteWriteCapability {
    suspend fun execute(plan: RafGitFsSyncPlan, step: RafGitFsPlanStep): RafGitFsExecutionOutcome
}

/** Prompt 6 fallback; Prompt 7+ replaces the Hilt binding with a governed writer. */
@Singleton
class RafGitFsBlockedRemoteWriteCapability @Inject constructor() : RafGitFsRemoteWriteCapability {
    override suspend fun execute(
        plan: RafGitFsSyncPlan,
        step: RafGitFsPlanStep
    ): RafGitFsExecutionOutcome = RafGitFsExecutionOutcome(
        step = step,
        result = "BLOCKED",
        evidenceState = "TOKEN_VAZIO",
        retryable = false,
        detail = "TOKEN_VAZIO_CAPABILITY_PROMPT_7"
    )
}

@Singleton
class RafGitFsStepExecutor @Inject constructor(
    private val cacheManager: RafGitFsOfflineCacheManager,
    private val remoteWrite: RafGitFsRemoteWriteCapability,
    private val securityGate: RafGitFsRuntimeSecurityGate
) {
    suspend fun execute(
        plan: RafGitFsSyncPlan,
        step: RafGitFsPlanStep
    ): RafGitFsExecutionOutcome {
        if (!step.executableNow) return blocked(step, "STEP_NOT_EXECUTABLE")
        return when (step.action) {
            RafGitFsPlannedAction.NO_OP -> RafGitFsExecutionOutcome(
                step, "NO_OP", "OBSERVED", observedSha = step.observedSha
            )
            RafGitFsPlannedAction.CACHE_DOWNLOAD -> cache(plan, step, pin = false)
            RafGitFsPlannedAction.PIN_OFFLINE -> cache(plan, step, pin = true)
            RafGitFsPlannedAction.REMOVE_LOCAL_CACHE -> blocked(
                step,
                "CACHE_KEY_REQUIRED_FOR_EXPLICIT_DELETE"
            )
            RafGitFsPlannedAction.CREATE_WORKSPACE,
            RafGitFsPlannedAction.WRITE_WORKSPACE_FILE -> blocked(
                step,
                "LOCAL_WORKSPACE_UI_CAPABILITY_ONLY"
            )
            RafGitFsPlannedAction.CREATE_BRANCH,
            RafGitFsPlannedAction.CREATE_COMMIT,
            RafGitFsPlannedAction.PUSH_BRANCH,
            RafGitFsPlannedAction.OPEN_PULL_REQUEST -> executeRemote(plan, step)
            RafGitFsPlannedAction.DELETE_REMOTE -> blocked(
                step,
                "DESTRUCTIVE_REMOTE_PERMANENTLY_BLOCKED"
            )
        }
    }

    private suspend fun executeRemote(
        plan: RafGitFsSyncPlan,
        step: RafGitFsPlanStep
    ): RafGitFsExecutionOutcome {
        val assessment = securityGate.assessAfterExactApproval(plan)
        if (assessment.decision != RafGitFsSecurityDecision.ALLOW) {
            val detail = (assessment.blockingCodes + assessment.tokenVazioCodes)
                .distinct()
                .joinToString(",")
                .ifBlank { "SECURITY_ASSESSMENT_NOT_ALLOW" }
            return blocked(step, detail)
        }
        return remoteWrite.execute(plan, step)
    }

    private suspend fun cache(
        plan: RafGitFsSyncPlan,
        step: RafGitFsPlanStep,
        pin: Boolean
    ): RafGitFsExecutionOutcome {
        val path = step.path ?: return blocked(step, "PATH_REQUIRED")
        return when (val result = cacheManager.read(
            profileId = plan.profileId,
            repositoryFullName = plan.repositoryFullName,
            refName = plan.refName,
            path = path,
            allowNetwork = true,
            pinAfterDownload = pin
        )) {
            is RafGitFsCacheResult.Success -> RafGitFsExecutionOutcome(
                step = step,
                result = if (pin) "PINNED_OFFLINE" else "CONTENT_CACHED",
                evidenceState = "OBSERVED",
                observedSha = result.value.snapshot.blobSha,
                detail = result.value.checksumSha256.take(16)
            )
            is RafGitFsCacheResult.TokenVazio -> RafGitFsExecutionOutcome(
                step,
                "TOKEN_VAZIO",
                "TOKEN_VAZIO",
                retryable = true,
                detail = RafGitFsCanonical.sanitize(result.reason)
            )
            is RafGitFsCacheResult.Failure -> RafGitFsExecutionOutcome(
                step,
                "FAILED",
                "ERROR",
                retryable = result.retryable,
                detail = RafGitFsCanonical.sanitize("${result.code}:${result.message}")
            )
        }
    }

    private fun blocked(step: RafGitFsPlanStep, detail: String) = RafGitFsExecutionOutcome(
        step = step,
        result = "BLOCKED",
        evidenceState = "TOKEN_VAZIO",
        retryable = false,
        detail = detail
    )
}
