package com.rafgittools.rafgitfs.assurance

import com.rafgittools.rafgitfs.sync.RafGitFsCanonical

enum class RafGitFsEvidenceLevel {
    IMPLEMENTED_SOURCE,
    OBSERVED_LOCAL,
    OBSERVED_CI,
    OBSERVED_ANDROID,
    TOKEN_VAZIO,
    BLOCKED_BY_POLICY
}

data class RafGitFsCapabilityStatus(
    val capability: String,
    val evidence: RafGitFsEvidenceLevel,
    val claimAllowed: Boolean,
    val receipt: String?,
    val nextVerification: String?
)

data class RafGitFsCloseoutSnapshot(
    val version: String,
    val generatedAt: Long,
    val capabilities: List<RafGitFsCapabilityStatus>,
    val securityControls: Int,
    val unresolvedTokenVazio: Int,
    val closeoutHash: String,
    val claimAllowed: Boolean = false
)

object RafGitFsIndustrialCloseout {
    fun sourceSnapshot(generatedAt: Long = System.currentTimeMillis()): RafGitFsCloseoutSnapshot {
        val capabilities = listOf(
            implemented("architecture_contracts", "PR_300"),
            implemented("room_v6_metadata", "PR_300"),
            implemented("github_readonly_engine", "PR_301"),
            implemented("compose_virtual_browser", "PR_302"),
            implemented("selective_cache_offline", "PR_303"),
            implemented("governed_sync_jobs", "PR_304"),
            implemented("workspace_branch_commit_draft_pr", "PR_305"),
            blocked("direct_protected_branch_write", "BLOCKED_BY_DEFAULT"),
            blocked("remote_delete", "PERMANENT_POLICY_BLOCK"),
            tokenVazio("github_actions_pass", "Run workflows with observable steps and logs"),
            tokenVazio("android_device_execution", "Install and execute on a physical Android device"),
            tokenVazio("production_signing", "Generate and verify a production signing receipt"),
            tokenVazio("external_security_review", "Independent review and threat-model verification"),
            tokenVazio("performance_baseline", "Capture p50/p95/p99 on declared Android hardware")
        )
        val canonical = buildString {
            append("version=1\n")
            append("generatedAt=").append(generatedAt).append('\n')
            capabilities.sortedBy { it.capability }.forEach { capability ->
                append(capability.capability).append('|')
                    .append(capability.evidence.name).append('|')
                    .append(capability.claimAllowed).append('|')
                    .append(capability.receipt.orEmpty()).append('|')
                    .append(capability.nextVerification.orEmpty()).append('\n')
            }
        }
        return RafGitFsCloseoutSnapshot(
            version = "RafGitFS-V1",
            generatedAt = generatedAt,
            capabilities = capabilities,
            securityControls = 11,
            unresolvedTokenVazio = capabilities.count { it.evidence == RafGitFsEvidenceLevel.TOKEN_VAZIO },
            closeoutHash = RafGitFsCanonical.sha256(canonical),
            claimAllowed = false
        )
    }

    private fun implemented(capability: String, receipt: String) = RafGitFsCapabilityStatus(
        capability,
        RafGitFsEvidenceLevel.IMPLEMENTED_SOURCE,
        false,
        receipt,
        "Compile, run and capture an observable receipt"
    )

    private fun tokenVazio(capability: String, next: String) = RafGitFsCapabilityStatus(
        capability,
        RafGitFsEvidenceLevel.TOKEN_VAZIO,
        false,
        null,
        next
    )

    private fun blocked(capability: String, receipt: String) = RafGitFsCapabilityStatus(
        capability,
        RafGitFsEvidenceLevel.BLOCKED_BY_POLICY,
        false,
        receipt,
        null
    )
}
