package com.rafgittools.rafgitfs.sync

object RafGitFsDiffPlanner {
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
        val steps = diffs.sortedBy { it.path }.mapIndexed { index, diff ->
            toStep(index + 1, diff, requestedAction, workspaceId)
        }.ifEmpty {
            listOf(
                RafGitFsPlanStep(
                    1, RafGitFsPlannedAction.NO_OP, null, RafGitFsOperationRisk.READ_ONLY,
                    baseCommitSha, baseCommitSha, false, true, "NO_DIFFERENCE_OBSERVED"
                )
            )
        }
        val conflicts = diffs.filter { it.conflict }
        val payload = RafGitFsCanonical.planPayload(
            requestId, profileId, repositoryFullName, refName,
            baseCommitSha, steps, conflicts, generatedAt, workspaceId
        )
        return RafGitFsSyncPlan(
            requestId = requestId,
            profileId = profileId,
            repositoryFullName = repositoryFullName,
            refName = refName,
            baseCommitSha = baseCommitSha,
            steps = steps,
            conflicts = conflicts,
            generatedAt = generatedAt,
            planHash = RafGitFsCanonical.sha256(payload),
            workspaceId = workspaceId,
            claimAllowed = false
        )
    }

    private fun toStep(
        order: Int,
        diff: RafGitFsDiffItem,
        requestedAction: RafGitFsPlannedAction,
        workspaceId: String?
    ): RafGitFsPlanStep {
        if (diff.conflict) {
            return RafGitFsPlanStep(
                order, requestedAction, diff.path, riskFor(requestedAction),
                diff.localSha, diff.remoteSha, true, false, "CONFLICT_${diff.kind.name}"
            )
        }
        val risk = riskFor(requestedAction)
        val local = requestedAction in setOf(
            RafGitFsPlannedAction.NO_OP,
            RafGitFsPlannedAction.CACHE_DOWNLOAD,
            RafGitFsPlannedAction.PIN_OFFLINE,
            RafGitFsPlannedAction.REMOVE_LOCAL_CACHE,
            RafGitFsPlannedAction.CREATE_WORKSPACE,
            RafGitFsPlannedAction.WRITE_WORKSPACE_FILE
        )
        val branchWrite = requestedAction in setOf(
            RafGitFsPlannedAction.CREATE_BRANCH,
            RafGitFsPlannedAction.CREATE_COMMIT,
            RafGitFsPlannedAction.PUSH_BRANCH,
            RafGitFsPlannedAction.OPEN_PULL_REQUEST
        )
        val executable = local || (branchWrite && workspaceId != null)
        return RafGitFsPlanStep(
            order, requestedAction, diff.path, risk, diff.localSha, diff.remoteSha,
            requiresApproval = risk != RafGitFsOperationRisk.READ_ONLY,
            executableNow = executable,
            reason = when {
                requestedAction == RafGitFsPlannedAction.NO_OP -> "OBSERVED_EQUAL"
                local -> "LOCAL_CAPABILITY_AVAILABLE"
                branchWrite && workspaceId != null -> "GOVERNED_BRANCH_CAPABILITY_AVAILABLE"
                branchWrite -> "WORKSPACE_REQUIRED"
                else -> "DESTRUCTIVE_REMOTE_PERMANENTLY_BLOCKED"
            }
        )
    }

    fun riskFor(action: RafGitFsPlannedAction): RafGitFsOperationRisk = when (action) {
        RafGitFsPlannedAction.NO_OP,
        RafGitFsPlannedAction.CACHE_DOWNLOAD -> RafGitFsOperationRisk.READ_ONLY
        RafGitFsPlannedAction.PIN_OFFLINE,
        RafGitFsPlannedAction.REMOVE_LOCAL_CACHE,
        RafGitFsPlannedAction.CREATE_WORKSPACE,
        RafGitFsPlannedAction.WRITE_WORKSPACE_FILE -> RafGitFsOperationRisk.LOCAL_MUTATION
        RafGitFsPlannedAction.CREATE_BRANCH,
        RafGitFsPlannedAction.CREATE_COMMIT,
        RafGitFsPlannedAction.PUSH_BRANCH,
        RafGitFsPlannedAction.OPEN_PULL_REQUEST -> RafGitFsOperationRisk.REMOTE_BRANCH_WRITE
        RafGitFsPlannedAction.DELETE_REMOTE -> RafGitFsOperationRisk.DESTRUCTIVE_REMOTE
    }
}
