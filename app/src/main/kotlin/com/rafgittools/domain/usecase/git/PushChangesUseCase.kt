package com.rafgittools.domain.usecase.git

import com.rafgittools.domain.repository.Credentials
import com.rafgittools.domain.repository.GitRepository
import com.rafgittools.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for pushing changes to a remote repository.
 * 
 * This use case handles pushing local commits to a remote repository,
 * including authentication handling.
 *
 * For GitHub HTTPS, pass the PAT/OAuth token as the password and a non-empty
 * username; do not embed credential material in the remote URL.
 * 
 * Example usage:
 * ```
 * val result = pushChangesUseCase(
 *     PushChangesParams(
 *         repoPath = "/path/to/repo",
 *         remote = "origin",
 *         branch = "main",
 *         credentials = Credentials.UsernamePassword("user", "YOUR-PAT")
 *     )
 * )
 * result.onSuccess {
 *     println("Changes pushed successfully")
 * }
 * ```
 */
class PushChangesUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<PushChangesParams, Result<Unit>> {
    
    override suspend fun invoke(params: PushChangesParams): Result<Unit> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        
        if (params.remote.isBlank()) {
            return Result.failure(IllegalArgumentException("Remote name cannot be empty"))
        }
        
        return gitRepository.push(
            repoPath = params.repoPath,
            remote = params.remote,
            branch = params.branch,
            credentials = params.credentials
        )
    }
}

/**
 * Parameters for PushChangesUseCase
 * 
 * @property repoPath Path to the local Git repository
 * @property remote Remote name (default: "origin")
 * @property branch Optional branch name to push
 * @property credentials Optional credentials for authentication
 */
data class PushChangesParams(
    val repoPath: String,
    val remote: String = "origin",
    val branch: String? = null,
    val credentials: Credentials? = null
)
