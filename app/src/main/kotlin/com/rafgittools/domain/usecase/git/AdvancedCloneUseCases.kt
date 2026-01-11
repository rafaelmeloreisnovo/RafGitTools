package com.rafgittools.domain.usecase.git

import com.rafgittools.domain.model.GitRepository
import com.rafgittools.domain.repository.Credentials
import com.rafgittools.domain.repository.GitRepository as IGitRepository
import com.rafgittools.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for shallow cloning a Git repository.
 * Feature #20 from roadmap
 * 
 * Shallow clone is useful when you don't need the full history
 * and want to save bandwidth and disk space.
 */
class CloneShallowUseCase @Inject constructor(
    private val gitRepository: IGitRepository
) : UseCase<CloneShallowParams, Result<GitRepository>> {
    
    override suspend fun invoke(params: CloneShallowParams): Result<GitRepository> {
        if (params.url.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository URL cannot be empty"))
        }
        if (params.localPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Local path cannot be empty"))
        }
        if (params.depth < 1) {
            return Result.failure(IllegalArgumentException("Depth must be at least 1"))
        }
        
        return gitRepository.cloneShallow(
            url = params.url,
            localPath = params.localPath,
            depth = params.depth,
            credentials = params.credentials
        )
    }
}

data class CloneShallowParams(
    val url: String,
    val localPath: String,
    val depth: Int = 1,
    val credentials: Credentials? = null
)

/**
 * Use case for cloning a single branch from a Git repository.
 * Feature #21 from roadmap
 * 
 * Single branch clone is useful when you only need one specific branch
 * and want to minimize the amount of data transferred.
 */
class CloneSingleBranchUseCase @Inject constructor(
    private val gitRepository: IGitRepository
) : UseCase<CloneSingleBranchParams, Result<GitRepository>> {
    
    override suspend fun invoke(params: CloneSingleBranchParams): Result<GitRepository> {
        if (params.url.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository URL cannot be empty"))
        }
        if (params.localPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Local path cannot be empty"))
        }
        if (params.branch.isBlank()) {
            return Result.failure(IllegalArgumentException("Branch name cannot be empty"))
        }
        
        return gitRepository.cloneSingleBranch(
            url = params.url,
            localPath = params.localPath,
            branch = params.branch,
            credentials = params.credentials
        )
    }
}

data class CloneSingleBranchParams(
    val url: String,
    val localPath: String,
    val branch: String,
    val credentials: Credentials? = null
)

/**
 * Use case for cloning a Git repository with submodules.
 * Feature #22 from roadmap
 * 
 * This will clone the repository and automatically initialize
 * and update any submodules in the repository.
 */
class CloneWithSubmodulesUseCase @Inject constructor(
    private val gitRepository: IGitRepository
) : UseCase<CloneWithSubmodulesParams, Result<GitRepository>> {
    
    override suspend fun invoke(params: CloneWithSubmodulesParams): Result<GitRepository> {
        if (params.url.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository URL cannot be empty"))
        }
        if (params.localPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Local path cannot be empty"))
        }
        
        return gitRepository.cloneWithSubmodules(
            url = params.url,
            localPath = params.localPath,
            credentials = params.credentials
        )
    }
}

data class CloneWithSubmodulesParams(
    val url: String,
    val localPath: String,
    val credentials: Credentials? = null
)
