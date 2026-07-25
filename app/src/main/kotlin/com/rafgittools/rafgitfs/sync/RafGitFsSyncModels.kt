package com.rafgittools.rafgitfs.sync

enum class RafGitFsSyncPhase { SCAN, DIFF, PLAN, DRY_RUN, APPROVE, EXECUTE, RECEIPT }

enum class RafGitFsJobState {
    CREATED, SCANNING, DIFF_READY, PLAN_READY, APPROVAL_REQUIRED, APPROVED,
    EXECUTING, PAUSED, CANCELLED, COMPLETE, FAILED, TOKEN_VAZIO
}

enum class RafGitFsOperationRisk {
    READ_ONLY, LOCAL_MUTATION, REMOTE_BRANCH_WRITE, PROTECTED_BRANCH_WRITE, DESTRUCTIVE_REMOTE
}

enum class RafGitFsPlannedAction {
    NO_OP, CACHE_DOWNLOAD, PIN_OFFLINE, REMOVE_LOCAL_CACHE,
    CREATE_WORKSPACE, WRITE_WORKSPACE_FILE, CREATE_BRANCH, CREATE_COMMIT,
    PUSH_BRANCH, OPEN_PULL_REQUEST, DELETE_REMOTE
}

data class RafGitFsObservedFile(
    val path: String,
    val remoteSha: String?,
    val localSha: String?,
    val localExists: Boolean,
    val remoteExists: Boolean,
    val pinned: Boolean = false
)

data class RafGitFsDiffItem(
    val path: String,
    val kind: Kind,
    val localSha: String?,
    val remoteSha: String?,
    val conflict: Boolean,
    val evidenceState: String
) {
    enum class Kind { EQUAL, LOCAL_ONLY, REMOTE_ONLY, LOCAL_CHANGED, REMOTE_CHANGED, BOTH_CHANGED, UNKNOWN }
}

data class RafGitFsPlanStep(
    val order: Int,
    val action: RafGitFsPlannedAction,
    val path: String?,
    val risk: RafGitFsOperationRisk,
    val baseSha: String?,
    val observedSha: String?,
    val requiresApproval: Boolean,
    val executableNow: Boolean,
    val reason: String
)

data class RafGitFsSyncPlan(
    val requestId: String,
    val profileId: String,
    val repositoryFullName: String,
    val refName: String,
    val baseCommitSha: String?,
    val steps: List<RafGitFsPlanStep>,
    val conflicts: List<RafGitFsDiffItem>,
    val generatedAt: Long,
    val planHash: String,
    val workspaceId: String? = null,
    val claimAllowed: Boolean = false
) {
    val requiresApproval: Boolean get() = steps.any { it.requiresApproval }
    val executableStepCount: Int get() = steps.count { it.executableNow }
}

data class RafGitFsApproval(
    val requestId: String,
    val planHash: String,
    val approvedAt: Long,
    val approvedBy: String,
    val scope: String,
    val confirmation: String
)

data class RafGitFsExecutionOutcome(
    val step: RafGitFsPlanStep,
    val result: String,
    val evidenceState: String,
    val observedSha: String? = null,
    val retryable: Boolean = false,
    val detail: String? = null
)

data class RafGitFsJobSnapshot(
    val jobId: String,
    val requestId: String,
    val phase: RafGitFsSyncPhase,
    val state: RafGitFsJobState,
    val completedSteps: Int,
    val totalSteps: Int,
    val lastErrorCode: String?,
    val updatedAt: Long
)
