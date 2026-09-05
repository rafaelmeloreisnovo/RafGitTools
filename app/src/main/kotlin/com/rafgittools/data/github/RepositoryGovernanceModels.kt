package com.rafgittools.data.github

import com.google.gson.annotations.SerializedName

/**
 * Provider-bound models used by the repository governance screen.
 *
 * These models intentionally keep observed provider state separate from desired state.
 * A missing/forbidden field is not interpreted as false; callers must keep it TOKEN_VAZIO.
 */
data class GovernanceRepositorySummary(
    val name: String,
    @SerializedName("full_name") val fullName: String,
    val owner: GovernanceOwner,
    @SerializedName("private") val isPrivate: Boolean,
    val archived: Boolean = false,
    @SerializedName("default_branch") val defaultBranch: String,
    val permissions: GovernancePermissions? = null
)

data class GovernanceOwner(
    val login: String
)

data class GovernancePermissions(
    val admin: Boolean = false,
    val maintain: Boolean = false,
    val push: Boolean = false,
    val triage: Boolean = false,
    val pull: Boolean = true
)

data class GovernanceRepositoryDetails(
    val name: String,
    @SerializedName("full_name") val fullName: String,
    val owner: GovernanceOwner,
    val archived: Boolean = false,
    @SerializedName("private") val isPrivate: Boolean,
    @SerializedName("default_branch") val defaultBranch: String,
    @SerializedName("has_issues") val hasIssues: Boolean = true,
    @SerializedName("has_projects") val hasProjects: Boolean = true,
    @SerializedName("has_wiki") val hasWiki: Boolean = true,
    @SerializedName("has_discussions") val hasDiscussions: Boolean = false,
    @SerializedName("allow_merge_commit") val allowMergeCommit: Boolean = true,
    @SerializedName("allow_squash_merge") val allowSquashMerge: Boolean = true,
    @SerializedName("allow_rebase_merge") val allowRebaseMerge: Boolean = true,
    @SerializedName("delete_branch_on_merge") val deleteBranchOnMerge: Boolean = false,
    @SerializedName("web_commit_signoff_required") val webCommitSignoffRequired: Boolean = false,
    @SerializedName("security_and_analysis") val securityAndAnalysis: GovernanceSecurityAndAnalysis? = null,
    val permissions: GovernancePermissions? = null
)

data class GovernanceSecurityAndAnalysis(
    @SerializedName("advanced_security") val advancedSecurity: GovernanceFeatureStatus? = null,
    @SerializedName("secret_scanning") val secretScanning: GovernanceFeatureStatus? = null,
    @SerializedName("secret_scanning_push_protection") val secretScanningPushProtection: GovernanceFeatureStatus? = null
)

data class GovernanceFeatureStatus(
    val status: String? = null
) {
    fun isEnabledOrNull(): Boolean? = when (status?.lowercase()) {
        "enabled" -> true
        "disabled" -> false
        else -> null
    }
}

data class UpdateRepositoryGovernanceRequest(
    @SerializedName("has_issues") val hasIssues: Boolean? = null,
    @SerializedName("has_projects") val hasProjects: Boolean? = null,
    @SerializedName("has_wiki") val hasWiki: Boolean? = null,
    @SerializedName("has_discussions") val hasDiscussions: Boolean? = null,
    @SerializedName("allow_merge_commit") val allowMergeCommit: Boolean? = null,
    @SerializedName("allow_squash_merge") val allowSquashMerge: Boolean? = null,
    @SerializedName("allow_rebase_merge") val allowRebaseMerge: Boolean? = null,
    @SerializedName("delete_branch_on_merge") val deleteBranchOnMerge: Boolean? = null,
    @SerializedName("web_commit_signoff_required") val webCommitSignoffRequired: Boolean? = null,
    @SerializedName("security_and_analysis") val securityAndAnalysis: UpdateGovernanceSecurityAndAnalysis? = null
)

data class UpdateGovernanceSecurityAndAnalysis(
    @SerializedName("advanced_security") val advancedSecurity: GovernanceFeatureStatus? = null,
    @SerializedName("secret_scanning") val secretScanning: GovernanceFeatureStatus? = null,
    @SerializedName("secret_scanning_push_protection") val secretScanningPushProtection: GovernanceFeatureStatus? = null
)

data class BranchProtectionRequest(
    @SerializedName("required_status_checks") val requiredStatusChecks: RequiredStatusChecksRequest? = null,
    @SerializedName("enforce_admins") val enforceAdmins: Boolean = true,
    @SerializedName("required_pull_request_reviews") val requiredPullRequestReviews: PullRequestReviewProtectionRequest = PullRequestReviewProtectionRequest(),
    val restrictions: BranchRestrictionsRequest? = null,
    @SerializedName("required_linear_history") val requiredLinearHistory: Boolean = false,
    @SerializedName("allow_force_pushes") val allowForcePushes: Boolean = false,
    @SerializedName("allow_deletions") val allowDeletions: Boolean = false,
    @SerializedName("block_creations") val blockCreations: Boolean = false,
    @SerializedName("required_conversation_resolution") val requiredConversationResolution: Boolean = true,
    @SerializedName("lock_branch") val lockBranch: Boolean = false,
    @SerializedName("allow_fork_syncing") val allowForkSyncing: Boolean = false
)

data class RequiredStatusChecksRequest(
    val strict: Boolean = true,
    val contexts: List<String> = emptyList()
)

data class PullRequestReviewProtectionRequest(
    @SerializedName("dismiss_stale_reviews") val dismissStaleReviews: Boolean = true,
    @SerializedName("require_code_owner_reviews") val requireCodeOwnerReviews: Boolean = false,
    @SerializedName("required_approving_review_count") val requiredApprovingReviewCount: Int = 1,
    @SerializedName("require_last_push_approval") val requireLastPushApproval: Boolean = false
)

data class BranchRestrictionsRequest(
    val users: List<String> = emptyList(),
    val teams: List<String> = emptyList(),
    val apps: List<String> = emptyList()
)

data class BranchProtectionSnapshot(
    @SerializedName("required_pull_request_reviews") val requiredPullRequestReviews: PullRequestReviewProtectionSnapshot? = null,
    @SerializedName("enforce_admins") val enforceAdmins: EnabledFlag? = null,
    @SerializedName("required_linear_history") val requiredLinearHistory: EnabledFlag? = null,
    @SerializedName("allow_force_pushes") val allowForcePushes: EnabledFlag? = null,
    @SerializedName("allow_deletions") val allowDeletions: EnabledFlag? = null,
    @SerializedName("required_conversation_resolution") val requiredConversationResolution: EnabledFlag? = null
)

data class PullRequestReviewProtectionSnapshot(
    @SerializedName("dismiss_stale_reviews") val dismissStaleReviews: Boolean = false,
    @SerializedName("require_code_owner_reviews") val requireCodeOwnerReviews: Boolean = false,
    @SerializedName("required_approving_review_count") val requiredApprovingReviewCount: Int = 0,
    @SerializedName("require_last_push_approval") val requireLastPushApproval: Boolean = false
)

data class EnabledFlag(
    val enabled: Boolean = false
)
