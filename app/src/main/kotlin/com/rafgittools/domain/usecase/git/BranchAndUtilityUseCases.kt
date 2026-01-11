package com.rafgittools.domain.usecase.git

import com.rafgittools.domain.model.GitBranch
import com.rafgittools.domain.repository.GitRepository
import com.rafgittools.domain.repository.BlameLine
import com.rafgittools.domain.repository.ReflogEntry
import com.rafgittools.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for deleting a branch
 * Feature #32 from roadmap
 */
class DeleteBranchUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<DeleteBranchParams, Result<Unit>> {
    
    override suspend fun invoke(params: DeleteBranchParams): Result<Unit> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (params.branchName.isBlank()) {
            return Result.failure(IllegalArgumentException("Branch name cannot be empty"))
        }
        return gitRepository.deleteBranch(params.repoPath, params.branchName, params.force)
    }
}

data class DeleteBranchParams(
    val repoPath: String,
    val branchName: String,
    val force: Boolean = false
)

/**
 * Use case for renaming a branch
 * Feature #33 from roadmap
 */
class RenameBranchUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<RenameBranchParams, Result<GitBranch>> {
    
    override suspend fun invoke(params: RenameBranchParams): Result<GitBranch> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (params.oldName.isBlank()) {
            return Result.failure(IllegalArgumentException("Old branch name cannot be empty"))
        }
        if (params.newName.isBlank()) {
            return Result.failure(IllegalArgumentException("New branch name cannot be empty"))
        }
        return gitRepository.renameBranch(params.repoPath, params.oldName, params.newName)
    }
}

data class RenameBranchParams(
    val repoPath: String,
    val oldName: String,
    val newName: String
)

/**
 * Use case for cleaning untracked files
 * From roadmap
 */
class CleanUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<CleanParams, Result<List<String>>> {
    
    override suspend fun invoke(params: CleanParams): Result<List<String>> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.clean(
            repoPath = params.repoPath,
            dryRun = params.dryRun,
            directories = params.directories,
            force = params.force
        )
    }
}

data class CleanParams(
    val repoPath: String,
    val dryRun: Boolean = false,
    val directories: Boolean = false,
    val force: Boolean = true
)

/**
 * Use case for getting reflog entries
 * From roadmap
 */
class GetReflogUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<GetReflogParams, Result<List<ReflogEntry>>> {
    
    override suspend fun invoke(params: GetReflogParams): Result<List<ReflogEntry>> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.getReflog(
            repoPath = params.repoPath,
            ref = params.ref,
            limit = params.limit
        )
    }
}

data class GetReflogParams(
    val repoPath: String,
    val ref: String = "HEAD",
    val limit: Int = 50
)

/**
 * Use case for getting blame information for a file
 * Feature #180 from roadmap
 */
class BlameUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<BlameParams, Result<List<BlameLine>>> {
    
    override suspend fun invoke(params: BlameParams): Result<List<BlameLine>> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (params.filePath.isBlank()) {
            return Result.failure(IllegalArgumentException("File path cannot be empty"))
        }
        return gitRepository.blame(params.repoPath, params.filePath)
    }
}

data class BlameParams(
    val repoPath: String,
    val filePath: String
)
