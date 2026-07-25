package com.rafgittools.rafgitfs.write

import android.util.Base64
import com.rafgittools.rafgitfs.cache.RafGitFsChecksums
import com.rafgittools.rafgitfs.data.StagedOperationDao
import com.rafgittools.rafgitfs.data.StagedOperationEntity
import com.rafgittools.rafgitfs.data.SyncConflictDao
import com.rafgittools.rafgitfs.data.SyncConflictEntity
import com.rafgittools.rafgitfs.data.WorkspaceDao
import com.rafgittools.rafgitfs.remote.RafGitFsGithubApiService
import com.rafgittools.rafgitfs.sync.RafGitFsCanonical
import com.rafgittools.rafgitfs.sync.RafGitFsExecutionOutcome
import com.rafgittools.rafgitfs.sync.RafGitFsPlanStep
import com.rafgittools.rafgitfs.sync.RafGitFsPlannedAction
import com.rafgittools.rafgitfs.sync.RafGitFsRemoteWriteCapability
import com.rafgittools.rafgitfs.sync.RafGitFsSyncPlan
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RafGitFsGithubBranchWriter @Inject constructor(
    private val writeApi: RafGitFsGithubWriteApiService,
    private val readApi: RafGitFsGithubApiService,
    private val workspaceDao: WorkspaceDao,
    private val stagedDao: StagedOperationDao,
    private val conflictDao: SyncConflictDao,
    private val workspaceStore: RafGitFsWorkspaceStore
) : RafGitFsRemoteWriteCapability {

    override suspend fun execute(
        plan: RafGitFsSyncPlan,
        step: RafGitFsPlanStep
    ): RafGitFsExecutionOutcome {
        if (step.risk.name != "REMOTE_BRANCH_WRITE") return blocked(step, "REMOTE_RISK_REQUIRED")
        val workspaceId = plan.workspaceId ?: return blocked(step, "WORKSPACE_REQUIRED")
        val workspace = workspaceDao.getById(workspaceId) ?: return blocked(step, "WORKSPACE_NOT_FOUND")
        if (workspace.repositoryFullName != plan.repositoryFullName || workspace.baseRef != plan.refName) {
            return blocked(step, "WORKSPACE_PLAN_TARGET_MISMATCH")
        }
        if (plan.baseCommitSha.isNullOrBlank()) return blocked(step, "BASE_COMMIT_SHA_REQUIRED")
        if (plan.claimAllowed || workspace.claimAllowed) return blocked(step, "CLAIM_PROMOTION_BLOCKED")
        return when (step.action) {
            RafGitFsPlannedAction.CREATE_BRANCH -> createBranch(plan, step)
            RafGitFsPlannedAction.CREATE_COMMIT -> createCommit(plan, step)
            RafGitFsPlannedAction.PUSH_BRANCH -> pushBranch(plan, step)
            RafGitFsPlannedAction.OPEN_PULL_REQUEST -> openPullRequest(plan, step)
            else -> blocked(step, "UNSUPPORTED_REMOTE_STEP:${step.action.name}")
        }
    }

    suspend fun rollbackToBase(
        plan: RafGitFsSyncPlan,
        confirmation: String
    ): RafGitFsExecutionOutcome {
        val step = plan.steps.lastOrNull()
            ?: return blocked(fallbackStep(), "PLAN_HAS_NO_STEPS")
        if (confirmation != "ROLLBACK ${plan.planHash.take(12)}") {
            return blocked(step, "ROLLBACK_CONFIRMATION_MISMATCH")
        }
        val workspaceId = plan.workspaceId ?: return blocked(step, "WORKSPACE_REQUIRED")
        val workspace = workspaceDao.getById(workspaceId) ?: return blocked(step, "WORKSPACE_NOT_FOUND")
        val branch = workspace.branchName ?: return blocked(step, "BRANCH_NOT_CREATED")
        val baseSha = plan.baseCommitSha ?: return blocked(step, "BASE_COMMIT_SHA_REQUIRED")
        val (owner, repo) = splitRepo(plan.repositoryFullName) ?: return blocked(step, "INVALID_REPOSITORY")
        val current = readApi.resolveCommit(owner, repo, branch)
        val currentBody = current.body()
        if (!current.isSuccessful || currentBody == null) return httpFailure(step, "ROLLBACK_BRANCH_READ", current.code())
        if (currentBody.sha.equals(baseSha, ignoreCase = true)) {
            return observed(step, "ALREADY_AT_BASE", baseSha, branch)
        }
        val baseCommit = writeApi.getGitCommit(owner, repo, baseSha)
        val base = baseCommit.body()
        if (!baseCommit.isSuccessful || base == null) return httpFailure(step, "ROLLBACK_BASE_TREE", baseCommit.code())
        val revertCommit = writeApi.createCommit(
            owner, repo,
            RafGitFsCreateCommitRequest(
                message = "revert(rafgitfs): rollback ${plan.requestId.take(8)} to ${baseSha.take(12)}",
                tree = base.tree.sha,
                parents = listOf(currentBody.sha)
            )
        )
        val revert = revertCommit.body()
        if (!revertCommit.isSuccessful || revert == null) return httpFailure(step, "ROLLBACK_COMMIT", revertCommit.code())
        val pushed = writeApi.updateBranchRef(
            owner, repo, encodeBranch(branch), RafGitFsUpdateRefRequest(revert.sha, force = false)
        )
        if (!pushed.isSuccessful) return httpFailure(step, "ROLLBACK_PUSH", pushed.code())
        workspaceDao.upsert(
            workspace.copy(state = "ROLLED_BACK", updatedAt = System.currentTimeMillis(), claimAllowed = false)
        )
        upsertMeta(plan, branch, baseSha, revert.sha, "ROLLED_BACK")
        return observed(step, "ROLLED_BACK", revert.sha, branch)
    }

    private suspend fun createBranch(plan: RafGitFsSyncPlan, step: RafGitFsPlanStep): RafGitFsExecutionOutcome {
        val workspace = workspaceDao.getById(plan.workspaceId!!) ?: return blocked(step, "WORKSPACE_NOT_FOUND")
        val baseSha = plan.baseCommitSha!!
        val (owner, repo) = splitRepo(plan.repositoryFullName) ?: return blocked(step, "INVALID_REPOSITORY")
        val currentBase = readApi.resolveCommit(owner, repo, plan.refName)
        val current = currentBase.body()
        if (!currentBase.isSuccessful || current == null) return httpFailure(step, "BASE_REF_READ", currentBase.code())
        if (!current.sha.equals(baseSha, ignoreCase = true)) {
            recordBaseConflict(plan, current.sha)
            workspaceStore.setConflict(plan.workspaceId)
            return blocked(step, "BASE_REF_MOVED:${current.sha.take(12)}")
        }
        val existing = metadata(plan.workspaceId)
        val branch = existing?.path ?: workspace.branchName ?: branchName(plan)
        if (isProtected(branch) || branch == plan.refName) return blocked(step, "PROTECTED_BRANCH_TARGET")
        if (existing?.state in setOf("BRANCH_CREATED", "COMMIT_READY", "PUSHED") || existing?.state?.startsWith("PR_OPEN:") == true) {
            return observed(step, "BRANCH_ALREADY_RECORDED", existing.localSha ?: baseSha, branch)
        }
        val response = writeApi.createRef(
            owner, repo, RafGitFsCreateRefRequest("refs/heads/$branch", baseSha)
        )
        val created = response.body()
        if (!response.isSuccessful || created == null) {
            if (response.code() == 422) {
                val resolved = readApi.resolveCommit(owner, repo, branch)
                val body = resolved.body()
                if (resolved.isSuccessful && body != null && body.sha.equals(baseSha, ignoreCase = true)) {
                    workspaceDao.upsert(workspace.copy(branchName = branch, state = "BRANCH_CREATED", updatedAt = System.currentTimeMillis(), claimAllowed = false))
                    upsertMeta(plan, branch, baseSha, baseSha, "BRANCH_CREATED")
                    return observed(step, "BRANCH_IDEMPOTENT", baseSha, branch)
                }
            }
            return httpFailure(step, "CREATE_REF", response.code())
        }
        workspaceDao.upsert(workspace.copy(branchName = branch, state = "BRANCH_CREATED", updatedAt = System.currentTimeMillis(), claimAllowed = false))
        upsertMeta(plan, branch, baseSha, created.`object`.sha, "BRANCH_CREATED")
        return observed(step, "BRANCH_CREATED", created.`object`.sha, branch)
    }

    private suspend fun createCommit(plan: RafGitFsSyncPlan, step: RafGitFsPlanStep): RafGitFsExecutionOutcome {
        val workspaceId = plan.workspaceId!!
        val meta = metadata(workspaceId) ?: return blocked(step, "BRANCH_STEP_REQUIRED")
        if (meta.state in setOf("COMMIT_READY", "PUSHED") || meta.state.startsWith("PR_OPEN:")) {
            return observed(step, "COMMIT_ALREADY_RECORDED", meta.localSha, meta.path)
        }
        val baseSha = plan.baseCommitSha!!
        val branch = meta.path ?: return blocked(step, "BRANCH_METADATA_MISSING")
        val files = try { workspaceStore.stagedFiles(workspaceId) }
        catch (error: Exception) { return blocked(step, RafGitFsCanonical.sanitize(error.message).orEmpty()) }
        if (files.isEmpty()) return blocked(step, "NO_STAGED_FILES")
        val (owner, repo) = splitRepo(plan.repositoryFullName) ?: return blocked(step, "INVALID_REPOSITORY")
        val baseResponse = writeApi.getGitCommit(owner, repo, baseSha)
        val base = baseResponse.body()
        if (!baseResponse.isSuccessful || base == null) return httpFailure(step, "BASE_COMMIT_READ", baseResponse.code())
        val treeEntries = mutableListOf<RafGitFsTreeEntryRequest>()
        for (file in files.sortedBy { it.path }) {
            val encoded = Base64.encodeToString(file.bytes, Base64.NO_WRAP)
            val blobResponse = writeApi.createBlob(owner, repo, RafGitFsCreateBlobRequest(encoded))
            val blob = blobResponse.body()
            if (!blobResponse.isSuccessful || blob == null) return httpFailure(step, "CREATE_BLOB:${file.path}", blobResponse.code())
            if (!RafGitFsChecksums.verifyGitBlob(file.bytes, blob.sha)) {
                return blocked(step, "REMOTE_BLOB_SHA_MISMATCH:${file.path}")
            }
            treeEntries += RafGitFsTreeEntryRequest(file.path, file.mode, "blob", blob.sha)
        }
        val treeResponse = writeApi.createTree(owner, repo, RafGitFsCreateTreeRequest(base.tree.sha, treeEntries))
        val tree = treeResponse.body()
        if (!treeResponse.isSuccessful || tree == null) return httpFailure(step, "CREATE_TREE", treeResponse.code())
        val commitResponse = writeApi.createCommit(
            owner, repo,
            RafGitFsCreateCommitRequest(
                message = "feat(rafgitfs): workspace ${workspaceId.take(8)}\n\nPlan: ${plan.planHash}",
                tree = tree.sha,
                parents = listOf(baseSha)
            )
        )
        val commit = commitResponse.body()
        if (!commitResponse.isSuccessful || commit == null) return httpFailure(step, "CREATE_COMMIT", commitResponse.code())
        upsertMeta(plan, branch, baseSha, commit.sha, "COMMIT_READY")
        return observed(step, "COMMIT_READY", commit.sha, branch)
    }

    private suspend fun pushBranch(plan: RafGitFsSyncPlan, step: RafGitFsPlanStep): RafGitFsExecutionOutcome {
        val meta = metadata(plan.workspaceId!!) ?: return blocked(step, "COMMIT_STEP_REQUIRED")
        val branch = meta.path ?: return blocked(step, "BRANCH_METADATA_MISSING")
        val commitSha = meta.localSha ?: return blocked(step, "COMMIT_SHA_MISSING")
        if (meta.state == "PUSHED" || meta.state.startsWith("PR_OPEN:")) {
            return observed(step, "BRANCH_ALREADY_PUSHED", commitSha, branch)
        }
        if (meta.state != "COMMIT_READY") return blocked(step, "COMMIT_NOT_READY:${meta.state}")
        val (owner, repo) = splitRepo(plan.repositoryFullName) ?: return blocked(step, "INVALID_REPOSITORY")
        val response = writeApi.updateBranchRef(
            owner, repo, encodeBranch(branch), RafGitFsUpdateRefRequest(commitSha, force = false)
        )
        val pushed = response.body()
        if (!response.isSuccessful || pushed == null) return httpFailure(step, "PUSH_BRANCH", response.code())
        upsertMeta(plan, branch, plan.baseCommitSha, pushed.`object`.sha, "PUSHED")
        return observed(step, "PUSHED", pushed.`object`.sha, branch)
    }

    private suspend fun openPullRequest(plan: RafGitFsSyncPlan, step: RafGitFsPlanStep): RafGitFsExecutionOutcome {
        val meta = metadata(plan.workspaceId!!) ?: return blocked(step, "PUSH_STEP_REQUIRED")
        val branch = meta.path ?: return blocked(step, "BRANCH_METADATA_MISSING")
        if (meta.state.startsWith("PR_OPEN:")) {
            return observed(step, meta.state, meta.localSha, branch)
        }
        if (meta.state != "PUSHED") return blocked(step, "BRANCH_NOT_PUSHED:${meta.state}")
        val (owner, repo) = splitRepo(plan.repositoryFullName) ?: return blocked(step, "INVALID_REPOSITORY")
        val response = writeApi.openPullRequest(
            owner, repo,
            RafGitFsOpenPullRequestRequest(
                title = "RafGitFS: ${plan.workspaceId.take(8)}",
                body = "Governed RafGitFS workspace.\n\nPlan: `${plan.planHash}`\nRequest: `${plan.requestId}`\nclaim_allowed=false",
                head = branch,
                base = plan.refName,
                draft = true,
                maintainerCanModify = true
            )
        )
        val pull = response.body()
        if (!response.isSuccessful || pull == null) return httpFailure(step, "OPEN_PULL_REQUEST", response.code())
        workspaceStore.setPublished(plan.workspaceId, branch)
        upsertMeta(plan, branch, plan.baseCommitSha, pull.head.sha, "PR_OPEN:${pull.number}")
        return observed(step, "PR_OPEN:${pull.number}", pull.head.sha, branch)
    }

    private suspend fun recordBaseConflict(plan: RafGitFsSyncPlan, currentSha: String) {
        conflictDao.upsert(
            SyncConflictEntity(
                conflictId = "${plan.requestId}:base",
                jobId = null,
                workspaceId = plan.workspaceId,
                repositoryFullName = plan.repositoryFullName,
                refName = plan.refName,
                path = "__BASE_BRANCH__",
                conflictState = "BASE_REF_MOVED",
                localSha = plan.baseCommitSha,
                remoteSha = currentSha,
                resolution = null,
                detectedAt = System.currentTimeMillis(),
                resolvedAt = null
            )
        )
    }

    private suspend fun metadata(workspaceId: String): StagedOperationEntity? =
        stagedDao.listForWorkspace(workspaceId).firstOrNull { it.operationType == "PUBLISH_META" }

    private suspend fun upsertMeta(
        plan: RafGitFsSyncPlan,
        branch: String,
        baseSha: String?,
        currentSha: String?,
        state: String
    ) {
        stagedDao.upsert(
            StagedOperationEntity(
                operationId = "${plan.workspaceId}:publish",
                jobId = null,
                workspaceId = plan.workspaceId,
                operationType = "PUBLISH_META",
                repositoryFullName = plan.repositoryFullName,
                refName = plan.refName,
                path = branch,
                baseSha = baseSha,
                localSha = currentSha,
                payloadHash = plan.planHash,
                state = state,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    private fun branchName(plan: RafGitFsSyncPlan): String {
        val base = plan.refName.lowercase().replace(Regex("[^a-z0-9._-]+"), "-").trim('-').take(24)
        return "rafgitfs/${base.ifBlank { "base" }}-${plan.requestId.take(8)}"
    }

    private fun isProtected(branch: String): Boolean = branch.lowercase() in setOf(
        "main", "master", "develop", "development", "production", "release"
    ) || !branch.startsWith("rafgitfs/")

    private fun encodeBranch(branch: String): String = branch.split('/').joinToString("%2F") {
        URLEncoder.encode(it, "UTF-8").replace("+", "%20")
    }

    private fun splitRepo(fullName: String): Pair<String, String>? {
        val parts = fullName.split('/', limit = 2)
        return if (parts.size == 2 && parts.all { it.isNotBlank() }) parts[0] to parts[1] else null
    }

    private fun observed(step: RafGitFsPlanStep, result: String, sha: String?, detail: String?) =
        RafGitFsExecutionOutcome(step, result, "OBSERVED", sha, false, detail)

    private fun blocked(step: RafGitFsPlanStep, detail: String) =
        RafGitFsExecutionOutcome(step, "BLOCKED", "TOKEN_VAZIO", null, false, detail)

    private fun httpFailure(step: RafGitFsPlanStep, stage: String, code: Int) =
        RafGitFsExecutionOutcome(
            step, "FAILED", if (code in 400..499) "TOKEN_VAZIO" else "ERROR",
            null, code == 408 || code == 429 || code >= 500, "$stage:HTTP_$code"
        )

    private fun fallbackStep() = RafGitFsPlanStep(
        0, RafGitFsPlannedAction.NO_OP, null,
        com.rafgittools.rafgitfs.sync.RafGitFsOperationRisk.REMOTE_BRANCH_WRITE,
        null, null, true, false, "FALLBACK"
    )
}
