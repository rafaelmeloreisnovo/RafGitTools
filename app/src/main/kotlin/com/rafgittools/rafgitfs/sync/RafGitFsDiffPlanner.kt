package com.rafgittools.rafgitfs.sync

object RafGitFsDiffPlanner {
    private val branchActions = setOf(
        RafGitFsPlannedAction.CREATE_BRANCH,
        RafGitFsPlannedAction.CREATE_COMMIT,
        RafGitFsPlannedAction.PUSH_BRANCH,
        RafGitFsPlannedAction.OPEN_PULL_REQUEST
    )

    fun diff(observed: List<RafGitFsObservedFile>): List<RafGitFsDiffItem> = observed
        .distinctBy { it.path }
        .sortedBy { it.path }
        .map { file ->
            val kind = when {
                !file.localExists && !file.remoteExists -> RafGitFsDiffItem.Kind.UNKNOWN
                file.localExists && !file.remoteExists -> RafGitFsDiffItem.Kind.LOCAL_ONLY
                !file.localExists && file.remoteExists -> RafGitFsDiffItem.Kind.REMOTE_ONLY
                file.localSha == null || file.remoteSha == null -> RafGitFsDiffItem.Kind.UNKNOWN
                file.localSha.equals(file.remoteSha, ignoreCase = true) -> RafGitFsDiffItem.Kind.EQUAL
                else -> RafGitFsDiffItem.Kind.BOTH_CHANGED
            }
            RafGitFsDiffItem(
                file.path, kind, file.localSha, file.remoteSha,
                conflict = kind == RafGitFsDiffItem.Kind.BOTH_CHANGED || kind == RafGitFsDiffItem.Kind.UNKNOWN,
                evidenceState = if (kind == RafGitFsDiffItem.Kind.UNKNOWN) "TOKEN_VAZIO" else "OBSERVED"
            )
        }

    fun plan(
        requestId: String,
        profileId: String,
        repositoryFullName: String,
        refName: String,
        baseCommitSha: String?,
        diffs: List<RafGitFsDiffItem>,
        requestedAction: RafGitFsPlannedAction,
        generatedAt: Long = System.currentTimeMillis(),
        workspaceId: String? = null
    ): RafGitFsSyncPlan {
        val conflicts = diffs.filter { it.conflict }
        val steps = when {
            requestedAction in branchActions -> remoteSequence(baseCommitSha, workspaceId)
            diffs.isEmpty() -> listOf(
                RafGitFsPlanStep(
                    1, RafGitFsPlannedAction.NO_OP, null, RafGitFsOperationRisk.READ_ONLY,
                    baseCommitSha, baseCommitSha, false, true, "NO_DIFFERENCE_OBSERVED"
                )
            )
            else -> diffs.sortedBy { it.path }.mapIndexed { index, item ->
                localStep(index + 1, item, requestedAction)
            }
        }
        val payload = RafGitFsCanonical.planPayload(
            requestId, profileId, repositoryFullName, refName,
            baseCommitSha, steps, conflicts, generatedAt, workspaceId
        )
        return RafGitFsSyncPlan(
            requestId, profileId, repositoryFullName, refName, baseCommitSha,
            steps, conflicts, generatedAt, RafGitFsCanonical.sha256(payload), workspaceId, false
        )
    }

    private fun remoteSequence(baseCommitSha: String?, workspaceId: String?): List<RafGitFsPlanStep> {
        val executable = workspaceId != null && !baseCommitSha.isNullOrBlank()
        val reason = if (executable) "GOVERNED_BRANCH_CAPABILITY_AVAILABLE" else "WORKSPACE_AND_BASE_SHA_REQUIRED"
        return listOf(
            RafGitFsPlannedAction.CREATE_BRANCH,
            RafGitFsPlannedAction.CREATE_COMMIT,
            RafGitFsPlannedAction.PUSH_BRANCH,
            RafGitFsPlannedAction.OPEN_PULL_REQUEST
        ).mapIndexed { index, action ->
            RafGitFsPlanStep(
                order = index + 1,
                action = action,
                path = null,
                risk = RafGitFsOperationRisk.REMOTE_BRANCH_WRITE,
                baseSha = baseCommitSha,
                observedSha = baseCommitSha,
                requiresApproval = true,
                executableNow = executable,
                reason = reason
            )
        }
    }

    private fun localStep(
        order: Int,
        diff: RafGitFsDiffItem,
        action: RafGitFsPlannedAction
    ): RafGitFsPlanStep {
        if (diff.conflict) {
            return RafGitFsPlanStep(
                order, action, diff.path, riskFor(action), diff.localSha, diff.remoteSha,
                true, false, "CONFLICT_${diff.kind.name}"
            )
        }
        val risk = riskFor(action)
        val executable = action != RafGitFsPlannedAction.DELETE_REMOTE
        return RafGitFsPlanStep(
            order, action, diff.path, risk, diff.localSha, diff.remoteSha,
            requiresApproval = risk != RafGitFsOperationRisk.READ_ONLY,
            executableNow = executable,
            reason = if (executable) "LOCAL_CAPABILITY_AVAILABLE" else "DESTRUCTIVE_REMOTE_PERMANENTLY_BLOCKED"
        )
    }

    fun riskFor(action: RafGitFsPlannedAction): RafGitFsOperationRisk = when (action) {
        RafGitFsPlannedAction.NO_OP,
        RafGitFsPlannedAction.CACHE_DOWNLOAD -> RafGitFsOperationRisk.READ_ONLY
        RafGitFsPlannedAction.PIN_OFFLINE,
        RafGitFsPlannedAction.REMOVE_LOCAL_CACHE,
        RafGitFsPlannedAction.CREATE_WORKSPACE,
        RafGitFsPlannedAction.WRITE_WORKSPACE_FILE -> RafGitFsOperationRisk.LOCAL_MUTATION
        in branchActions -> RafGitFsOperationRisk.REMOTE_BRANCH_WRITE
        RafGitFsPlannedAction.DELETE_REMOTE -> RafGitFsOperationRisk.DESTRUCTIVE_REMOTE
    }
}
