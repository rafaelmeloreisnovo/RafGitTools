package com.rafgittools.data.github

import com.google.gson.annotations.SerializedName
import com.rafgittools.domain.model.github.*
import retrofit2.http.*

/**
 * GitHub API service interface
 */
interface GithubApiService {
    
    // Repositories
    @GET("user/repos")
    suspend fun getUserRepositories(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30,
        @Query("sort") sort: String = "updated",
        @Query("type") type: String = "all"
    ): List<GithubRepository>
    
    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GithubRepository
    
    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): SearchResponse<GithubRepository>

    @GET("search/issues")
    suspend fun searchIssues(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): SearchResponse<GithubSearchIssue>
    ): SearchResponse<GithubIssue>

    @GET("search/users")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): SearchResponse<GithubUser>

    @Headers("Accept: application/vnd.github.v3.text-match+json")
    @GET("search/code")
    suspend fun searchCode(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): SearchResponse<GithubCodeSearchItem>
    
    // User
    @GET("user")
    suspend fun getAuthenticatedUser(): GithubUser
    
    @GET("users/{username}")
    suspend fun getUser(
        @Path("username") username: String
    ): GithubUser
    
    // Issues
    @GET("repos/{owner}/{repo}/issues")
    suspend fun getIssues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<GithubIssue>
    
    @GET("repos/{owner}/{repo}/issues/{number}")
    suspend fun getIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int
    ): GithubIssue
    
    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body issue: CreateIssueRequest
    ): GithubIssue
    
    // Pull Requests
    @GET("repos/{owner}/{repo}/pulls")
    suspend fun getPullRequests(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<GithubPullRequest>
    
    @GET("repos/{owner}/{repo}/pulls/{number}")
    suspend fun getPullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int
    ): GithubPullRequest
    
    // Issue Comments
    @GET("repos/{owner}/{repo}/issues/{number}/comments")
    suspend fun getIssueComments(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<GithubComment>
    
    @POST("repos/{owner}/{repo}/issues/{number}/comments")
    suspend fun createIssueComment(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body comment: CreateCommentRequest
    ): GithubComment
    
    @PATCH("repos/{owner}/{repo}/issues/{number}")
    suspend fun updateIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body update: UpdateIssueRequest
    ): GithubIssue
    
    // Pull Request Reviews
    @GET("repos/{owner}/{repo}/pulls/{number}/reviews")
    suspend fun getPullRequestReviews(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<GithubReview>
    
    @GET("repos/{owner}/{repo}/pulls/{number}/comments")
    suspend fun getPullRequestComments(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<GithubReviewComment>
    
    @GET("repos/{owner}/{repo}/pulls/{number}/files")
    suspend fun getPullRequestFiles(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<GithubPullRequestFile>
    
    @GET("repos/{owner}/{repo}/pulls/{number}/commits")
    suspend fun getPullRequestCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<GithubCommit>
    
    @POST("repos/{owner}/{repo}/pulls")
    suspend fun createPullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body pullRequest: CreatePullRequestRequest
    ): GithubPullRequest
    
    @POST("repos/{owner}/{repo}/pulls/{number}/reviews")
    suspend fun createReview(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body review: CreateReviewRequest
    ): GithubReview
    
    @PUT("repos/{owner}/{repo}/pulls/{number}/merge")
    suspend fun mergePullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body merge: MergePullRequestRequest
    ): MergeResult
    
    // Releases
    @GET("repos/{owner}/{repo}/releases")
    suspend fun getReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<GithubRelease>
    
    @GET("repos/{owner}/{repo}/releases/{id}")
    suspend fun getRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("id") id: Long
    ): GithubRelease
    
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GithubRelease
    
    @POST("repos/{owner}/{repo}/releases")
    suspend fun createRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body release: CreateReleaseRequest
    ): GithubRelease
    
    // Repository Contents
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Query("ref") ref: String? = null
    ): List<GithubContent>
    
    @GET("repos/{owner}/{repo}/readme")
    suspend fun getReadme(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("ref") ref: String? = null
    ): GithubContent
    
    // Branches
    @GET("repos/{owner}/{repo}/branches")
    suspend fun getBranches(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<GithubBranchInfo>
    
    // Commits
    @GET("repos/{owner}/{repo}/commits")
    suspend fun getCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("sha") sha: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<GithubCommit>
    
    @GET("repos/{owner}/{repo}/commits/{sha}")
    suspend fun getCommit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("sha") sha: String
    ): GithubCommitDetail
    
    // Labels
    @GET("repos/{owner}/{repo}/labels")
    suspend fun getLabels(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<GithubLabel>
    
    // Milestones
    @GET("repos/{owner}/{repo}/milestones")
    suspend fun getMilestones(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<GithubMilestone>
    
    // Assignees
    @GET("repos/{owner}/{repo}/assignees")
    suspend fun getAssignees(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<GithubUser>
    
    // Reactions
    @GET("repos/{owner}/{repo}/issues/{number}/reactions")
    suspend fun getIssueReactions(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int
    ): List<GithubReaction>
    
    @POST("repos/{owner}/{repo}/issues/{number}/reactions")
    suspend fun createIssueReaction(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body reaction: CreateReactionRequest
    ): GithubReaction
    
    // Notifications
    @GET("notifications")
    suspend fun getNotifications(
        @Query("all") all: Boolean = false,
        @Query("participating") participating: Boolean = false,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<GithubNotification>
    
    @PATCH("notifications/threads/{threadId}")
    suspend fun markNotificationRead(
        @Path("threadId") threadId: String
    )
    
    @PUT("notifications")
    suspend fun markAllNotificationsRead(
        @Body request: MarkNotificationsReadRequest
    )
    
    // Starring
    @GET("user/starred/{owner}/{repo}")
    suspend fun isStarred(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    )
    
    @PUT("user/starred/{owner}/{repo}")
    suspend fun starRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    )
    
    @DELETE("user/starred/{owner}/{repo}")
    suspend fun unstarRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    )
    
    // Watching
    @GET("repos/{owner}/{repo}/subscription")
    suspend fun getSubscription(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GithubSubscription
    
    @PUT("repos/{owner}/{repo}/subscription")
    suspend fun setSubscription(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body subscription: SetSubscriptionRequest
    ): GithubSubscription
    
    @DELETE("repos/{owner}/{repo}/subscription")
    suspend fun deleteSubscription(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    )
    
    // Forking
    @POST("repos/{owner}/{repo}/forks")
    suspend fun forkRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body fork: ForkRepositoryRequest? = null
    ): GithubRepository
}

/**
 * Search response wrapper
 */
data class SearchResponse<T>(
    val total_count: Int,
    val incomplete_results: Boolean,
    val items: List<T>
)

/**
 * Code search item
 */
data class GithubCodeSearchItem(
    val name: String,
    val path: String,
    val repository: GithubRepository,
    @SerializedName("text_matches")
    val textMatches: List<GithubTextMatch>? = null
)

/**
 * Text match fragment for search results
 */
data class GithubTextMatch(
    val fragment: String
)

/**
 * Create issue request
 */
data class CreateIssueRequest(
    val title: String,
    val body: String?,
    val labels: List<String>?,
    val assignees: List<String>?
)

/**
 * Create comment request
 */
data class CreateCommentRequest(
    val body: String
)

/**
 * Update issue request
 */
data class UpdateIssueRequest(
    val title: String? = null,
    val body: String? = null,
    val state: String? = null,
    val labels: List<String>? = null,
    val assignees: List<String>? = null,
    val milestone: Int? = null
)

/**
 * Create pull request request
 */
data class CreatePullRequestRequest(
    val title: String,
    val body: String?,
    val head: String,
    val base: String,
    val draft: Boolean = false,
    val maintainer_can_modify: Boolean = true
)

/**
 * Create review request
 */
data class CreateReviewRequest(
    val body: String?,
    val event: String, // APPROVE, REQUEST_CHANGES, COMMENT
    val comments: List<ReviewCommentRequest>? = null
)

/**
 * Review comment request
 */
data class ReviewCommentRequest(
    val path: String,
    val body: String,
    val line: Int? = null,
    val side: String? = null, // LEFT, RIGHT
    val start_line: Int? = null,
    val start_side: String? = null
)

/**
 * Merge pull request request
 */
data class MergePullRequestRequest(
    val commit_title: String? = null,
    val commit_message: String? = null,
    val sha: String? = null,
    val merge_method: String = "merge" // merge, squash, rebase
)

/**
 * Merge result
 */
data class MergeResult(
    val sha: String?,
    val merged: Boolean,
    val message: String
)

/**
 * Create release request
 */
data class CreateReleaseRequest(
    val tag_name: String,
    val target_commitish: String? = null,
    val name: String?,
    val body: String?,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val generate_release_notes: Boolean = false
)

/**
 * Create reaction request
 */
data class CreateReactionRequest(
    val content: String // +1, -1, laugh, confused, heart, hooray, rocket, eyes
)

/**
 * Mark notifications read request
 */
data class MarkNotificationsReadRequest(
    val last_read_at: String? = null,
    val read: Boolean = true
)

/**
 * Set subscription request
 */
data class SetSubscriptionRequest(
    val subscribed: Boolean,
    val ignored: Boolean
)

/**
 * Fork repository request
 */
data class ForkRepositoryRequest(
    val organization: String? = null,
    val name: String? = null,
    val default_branch_only: Boolean = false
)
