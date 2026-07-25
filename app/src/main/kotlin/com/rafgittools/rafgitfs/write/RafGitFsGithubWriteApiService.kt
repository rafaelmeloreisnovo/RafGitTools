package com.rafgittools.rafgitfs.write

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Minimal Git Data write surface. It intentionally has no DELETE, merge,
 * force-push or protected-branch update endpoint.
 */
interface RafGitFsGithubWriteApiService {
    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("repos/{owner}/{repo}/git/commits/{sha}")
    suspend fun getGitCommit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("sha") sha: String
    ): Response<RafGitFsGitCommitDto>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @POST("repos/{owner}/{repo}/git/refs")
    suspend fun createRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: RafGitFsCreateRefRequest
    ): Response<RafGitFsGitRefDto>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @POST("repos/{owner}/{repo}/git/blobs")
    suspend fun createBlob(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: RafGitFsCreateBlobRequest
    ): Response<RafGitFsGitObjectDto>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @POST("repos/{owner}/{repo}/git/trees")
    suspend fun createTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: RafGitFsCreateTreeRequest
    ): Response<RafGitFsGitTreeDto>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @POST("repos/{owner}/{repo}/git/commits")
    suspend fun createCommit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: RafGitFsCreateCommitRequest
    ): Response<RafGitFsGitCommitDto>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @PATCH("repos/{owner}/{repo}/git/refs/heads/{branch}")
    suspend fun updateBranchRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch", encoded = true) branch: String,
        @Body request: RafGitFsUpdateRefRequest
    ): Response<RafGitFsGitRefDto>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @POST("repos/{owner}/{repo}/pulls")
    suspend fun openPullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: RafGitFsOpenPullRequestRequest
    ): Response<RafGitFsPullRequestDto>
}

data class RafGitFsGitObjectDto(val sha: String, val url: String? = null)
data class RafGitFsGitRefDto(val ref: String, val nodeId: String? = null, val url: String? = null, val `object`: RafGitFsGitObjectDto)
data class RafGitFsGitTreeDto(val sha: String, val url: String? = null)
data class RafGitFsGitCommitDto(val sha: String, val tree: RafGitFsGitTreeDto, val parents: List<RafGitFsGitObjectDto> = emptyList())
data class RafGitFsPullRequestDto(val number: Int, val htmlUrl: String, val state: String, val draft: Boolean, val head: RafGitFsPullRefDto, val base: RafGitFsPullRefDto)
data class RafGitFsPullRefDto(val ref: String, val sha: String)

data class RafGitFsCreateRefRequest(val ref: String, val sha: String)
data class RafGitFsCreateBlobRequest(val content: String, val encoding: String = "base64")
data class RafGitFsTreeEntryRequest(val path: String, val mode: String = "100644", val type: String = "blob", val sha: String)
data class RafGitFsCreateTreeRequest(val baseTree: String, val tree: List<RafGitFsTreeEntryRequest>)
data class RafGitFsCreateCommitRequest(val message: String, val tree: String, val parents: List<String>)
data class RafGitFsUpdateRefRequest(val sha: String, val force: Boolean = false)
data class RafGitFsOpenPullRequestRequest(
    val title: String,
    val body: String,
    val head: String,
    val base: String,
    val draft: Boolean = true,
    val maintainerCanModify: Boolean = true
)
