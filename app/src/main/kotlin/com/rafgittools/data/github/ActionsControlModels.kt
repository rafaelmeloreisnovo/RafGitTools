package com.rafgittools.data.github

import com.google.gson.annotations.SerializedName

/**
 * Read-only provider snapshots. An HTTP 204 mutation is REQUEST_ACCEPTED,
 * never evidence of a successful GitHub Actions run or Android execution.
 */
data class ActionsWorkflowList(
    val total_count: Int? = null,
    val workflows: List<ActionsWorkflow>? = null
)

data class ActionsWorkflow(
    val id: Long,
    val name: String? = null,
    val path: String? = null,
    val state: String? = null
)

data class ActionsRunList(
    val total_count: Int? = null,
    val workflow_runs: List<ActionsRun>? = null
)

data class ActionsRun(
    val id: Long,
    val name: String? = null,
    @SerializedName("workflow_id") val workflowId: Long? = null,
    val status: String? = null,
    val conclusion: String? = null,
    @SerializedName("head_sha") val headSha: String? = null,
    @SerializedName("head_branch") val headBranch: String? = null,
    @SerializedName("run_attempt") val runAttempt: Int? = null,
    @SerializedName("html_url") val htmlUrl: String? = null
)

/** Deliberately bounded. Never store credentials or HTTP request bodies here. */
data class ActionsControlReceipt(
    val repository: String,
    val operation: String,
    val targetId: Long,
    val sourceSha: String,
    val state: String,
    val providerHttpCode: Int,
    val targetRef: String = "TOKEN_VAZIO"
)

/** Simple control-plane policy independent of Android/Compose and safe to unit test. */
object ActionsControlPolicy {
    const val OWNER = "rafaelmeloreisnovo"
    val repositories: List<String> = listOf(
        "RafGitTools",
        "termux-app-rafacodephi",
        "termux-packages",
        "RafPolimata",
        "Vectras-VM-Android"
    )

    fun repositoryAllowed(repository: String) = repository in repositories
    fun workflowAllowed(workflow: ActionsWorkflow) =
        workflow.id > 0 && workflow.state == "active"
    fun runCanCancel(run: ActionsRun) =
        run.id > 0 && run.status in setOf("queued", "requested", "pending", "waiting", "in_progress")
    fun runCanRerun(run: ActionsRun) =
        run.id > 0 && run.status == "completed" && isExactSha(run.headSha)
    fun runCanRerunFailedJobs(run: ActionsRun) =
        runCanRerun(run) && run.conclusion in setOf("failure", "timed_out", "cancelled")
    fun isExactSha(sha: String?) =
        sha != null && Regex("^[0-9a-f]{40}$").matches(sha)
    fun isSafeRef(ref: String) =
        ref.isNotBlank() && ref.length <= 128 &&
            Regex("^[a-zA-Z0-9][a-zA-Z0-9._/-]*$").matches(ref) &&
            !ref.contains("..") && !ref.endsWith("/") &&
            !ref.contains("//") && !ref.startsWith("-")
}
