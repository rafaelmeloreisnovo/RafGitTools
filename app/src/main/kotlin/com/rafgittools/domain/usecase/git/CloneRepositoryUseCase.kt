package com.rafgittools.domain.usecase.git

import com.rafgittools.domain.model.GitRepository
import com.rafgittools.domain.repository.Credentials
import com.rafgittools.domain.repository.GitRepository as IGitRepository
import com.rafgittools.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for cloning a Git repository.
 * 
 * This use case encapsulates the business logic for cloning a repository
 * from a remote URL to a local path, handling authentication and validation.
 *
 * For GitHub HTTPS, pass the PAT/OAuth token as the password and a non-empty
 * username; do not embed credential material in the remote URL.
 * 
 * Example usage:
 * ```
 * val result = cloneRepositoryUseCase(
 *     CloneRepositoryParams(
 *         url = "https://github.com/user/repo.git",
 *         localPath = "/storage/repos/repo",
 *         credentials = Credentials.UsernamePassword("user", "YOUR-PAT")
 *     )
 * )
 * ```
 */
class CloneRepositoryUseCase @Inject constructor(
    private val gitRepository: IGitRepository
) : UseCase<CloneRepositoryParams, Result<GitRepository>> {
    
    override suspend fun invoke(params: CloneRepositoryParams): Result<GitRepository> {
        // Validate URL
        if (params.url.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository URL cannot be empty"))
        }
        
        // Validate local path
        if (params.localPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Local path cannot be empty"))
        }
        
        return gitRepository.cloneRepository(
            url = params.url,
            localPath = params.localPath,
            credentials = params.credentials
        )
    }
}

/**
 * Parameters for CloneRepositoryUseCase
 * 
 * @property url The Git repository URL (HTTP/HTTPS/SSH)
 * @property localPath Local path where the repository should be cloned
 * @property credentials Optional credentials for authentication
 */
data class CloneRepositoryParams(
    val url: String,
    val localPath: String,
    val credentials: Credentials? = null
)
