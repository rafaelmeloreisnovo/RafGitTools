package com.rafgittools.rafgitfs.assurance

import com.rafgittools.rafgitfs.data.StagedOperationDao
import com.rafgittools.rafgitfs.data.SyncConflictDao
import com.rafgittools.rafgitfs.data.WorkspaceDao
import com.rafgittools.rafgitfs.sync.RafGitFsSyncPlan
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RafGitFsRuntimeSecurityGate @Inject constructor(
    private val workspaceDao: WorkspaceDao,
    private val stagedDao: StagedOperationDao,
    private val conflictDao: SyncConflictDao
) {
    /** Called only after RafGitFsGovernedSyncEngine validates exact approval. */
    suspend fun assessAfterExactApproval(plan: RafGitFsSyncPlan): RafGitFsSecurityAssessment {
        val workspaceId = plan.workspaceId
        val workspace = workspaceId?.let { workspaceDao.getById(it) }
        val staged = workspaceId?.let { stagedDao.listForWorkspace(it) }.orEmpty()
            .filter { it.state == "STAGED" && it.operationType.startsWith("UPSERT_FILE:") }
        val unresolved = workspaceId?.let { id ->
            conflictDao.getByWorkspace(id).count { it.resolvedAt == null }
        } ?: 0
        val generatedBranch = workspace?.branchName ?: plan.generatedBranchName()
        return RafGitFsSecurityPolicy.assessPublication(
            RafGitFsPublicationContext(
                generatedBranch = generatedBranch,
                baseBranch = plan.refName,
                planHash = plan.planHash,
                approvalExact = true,
                unresolvedConflicts = unresolved,
                stagedFileCount = staged.size,
                forcePush = false,
                draftPullRequest = true,
                claimAllowed = plan.claimAllowed || (workspace?.claimAllowed == true),
                secretsPersistedInRoom = false,
                workspacePrivate = workspace?.localRoot?.contains("rafgitfs-workspaces-v1") == true
            )
        )
    }

    private fun RafGitFsSyncPlan.generatedBranchName(): String? {
        if (workspaceId == null || requestId.length < 8) return null
        val base = refName.lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "-")
            .trim('-')
            .take(24)
            .ifBlank { "base" }
        return "rafgitfs/$base-${requestId.take(8)}"
    }
}
