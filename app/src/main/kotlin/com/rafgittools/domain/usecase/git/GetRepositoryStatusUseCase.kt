package com.rafgittools.domain.usecase.git

import com.rafgittools.domain.model.GitStatus
import com.rafgittools.domain.repository.GitRepository
import com.rafgittools.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for getting the status of a Git repository.
 * 
 * This use case retrieves the current status of a repository including
 * staged, modified, untracked, and conflicting files.
 * 
 * Example usage:
 * ```
 * val result = getRepositoryStatusUseCase(
 *     GetRepositoryStatusParams(repoPath = "/path/to/repo")
 * )
 * result.onSuccess { status ->
 *     println("Branch: ${status.branch}")
 *     println("Modified files: ${status.modified}")
 * }
 * ```
 */
class GetRepositoryStatusUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<GetRepositoryStatusParams, Result<GitStatus>> {
    
    override suspend fun invoke(params: GetRepositoryStatusParams): Result<GitStatus> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        
        return gitRepository.getStatus(params.repoPath)
    }
}

/**
 * Parameters for GetRepositoryStatusUseCase
 * 
 * @property repoPath Path to the local Git repository
 */
data class GetRepositoryStatusParams(
    val repoPath: String
)
