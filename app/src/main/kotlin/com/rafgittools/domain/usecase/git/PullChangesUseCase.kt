package com.rafgittools.domain.usecase.git

import com.rafgittools.domain.repository.Credentials
import com.rafgittools.domain.repository.GitRepository
import com.rafgittools.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Pull changes from a remote repository.
 *
 * For GitHub HTTPS, use a non-empty username and place the PAT/OAuth token in
 * the password field. Never embed credentials in the remote URL.
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

data class PullChangesParams(
    val repoPath: String,
    val remote: String = "origin",
    val branch: String? = null,
    val credentials: Credentials? = null
)
