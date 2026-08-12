package com.rafgittools.domain.usecase.git

import com.rafgittools.domain.model.GitRepository
import com.rafgittools.domain.repository.Credentials
import com.rafgittools.domain.repository.GitRepository as IGitRepository
import com.rafgittools.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for cloning a Git repository.
 *
 * For GitHub HTTPS, pass the PAT/OAuth token as the password and a non-empty
 * username. Do not put credential material in the remote URL or username.
 *
 * Example:
 * ```
 * CloneRepositoryParams(
 *     url = "https://github.com/user/repo.git",
 *     localPath = "/storage/repos/repo",
 *     credentials = Credentials.UsernamePassword("user", "YOUR-PAT")
 * )
 * ```
 */
class CloneRepositoryUseCase @Inject constructor(
    private val gitRepository: IGitRepository
) : UseCase<CloneRepositoryParams, Result<GitRepository>> {
    override suspend fun invoke(params: CloneRepositoryParams): Result<GitRepository> {
        if (params.url.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository URL cannot be empty"))
        }
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

data class CloneRepositoryParams(
    val url: String,
    val localPath: String,
    val credentials: Credentials? = null
)
