package com.rafgittools.domain.usecase.git

import com.rafgittools.domain.model.GitCommit
import com.rafgittools.domain.repository.GitRepository
import com.rafgittools.domain.repository.ResetMode
import com.rafgittools.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for starting a rebase operation
 * Features #163-165 from roadmap
 */
class RebaseUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<RebaseParams, Result<Unit>> {
    
    override suspend fun invoke(params: RebaseParams): Result<Unit> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (params.upstream.isBlank()) {
            return Result.failure(IllegalArgumentException("Upstream branch cannot be empty"))
        }
        return gitRepository.rebase(params.repoPath, params.upstream)
    }
}

data class RebaseParams(
    val repoPath: String,
    val upstream: String
)

/**
 * Use case for continuing a rebase after resolving conflicts
 * Feature #165 from roadmap
 */
class RebaseContinueUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<RebaseContinueParams, Result<Unit>> {
    
    override suspend fun invoke(params: RebaseContinueParams): Result<Unit> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.rebaseContinue(params.repoPath)
    }
}

data class RebaseContinueParams(
    val repoPath: String
)

/**
 * Use case for aborting a rebase operation
 * Feature #165 from roadmap
 */
class RebaseAbortUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<RebaseAbortParams, Result<Unit>> {
    
    override suspend fun invoke(params: RebaseAbortParams): Result<Unit> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.rebaseAbort(params.repoPath)
    }
}

data class RebaseAbortParams(
    val repoPath: String
)

/**
 * Use case for skipping current commit during rebase
 * Feature #165 from roadmap
 */
class RebaseSkipUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<RebaseSkipParams, Result<Unit>> {
    
    override suspend fun invoke(params: RebaseSkipParams): Result<Unit> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.rebaseSkip(params.repoPath)
    }
}

data class RebaseSkipParams(
    val repoPath: String
)

/**
 * Use case for cherry-picking a commit
 * Features #166-167 from roadmap
 */
class CherryPickUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<CherryPickParams, Result<GitCommit>> {
    
    override suspend fun invoke(params: CherryPickParams): Result<GitCommit> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (params.commitSha.isBlank()) {
            return Result.failure(IllegalArgumentException("Commit SHA cannot be empty"))
        }
        return gitRepository.cherryPick(params.repoPath, params.commitSha)
    }
}

data class CherryPickParams(
    val repoPath: String,
    val commitSha: String
)

/**
 * Use case for continuing cherry-pick after resolving conflicts
 * Feature #167 from roadmap
 */
class CherryPickContinueUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<CherryPickContinueParams, Result<Unit>> {
    
    override suspend fun invoke(params: CherryPickContinueParams): Result<Unit> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.cherryPickContinue(params.repoPath)
    }
}

data class CherryPickContinueParams(
    val repoPath: String
)

/**
 * Use case for aborting cherry-pick operation
 * Feature from roadmap
 */
class CherryPickAbortUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<CherryPickAbortParams, Result<Unit>> {
    
    override suspend fun invoke(params: CherryPickAbortParams): Result<Unit> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.cherryPickAbort(params.repoPath)
    }
}

data class CherryPickAbortParams(
    val repoPath: String
)

/**
 * Use case for resetting to a specific commit
 * From roadmap
 */
class ResetUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<ResetParams, Result<Unit>> {
    
    override suspend fun invoke(params: ResetParams): Result<Unit> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (params.commitSha.isBlank()) {
            return Result.failure(IllegalArgumentException("Commit SHA cannot be empty"))
        }
        return gitRepository.reset(params.repoPath, params.commitSha, params.mode)
    }
}

data class ResetParams(
    val repoPath: String,
    val commitSha: String,
    val mode: ResetMode = ResetMode.MIXED
)

/**
 * Use case for reverting a commit
 * From roadmap
 */
class RevertCommitUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<RevertCommitParams, Result<GitCommit>> {
    
    override suspend fun invoke(params: RevertCommitParams): Result<GitCommit> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (params.commitSha.isBlank()) {
            return Result.failure(IllegalArgumentException("Commit SHA cannot be empty"))
        }
        return gitRepository.revert(params.repoPath, params.commitSha)
    }
}

data class RevertCommitParams(
    val repoPath: String,
    val commitSha: String
)
