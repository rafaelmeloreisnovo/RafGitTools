package com.rafgittools.domain.usecase.git

import com.rafgittools.domain.model.GitStash
import com.rafgittools.domain.repository.GitRepository
import com.rafgittools.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for listing all stashes in a repository
 * Feature #40 from roadmap
 */
class ListStashesUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<ListStashesParams, Result<List<GitStash>>> {
    
    override suspend fun invoke(params: ListStashesParams): Result<List<GitStash>> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.listStashes(params.repoPath)
    }
}

data class ListStashesParams(
    val repoPath: String
)

/**
 * Use case for creating a new stash
 * Feature #40 from roadmap
 */
class CreateStashUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<CreateStashParams, Result<GitStash>> {
    
    override suspend fun invoke(params: CreateStashParams): Result<GitStash> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.stash(
            repoPath = params.repoPath,
            message = params.message,
            includeUntracked = params.includeUntracked
        )
    }
}

data class CreateStashParams(
    val repoPath: String,
    val message: String? = null,
    val includeUntracked: Boolean = false
)

/**
 * Use case for applying a stash
 * Feature #40 from roadmap
 */
class ApplyStashUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<ApplyStashParams, Result<Unit>> {
    
    override suspend fun invoke(params: ApplyStashParams): Result<Unit> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.stashApply(params.repoPath, params.stashRef)
    }
}

data class ApplyStashParams(
    val repoPath: String,
    val stashRef: String? = null
)

/**
 * Use case for popping a stash (apply and drop)
 * Feature #40 from roadmap
 */
class PopStashUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<PopStashParams, Result<Unit>> {
    
    override suspend fun invoke(params: PopStashParams): Result<Unit> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.stashPop(params.repoPath, params.stashRef)
    }
}

data class PopStashParams(
    val repoPath: String,
    val stashRef: String? = null
)

/**
 * Use case for dropping a stash
 * Feature #40 from roadmap
 */
class DropStashUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<DropStashParams, Result<Unit>> {
    
    override suspend fun invoke(params: DropStashParams): Result<Unit> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.stashDrop(params.repoPath, params.stashIndex)
    }
}

data class DropStashParams(
    val repoPath: String,
    val stashIndex: Int = 0
)

/**
 * Use case for clearing all stashes
 * Feature #40 from roadmap
 */
class ClearStashesUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<ClearStashesParams, Result<Unit>> {
    
    override suspend fun invoke(params: ClearStashesParams): Result<Unit> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.stashClear(params.repoPath)
    }
}

data class ClearStashesParams(
    val repoPath: String
)
