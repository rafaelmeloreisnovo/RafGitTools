package com.rafgittools.rafgitfs.model

/**
 * Closed vocabularies for RafGitFS V1.
 *
 * V1 is architectural and read-only: these values describe future states without
 * enabling remote mutation, background mounting, or protected-branch writes.
 */
enum class RafGitFsProvider { GITHUB }

enum class RafGitFsProfileScope {
    AUTHENTICATED_USER,
    ORGANIZATION,
    SELECTED_REPOSITORIES
}

enum class RafGitFsAccessMode {
    READ_ONLY,
    GOVERNED_WORKSPACE
}

enum class RafGitFsCachePolicy {
    METADATA_ONLY,
    ON_DEMAND,
    SELECTIVE_OFFLINE
}

enum class RafGitFsWritePolicy {
    BLOCKED,
    BRANCH_AND_PULL_REQUEST,
    DIRECT_COMMIT
}

enum class RafGitFsEntryType {
    FILE,
    DIRECTORY,
    SYMLINK,
    SUBMODULE
}

enum class RafGitFsCacheState {
    REMOTE_ONLY,
    METADATA_CACHED,
    CONTENT_CACHED,
    PINNED_OFFLINE,
    STALE,
    CORRUPTED,
    TOKEN_VAZIO
}

enum class RafGitFsSyncState {
    IDLE,
    SCANNING,
    DIFF_READY,
    PLAN_READY,
    APPROVAL_REQUIRED,
    EXECUTING,
    PAUSED,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    TOKEN_VAZIO
}

enum class RafGitFsConflictState {
    NONE,
    LOCAL_CHANGED,
    REMOTE_CHANGED,
    BOTH_CHANGED,
    REMOTE_DELETED,
    LOCAL_DELETED,
    SHA_MISMATCH,
    TOKEN_VAZIO
}

enum class RafGitFsOperationType {
    LIST_REPOSITORIES,
    LIST_REFS,
    LIST_TREE,
    READ_CONTENT,
    CACHE_CONTENT,
    PIN_OFFLINE,
    CREATE_WORKSPACE,
    STAGE_CHANGE,
    CREATE_BRANCH,
    COMMIT,
    PUSH,
    OPEN_PULL_REQUEST,
    DELETE
}

enum class RafGitFsOperationPhase {
    REQUEST,
    SCAN,
    DIFF,
    PLAN,
    DRY_RUN,
    APPROVE,
    EXECUTE,
    RECEIPT
}

enum class RafGitFsEpistemicState {
    OBSERVED,
    CONVENTION,
    HYPOTHESIS,
    TOKEN_VAZIO
}

enum class RafGitFsRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
