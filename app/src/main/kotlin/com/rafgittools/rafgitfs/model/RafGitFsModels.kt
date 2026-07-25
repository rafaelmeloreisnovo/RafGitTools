package com.rafgittools.rafgitfs.model

/**
 * A configured virtual storage surface backed by GitHub repositories.
 *
 * The Room implementation is intentionally deferred to Prompt 2. This model is
 * persistence-agnostic so database and API adapters can share the same contract.
 */
data class RafGitFsStorageProfile(
    val id: String,
    val displayName: String,
    val provider: RafGitFsProvider = RafGitFsProvider.GITHUB,
    val scope: RafGitFsProfileScope,
    val scopeValue: String,
    val selectedRepositories: Set<String> = emptySet(),
    val defaultRef: String = "main",
    val accessMode: RafGitFsAccessMode = RafGitFsAccessMode.READ_ONLY,
    val cachePolicy: RafGitFsCachePolicy = RafGitFsCachePolicy.ON_DEMAND,
    val writePolicy: RafGitFsWritePolicy = RafGitFsWritePolicy.BLOCKED,
    val protectedBranchPatterns: Set<String> = setOf("main", "master", "release/*"),
    val receiptRequired: Boolean = true,
    val claimAllowed: Boolean = false,
    val enabled: Boolean = true,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

data class RafGitFsRepositoryRef(
    val owner: String,
    val repository: String,
    val ref: String,
    val commitSha: String? = null
) {
    val fullName: String get() = "$owner/$repository"
}

data class RafGitFsVirtualEntry(
    val repository: RafGitFsRepositoryRef,
    val path: String,
    val name: String,
    val type: RafGitFsEntryType,
    val gitSha: String?,
    val sizeBytes: Long?,
    val mimeType: String?,
    val cacheState: RafGitFsCacheState,
    val localPath: String? = null,
    val conflictState: RafGitFsConflictState = RafGitFsConflictState.NONE,
    val indexedAtEpochMs: Long
)

data class RafGitFsOperationRequest(
    val requestId: String,
    val profileId: String,
    val operation: RafGitFsOperationType,
    val target: RafGitFsRepositoryRef?,
    val path: String? = null,
    val expectedRemoteSha: String? = null,
    val humanApproved: Boolean = false,
    val dryRunCompleted: Boolean = false,
    val createdAtEpochMs: Long
)

data class RafGitFsOperationReceipt(
    val receiptId: String,
    val requestId: String,
    val profileId: String,
    val operation: RafGitFsOperationType,
    val terminalPhase: RafGitFsOperationPhase,
    val success: Boolean,
    val riskLevel: RafGitFsRiskLevel,
    val requestSha256: String,
    val resultSha256: String?,
    val remoteBeforeSha: String?,
    val remoteAfterSha: String?,
    val fOk: List<String>,
    val fGap: List<String>,
    val fNext: List<String>,
    val epistemicState: RafGitFsEpistemicState,
    val claimAllowed: Boolean = false,
    val createdAtEpochMs: Long
)

data class RafGitFsPolicyDecision(
    val allowed: Boolean,
    val riskLevel: RafGitFsRiskLevel,
    val reasons: List<String>,
    val requiredNextSteps: List<String>
)
