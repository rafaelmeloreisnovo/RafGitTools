package com.rafgittools.rafgitfs.remote

import com.rafgittools.data.github.GithubCodeSearchItem
import com.rafgittools.data.github.SearchResponse
import com.rafgittools.domain.model.github.GithubBranchInfo
import com.rafgittools.domain.model.github.GithubRepository
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Dedicated read-only GitHub surface for RafGitFS.
 *
 * Keeping this interface separate from [com.rafgittools.data.github.GithubApiService]
 * prevents the virtual-storage engine from receiving mutation endpoints by accident.
 */
interface RafGitFsGithubApiService {

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("user/repos")
    suspend fun listRepositories(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("sort") sort: String = "updated",
        @Query("direction") direction: String = "desc",
        @Query("type") type: String = "all"
    ): Response<List<GithubRepository>>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("repos/{owner}/{repo}/branches")
    suspend fun listBranches(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<List<GithubBranchInfo>>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("repos/{owner}/{repo}/tags")
    suspend fun listTags(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<List<RafGitFsTagDto>>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("repos/{owner}/{repo}/commits/{ref}")
    suspend fun resolveCommit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("ref", encoded = true) ref: String
    ): Response<RafGitFsCommitDto>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("repos/{owner}/{repo}/git/trees/{treeSha}")
    suspend fun getTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("treeSha") treeSha: String,
        @Query("recursive") recursive: String = "1"
    ): Response<RafGitFsTreeDto>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("repos/{owner}/{repo}/git/blobs/{blobSha}")
    suspend fun getBlob(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("blobSha") blobSha: String
    ): Response<RafGitFsBlobDto>

    @Headers(
        "Accept: application/vnd.github.text-match+json",
        "X-GitHub-Api-Version: 2022-11-28"
    )
    @GET("search/code")
    suspend fun searchCode(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<SearchResponse<GithubCodeSearchItem>>
}
