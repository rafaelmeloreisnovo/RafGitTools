package com.rafgittools.domain.usecase.git

import com.rafgittools.domain.model.GitAuthor
import com.rafgittools.domain.model.GitCommit
import com.rafgittools.domain.repository.GitRepository
import com.rafgittools.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for committing staged changes to a Git repository.
 * 
 * This use case creates a new commit with the staged changes and
 * the provided commit message and author information.
 * 
 * Example usage:
 * ```
 * val result = commitChangesUseCase(
 *     CommitChangesParams(
 *         repoPath = "/path/to/repo",
 *         message = "feat: add new feature",
 *         author = GitAuthor("John Doe", "john@example.com")
 *     )
 * )
 * result.onSuccess { commit ->
 *     println("Created commit: ${commit.sha}")
 * }
 * ```
 */
class CommitChangesUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<CommitChangesParams, Result<GitCommit>> {
    
    override suspend fun invoke(params: CommitChangesParams): Result<GitCommit> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        
        if (params.message.isBlank()) {
            return Result.failure(IllegalArgumentException("Commit message cannot be empty"))
        }
        
        return gitRepository.commit(
            repoPath = params.repoPath,
            message = params.message,
            author = params.author
        )
    }
}

/**
 * Parameters for CommitChangesUseCase
 * 
 * @property repoPath Path to the local Git repository
 * @property message Commit message (follows conventional commits specification)
 * @property author Optional author information (uses Git config if not provided)
 */
data class CommitChangesParams(
    val repoPath: String,
    val message: String,
    val author: GitAuthor? = null
)
