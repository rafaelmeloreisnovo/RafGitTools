package com.rafgittools.domain.usecase.git

import com.rafgittools.domain.repository.Credentials
import com.rafgittools.domain.repository.GitRepository
import com.rafgittools.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for pulling changes from a remote repository.
 * 
 * This use case handles pulling commits from a remote repository
 * and merging them into the current branch.
 * 
 * Example usage:
 * ```
 * val result = pullChangesUseCase(
 *     PullChangesParams(
 *         repoPath = "/path/to/repo",
 *         remote = "origin",
 *         credentials = Credentials.Token("ghp_xxx")
 *     )
 * )
 * result.onSuccess {
 *     println("Changes pulled successfully")
 * }
 * ```
 */
class PullChangesUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<PullChangesParams, Result<Unit>> {
    
    override suspend fun invoke(params: PullChangesParams): Result<Unit> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        
        if (params.remote.isBlank()) {
            return Result.failure(IllegalArgumentException("Remote name cannot be empty"))
        }
        
        return gitRepository.pull(
            repoPath = params.repoPath,
            remote = params.remote,
            branch = params.branch,
            credentials = params.credentials
        )
    }
}

/**
 * Parameters for PullChangesUseCase
 * 
 * @property repoPath Path to the local Git repository
 * @property remote Remote name (default: "origin")
 * @property branch Optional branch name to pull from
 * @property credentials Optional credentials for authentication
 */
data class PullChangesParams(
    val repoPath: String,
    val remote: String = "origin",
    val branch: String? = null,
    val credentials: Credentials? = null
)
