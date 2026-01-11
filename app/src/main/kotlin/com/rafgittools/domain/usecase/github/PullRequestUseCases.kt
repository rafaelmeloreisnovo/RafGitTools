package com.rafgittools.domain.usecase.github

import com.rafgittools.data.github.CreatePullRequestRequest
import com.rafgittools.data.github.CreateReviewRequest
import com.rafgittools.data.github.MergePullRequestRequest
import com.rafgittools.data.github.MergeResult
import com.rafgittools.data.github.GithubApiService
import com.rafgittools.domain.model.github.*
import com.rafgittools.domain.usecase.UseCase
import javax.inject.Inject

/**
 * Use case for getting pull requests in a repository
 * Features #109-126 from roadmap (Phase 2)
 */
class GetPullRequestsUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<GetPullRequestsParams, Result<List<GithubPullRequest>>> {
    
    override suspend fun invoke(params: GetPullRequestsParams): Result<List<GithubPullRequest>> {
        return try {
            val pullRequests = githubApiService.getPullRequests(
                owner = params.owner,
                repo = params.repo,
                state = params.state,
                page = params.page,
                perPage = params.perPage
            )
            Result.success(pullRequests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class GetPullRequestsParams(
    val owner: String,
    val repo: String,
    val state: String = "open",
    val page: Int = 1,
    val perPage: Int = 30
)

/**
 * Use case for getting a single pull request
 * Feature #110 from roadmap
 */
class GetPullRequestUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<GetPullRequestParams, Result<GithubPullRequest>> {
    
    override suspend fun invoke(params: GetPullRequestParams): Result<GithubPullRequest> {
        return try {
            val pullRequest = githubApiService.getPullRequest(
                owner = params.owner,
                repo = params.repo,
                number = params.number
            )
            Result.success(pullRequest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class GetPullRequestParams(
    val owner: String,
    val repo: String,
    val number: Int
)

/**
 * Use case for creating a pull request
 * Feature #111 from roadmap
 */
class CreatePullRequestUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<CreatePullRequestParams, Result<GithubPullRequest>> {
    
    override suspend fun invoke(params: CreatePullRequestParams): Result<GithubPullRequest> {
        if (params.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Pull request title cannot be empty"))
        }
        if (params.head.isBlank()) {
            return Result.failure(IllegalArgumentException("Head branch cannot be empty"))
        }
        if (params.base.isBlank()) {
            return Result.failure(IllegalArgumentException("Base branch cannot be empty"))
        }
        
        return try {
            val pullRequest = githubApiService.createPullRequest(
                owner = params.owner,
                repo = params.repo,
                pullRequest = CreatePullRequestRequest(
                    title = params.title,
                    body = params.body,
                    head = params.head,
                    base = params.base,
                    draft = params.draft,
                    maintainer_can_modify = params.maintainerCanModify
                )
            )
            Result.success(pullRequest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class CreatePullRequestParams(
    val owner: String,
    val repo: String,
    val title: String,
    val body: String? = null,
    val head: String,
    val base: String,
    val draft: Boolean = false,
    val maintainerCanModify: Boolean = true
)

/**
 * Use case for getting pull request files
 * Feature #122 from roadmap
 */
class GetPullRequestFilesUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<GetPullRequestFilesParams, Result<List<GithubPullRequestFile>>> {
    
    override suspend fun invoke(params: GetPullRequestFilesParams): Result<List<GithubPullRequestFile>> {
        return try {
            val files = githubApiService.getPullRequestFiles(
                owner = params.owner,
                repo = params.repo,
                number = params.number,
                page = params.page,
                perPage = params.perPage
            )
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class GetPullRequestFilesParams(
    val owner: String,
    val repo: String,
    val number: Int,
    val page: Int = 1,
    val perPage: Int = 30
)

/**
 * Use case for getting pull request commits
 * Feature #123 from roadmap
 */
class GetPullRequestCommitsUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<GetPullRequestCommitsParams, Result<List<GithubCommit>>> {
    
    override suspend fun invoke(params: GetPullRequestCommitsParams): Result<List<GithubCommit>> {
        return try {
            val commits = githubApiService.getPullRequestCommits(
                owner = params.owner,
                repo = params.repo,
                number = params.number,
                page = params.page,
                perPage = params.perPage
            )
            Result.success(commits)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class GetPullRequestCommitsParams(
    val owner: String,
    val repo: String,
    val number: Int,
    val page: Int = 1,
    val perPage: Int = 30
)

/**
 * Use case for getting pull request reviews
 * Feature from roadmap
 */
class GetPullRequestReviewsUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<GetPullRequestReviewsParams, Result<List<GithubReview>>> {
    
    override suspend fun invoke(params: GetPullRequestReviewsParams): Result<List<GithubReview>> {
        return try {
            val reviews = githubApiService.getPullRequestReviews(
                owner = params.owner,
                repo = params.repo,
                number = params.number,
                page = params.page,
                perPage = params.perPage
            )
            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class GetPullRequestReviewsParams(
    val owner: String,
    val repo: String,
    val number: Int,
    val page: Int = 1,
    val perPage: Int = 30
)

/**
 * Use case for creating a pull request review
 * Features #127-134 from roadmap
 */
class CreateReviewUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<CreateReviewParams, Result<GithubReview>> {
    
    override suspend fun invoke(params: CreateReviewParams): Result<GithubReview> {
        if (params.event !in listOf("APPROVE", "REQUEST_CHANGES", "COMMENT")) {
            return Result.failure(IllegalArgumentException("Invalid review event. Must be APPROVE, REQUEST_CHANGES, or COMMENT"))
        }
        
        return try {
            val review = githubApiService.createReview(
                owner = params.owner,
                repo = params.repo,
                number = params.number,
                review = CreateReviewRequest(
                    body = params.body,
                    event = params.event,
                    comments = null
                )
            )
            Result.success(review)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class CreateReviewParams(
    val owner: String,
    val repo: String,
    val number: Int,
    val body: String? = null,
    val event: String // APPROVE, REQUEST_CHANGES, COMMENT
)

/**
 * Use case for merging a pull request
 * Features #113-115 from roadmap
 */
class MergePullRequestUseCase @Inject constructor(
    private val githubApiService: GithubApiService
) : UseCase<MergePullRequestParams, Result<MergeResult>> {
    
    override suspend fun invoke(params: MergePullRequestParams): Result<MergeResult> {
        if (params.mergeMethod !in listOf("merge", "squash", "rebase")) {
            return Result.failure(IllegalArgumentException("Invalid merge method. Must be merge, squash, or rebase"))
        }
        
        return try {
            val result = githubApiService.mergePullRequest(
                owner = params.owner,
                repo = params.repo,
                number = params.number,
                merge = MergePullRequestRequest(
                    commit_title = params.commitTitle,
                    commit_message = params.commitMessage,
                    sha = params.sha,
                    merge_method = params.mergeMethod
                )
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class MergePullRequestParams(
    val owner: String,
    val repo: String,
    val number: Int,
    val commitTitle: String? = null,
    val commitMessage: String? = null,
    val sha: String? = null,
    val mergeMethod: String = "merge" // merge, squash, rebase
)
