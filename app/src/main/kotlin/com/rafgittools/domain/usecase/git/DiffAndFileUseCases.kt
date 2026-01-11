package com.rafgittools.domain.usecase.git

import com.rafgittools.domain.model.GitDiff
import com.rafgittools.domain.model.GitFile
import com.rafgittools.domain.model.FileContent
import com.rafgittools.domain.repository.GitRepository
import com.rafgittools.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for getting diff for working directory changes
 * Feature #39 from roadmap
 */
class GetDiffUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<GetDiffParams, Result<List<GitDiff>>> {
    
    override suspend fun invoke(params: GetDiffParams): Result<List<GitDiff>> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.getDiff(params.repoPath, params.cached)
    }
}

data class GetDiffParams(
    val repoPath: String,
    val cached: Boolean = false
)

/**
 * Use case for getting diff between two commits
 * Feature #39 from roadmap
 */
class GetDiffBetweenCommitsUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<GetDiffBetweenCommitsParams, Result<List<GitDiff>>> {
    
    override suspend fun invoke(params: GetDiffBetweenCommitsParams): Result<List<GitDiff>> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (params.oldCommitSha.isBlank()) {
            return Result.failure(IllegalArgumentException("Old commit SHA cannot be empty"))
        }
        if (params.newCommitSha.isBlank()) {
            return Result.failure(IllegalArgumentException("New commit SHA cannot be empty"))
        }
        return gitRepository.getDiffBetweenCommits(
            repoPath = params.repoPath,
            oldCommitSha = params.oldCommitSha,
            newCommitSha = params.newCommitSha
        )
    }
}

data class GetDiffBetweenCommitsParams(
    val repoPath: String,
    val oldCommitSha: String,
    val newCommitSha: String
)

/**
 * Use case for listing files in a repository directory
 * Feature #43 from roadmap
 */
class ListFilesUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<ListFilesParams, Result<List<GitFile>>> {
    
    override suspend fun invoke(params: ListFilesParams): Result<List<GitFile>> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.listFiles(
            repoPath = params.repoPath,
            path = params.path,
            ref = params.ref
        )
    }
}

data class ListFilesParams(
    val repoPath: String,
    val path: String = "",
    val ref: String? = null
)

/**
 * Use case for getting file content from repository
 * Feature #45 from roadmap
 */
class GetFileContentUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<GetFileContentParams, Result<FileContent>> {
    
    override suspend fun invoke(params: GetFileContentParams): Result<FileContent> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (params.filePath.isBlank()) {
            return Result.failure(IllegalArgumentException("File path cannot be empty"))
        }
        return gitRepository.getFileContent(
            repoPath = params.repoPath,
            filePath = params.filePath,
            ref = params.ref
        )
    }
}

data class GetFileContentParams(
    val repoPath: String,
    val filePath: String,
    val ref: String? = null
)
