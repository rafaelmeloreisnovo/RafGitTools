package com.rafgittools.domain.usecase.git

import com.rafgittools.domain.model.GitCommit
import com.rafgittools.domain.repository.GitRepository
import com.rafgittools.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for getting commit history from a Git repository.
 * 
 * This use case retrieves the commit history for a repository, optionally
 * filtering by branch and limiting the number of results.
 * 
 * Example usage:
 * ```
 * val result = getCommitHistoryUseCase(
 *     GetCommitHistoryParams(
 *         repoPath = "/path/to/repo",
 *         branch = "main",
 *         limit = 50
 *     )
 * )
 * result.onSuccess { commits ->
 *     commits.forEach { commit ->
 *         println("${commit.sha}: ${commit.message}")
 *     }
 * }
 * ```
 */
class GetCommitHistoryUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<GetCommitHistoryParams, Result<List<GitCommit>>> {
    
    companion object {
        const val DEFAULT_LIMIT = 50
        const val MAX_LIMIT = 1000
    }
    
    override suspend fun invoke(params: GetCommitHistoryParams): Result<List<GitCommit>> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        
        val limit = params.limit.coerceIn(1, MAX_LIMIT)
        
        return gitRepository.getCommits(
            repoPath = params.repoPath,
            branch = params.branch,
            limit = limit
        )
    }
}

/**
 * Parameters for GetCommitHistoryUseCase
 * 
 * @property repoPath Path to the local Git repository
 * @property branch Optional branch name to filter commits
 * @property limit Maximum number of commits to return (default: 50, max: 1000)
 */
data class GetCommitHistoryParams(
    val repoPath: String,
    val branch: String? = null,
    val limit: Int = GetCommitHistoryUseCase.DEFAULT_LIMIT
)
