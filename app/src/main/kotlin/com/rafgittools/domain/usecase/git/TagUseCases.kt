package com.rafgittools.domain.usecase.git

import com.rafgittools.domain.model.GitTag
import com.rafgittools.domain.model.GitAuthor
import com.rafgittools.domain.repository.GitRepository
import com.rafgittools.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for listing all tags in a repository
 * Features #168-169 from roadmap
 */
class ListTagsUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<ListTagsParams, Result<List<GitTag>>> {
    
    override suspend fun invoke(params: ListTagsParams): Result<List<GitTag>> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.listTags(params.repoPath)
    }
}

data class ListTagsParams(
    val repoPath: String
)

/**
 * Use case for creating a lightweight tag
 * Feature #169 from roadmap
 */
class CreateLightweightTagUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<CreateLightweightTagParams, Result<GitTag>> {
    
    override suspend fun invoke(params: CreateLightweightTagParams): Result<GitTag> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (params.tagName.isBlank()) {
            return Result.failure(IllegalArgumentException("Tag name cannot be empty"))
        }
        return gitRepository.createLightweightTag(
            repoPath = params.repoPath,
            tagName = params.tagName,
            commitSha = params.commitSha
        )
    }
}

data class CreateLightweightTagParams(
    val repoPath: String,
    val tagName: String,
    val commitSha: String? = null
)

/**
 * Use case for creating an annotated tag
 * Feature #168 from roadmap
 */
class CreateAnnotatedTagUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<CreateAnnotatedTagParams, Result<GitTag>> {
    
    override suspend fun invoke(params: CreateAnnotatedTagParams): Result<GitTag> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (params.tagName.isBlank()) {
            return Result.failure(IllegalArgumentException("Tag name cannot be empty"))
        }
        if (params.message.isBlank()) {
            return Result.failure(IllegalArgumentException("Tag message cannot be empty for annotated tags"))
        }
        return gitRepository.createAnnotatedTag(
            repoPath = params.repoPath,
            tagName = params.tagName,
            message = params.message,
            tagger = params.tagger,
            commitSha = params.commitSha
        )
    }
}

data class CreateAnnotatedTagParams(
    val repoPath: String,
    val tagName: String,
    val message: String,
    val tagger: GitAuthor? = null,
    val commitSha: String? = null
)

/**
 * Use case for deleting a tag
 * Feature from roadmap
 */
class DeleteTagUseCase @Inject constructor(
    private val gitRepository: GitRepository
) : UseCase<DeleteTagParams, Result<Unit>> {
    
    override suspend fun invoke(params: DeleteTagParams): Result<Unit> {
        if (params.repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (params.tagName.isBlank()) {
            return Result.failure(IllegalArgumentException("Tag name cannot be empty"))
        }
        return gitRepository.deleteTag(params.repoPath, params.tagName)
    }
}

data class DeleteTagParams(
    val repoPath: String,
    val tagName: String
)
