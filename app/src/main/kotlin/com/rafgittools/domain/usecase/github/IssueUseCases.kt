package com.rafgittools.domain.usecase.github

import com.rafgittools.data.github.CreateIssueRequest
import com.rafgittools.data.github.CreateCommentRequest
import com.rafgittools.data.github.UpdateIssueRequest
import com.rafgittools.data.github.GithubApiService
import com.rafgittools.domain.model.github.GithubIssue
import com.rafgittools.domain.model.github.GithubComment
import com.rafgittools.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for getting issues in a repository
 * Features #91-108 from roadmap (Phase 2)
 */
class GetIssuesUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<GetIssuesParams, Result<List<GithubIssue>>> {
    
    override suspend fun invoke(params: GetIssuesParams): Result<List<GithubIssue>> {
        return try {
            val issues = githubApiService.getIssues(
                owner = params.owner,
                repo = params.repo,
                state = params.state,
                page = params.page,
                perPage = params.perPage
            )
            Result.success(issues)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class GetIssuesParams(
    val owner: String,
    val repo: String,
    val state: String = "open",
    val page: Int = 1,
    val perPage: Int = 30
)

/**
 * Use case for getting a single issue
 * Feature #92 from roadmap
 */
class GetIssueUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<GetIssueParams, Result<GithubIssue>> {
    
    override suspend fun invoke(params: GetIssueParams): Result<GithubIssue> {
        return try {
            val issue = githubApiService.getIssue(
                owner = params.owner,
                repo = params.repo,
                number = params.number
            )
            Result.success(issue)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class GetIssueParams(
    val owner: String,
    val repo: String,
    val number: Int
)

/**
 * Use case for creating a new issue
 * Feature #93 from roadmap
 */
class CreateIssueUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<CreateIssueParams, Result<GithubIssue>> {
    
    override suspend fun invoke(params: CreateIssueParams): Result<GithubIssue> {
        if (params.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Issue title cannot be empty"))
        }
        
        return try {
            val issue = githubApiService.createIssue(
                owner = params.owner,
                repo = params.repo,
                issue = CreateIssueRequest(
                    title = params.title,
                    body = params.body,
                    labels = params.labels,
                    assignees = params.assignees
                )
            )
            Result.success(issue)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class CreateIssueParams(
    val owner: String,
    val repo: String,
    val title: String,
    val body: String? = null,
    val labels: List<String>? = null,
    val assignees: List<String>? = null
)

/**
 * Use case for updating an issue
 * Feature #94 from roadmap
 */
class UpdateIssueUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<UpdateIssueParams, Result<GithubIssue>> {
    
    override suspend fun invoke(params: UpdateIssueParams): Result<GithubIssue> {
        return try {
            val issue = githubApiService.updateIssue(
                owner = params.owner,
                repo = params.repo,
                number = params.number,
                update = UpdateIssueRequest(
                    title = params.title,
                    body = params.body,
                    state = params.state,
                    labels = params.labels,
                    assignees = params.assignees,
                    milestone = params.milestone
                )
            )
            Result.success(issue)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class UpdateIssueParams(
    val owner: String,
    val repo: String,
    val number: Int,
    val title: String? = null,
    val body: String? = null,
    val state: String? = null,
    val labels: List<String>? = null,
    val assignees: List<String>? = null,
    val milestone: Int? = null
)

/**
 * Use case for getting issue comments
 * Feature #95 from roadmap
 */
class GetIssueCommentsUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<GetIssueCommentsParams, Result<List<GithubComment>>> {
    
    override suspend fun invoke(params: GetIssueCommentsParams): Result<List<GithubComment>> {
        return try {
            val comments = githubApiService.getIssueComments(
                owner = params.owner,
                repo = params.repo,
                number = params.number,
                page = params.page,
                perPage = params.perPage
            )
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class GetIssueCommentsParams(
    val owner: String,
    val repo: String,
    val number: Int,
    val page: Int = 1,
    val perPage: Int = 30
)

/**
 * Use case for creating an issue comment
 * Feature #95 from roadmap
 */
class CreateIssueCommentUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<CreateIssueCommentParams, Result<GithubComment>> {
    
    override suspend fun invoke(params: CreateIssueCommentParams): Result<GithubComment> {
        if (params.body.isBlank()) {
            return Result.failure(IllegalArgumentException("Comment body cannot be empty"))
        }
        
        return try {
            val comment = githubApiService.createIssueComment(
                owner = params.owner,
                repo = params.repo,
                number = params.number,
                comment = CreateCommentRequest(body = params.body)
            )
            Result.success(comment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class CreateIssueCommentParams(
    val owner: String,
    val repo: String,
    val number: Int,
    val body: String
)
