package com.rafgittools.domain.usecase.git

import com.rafgittools.domain.model.GitBranch
import com.rafgittools.domain.repository.GitRepository
import com.rafgittools.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for getting all branches in a Git repository.
 * 
 * This use case retrieves all local and remote branches from the repository.
 * 
 * Example usage:
 * ```
 * val result = getBranchesUseCase(
 *     GetBranchesParams(repoPath = "/path/to/repo")
 * )
 * result.onSuccess { branches ->
 *     branches.forEach { branch ->
 *         println("${branch.shortName} (current: ${branch.isCurrent})")
 *     }
 * }
 * ```
 */
class GetBranchesUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<GetBranchesParams, Result<List<GitBranch>>> {
    
    override suspend fun invoke(params: GetBranchesParams): Result<List<GitBranch>> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        
        return gitRepository.getBranches(params.repoPath)
    }
}

/**
 * Parameters for GetBranchesUseCase
 * 
 * @property repoPath Path to the local Git repository
 */
data class GetBranchesParams(
    val repoPath: String
)
