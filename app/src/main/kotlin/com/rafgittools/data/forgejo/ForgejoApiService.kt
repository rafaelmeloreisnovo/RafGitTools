package com.rafgittools.data.forgejo

import com.google.gson.annotations.SerializedName
import retrofit2.http.*

/**
 * Forgejo API service interface (compatible with Gitea, with Forgejo-specific features)
 *
 * Forgejo is a community-driven fork of Gitea with enhanced federation, CI/CD, and security.
 * API is mostly compatible with Gitea v1, but adds Forgejo-specific endpoints and fields.
 */
interface ForgejoApiService {

    // User Repositories
    @GET("user/repos")
    suspend fun getUserRepositories(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): List<ForgejoRepository>

    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): ForgejoRepository

    // Issues
    @GET("repos/{owner}/{repo}/issues")
    suspend fun getIssues(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): List<ForgejoIssue>

    // Pull Requests
    @GET("repos/{owner}/{repo}/pulls")
    suspend fun getPullRequests(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): List<ForgejoPullRequest>

    // Forgejo-specific: Actions (CI/CD)
    @GET("repos/{owner}/{repo}/actions/workflows")
    suspend fun getWorkflows(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): List<ForgejoWorkflow>

    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun getActionRuns(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): List<ForgejoActionRun>

    // Organizations (Forgejo/Gitea)
    @GET("orgs/{org}")
    suspend fun getOrganization(
        @Header("Authorization") authorization: String,
        @Path("org") org: String
    ): ForgejoOrganization

    // User info
    @GET("user")
    suspend fun getAuthenticatedUser(
        @Header("Authorization") authorization: String
    ): ForgejoUser
}

/**
 * Forgejo repository model
 */
data class ForgejoRepository(
    val id: Long,
    val name: String,
    @SerializedName("full_name")
    val fullName: String,
    val description: String? = null,
    val private: Boolean = false,
    val fork: Boolean = false,
    @SerializedName("clone_url")
    val cloneUrl: String,
    @SerializedName("ssh_url")
    val sshUrl: String? = null,
    @SerializedName("html_url")
    val htmlUrl: String? = null,
    val archived: Boolean = false,
    @SerializedName("default_branch")
    val defaultBranch: String = "main",
    val size: Long? = null,
    @SerializedName("star_count")
    val starCount: Int = 0,
    @SerializedName("fork_count")
    val forkCount: Int = 0,
    @SerializedName("open_issues_count")
    val openIssuesCount: Int = 0,
    @SerializedName("mirror")
    val isMirror: Boolean = false,
    @SerializedName("empty")
    val isEmpty: Boolean = false,
    @SerializedName("template")
    val isTemplate: Boolean = false,
    val owner: ForgejoUser? = null
)

/**
 * Forgejo issue model
 */
data class ForgejoIssue(
    val id: Long,
    val number: Long,
    val title: String,
    val body: String? = null,
    val state: String,
    @SerializedName("pull_request")
    val pullRequest: ForgejoPullRequestLink? = null,
    val user: ForgejoUser? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

/**
 * Forgejo pull request model
 */
data class ForgejoPullRequest(
    val id: Long,
    val number: Long,
    val title: String,
    val body: String? = null,
    val state: String,
    val draft: Boolean = false,
    val head: ForgejoBranchRef? = null,
    val base: ForgejoBranchRef? = null,
    val user: ForgejoUser? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    @SerializedName("merged_at")
    val mergedAt: String? = null
)

/**
 * Branch reference in pull request
 */
data class ForgejoBranchRef(
    val label: String,
    val ref: String,
    val sha: String,
    val repo: ForgejoRepository? = null
)

/**
 * Forgejo pull request link (in issue)
 */
data class ForgejoPullRequestLink(
    @SerializedName("html_url")
    val htmlUrl: String
)

/**
 * Forgejo workflow (CI/CD)
 */
data class ForgejoWorkflow(
    val id: Long,
    val node_id: String,
    val name: String,
    val path: String,
    val state: String,
    val created_at: String? = null,
    val updated_at: String? = null
)

/**
 * Forgejo action run
 */
data class ForgejoActionRun(
    val id: Long,
    val name: String,
    val head_branch: String,
    val head_sha: String,
    val status: String,
    val conclusion: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    @SerializedName("workflow_id")
    val workflowId: Long? = null
)

/**
 * Forgejo user model
 */
data class ForgejoUser(
    val id: Long,
    val login: String,
    @SerializedName("full_name")
    val fullName: String? = null,
    val email: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    @SerializedName("html_url")
    val htmlUrl: String? = null,
    val admin: Boolean = false,
    @SerializedName("is_admin")
    val isAdmin: Boolean = false
)

/**
 * Forgejo organization
 */
data class ForgejoOrganization(
    val id: Long,
    val username: String,
    val full_name: String? = null,
    val avatar_url: String? = null,
    val description: String? = null,
    val website: String? = null,
    val location: String? = null,
    val repo_admin_change_team_access: Boolean = false
)
