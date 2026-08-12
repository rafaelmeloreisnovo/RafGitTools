package com.rafgittools.data.privacy

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

/** Dedicated, minimal GitHub API surface for repository privacy governance. */
interface RepositoryPrivacyApi {
    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("user/repos")
    suspend fun listRepositories(
        @Query("visibility") visibility: String = "all",
        @Query("affiliation") affiliation: String = "owner,organization_member",
        @Query("sort") sort: String = "full_name",
        @Query("direction") direction: String = "asc",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100
    ): List<PrivacyRepositoryDto>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @PATCH("repos/{owner}/{repo}")
    suspend fun updateVisibility(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: VisibilityPatchRequest
    ): PrivacyRepositoryDto
}

data class VisibilityPatchRequest(val visibility: String)

data class PrivacyRepositoryDto(
    val id: Long,
    val name: String,
    @SerializedName("full_name") val fullName: String,
    val owner: PrivacyRepositoryOwnerDto,
    @SerializedName("private") val isPrivate: Boolean,
    @SerializedName("fork") val isFork: Boolean,
    val visibility: String? = null,
    val archived: Boolean = false,
    val disabled: Boolean = false,
    @SerializedName("has_pages") val hasPages: Boolean = false,
    @SerializedName("stargazers_count") val stargazersCount: Int = 0,
    @SerializedName("watchers_count") val watchersCount: Int = 0,
    @SerializedName("forks_count") val forksCount: Int = 0,
    val permissions: PrivacyRepositoryPermissionsDto? = null
)

data class PrivacyRepositoryOwnerDto(
    val login: String,
    val type: String = "Unknown"
)

data class PrivacyRepositoryPermissionsDto(
    val admin: Boolean = false,
    val maintain: Boolean = false,
    val push: Boolean = false,
    val triage: Boolean = false,
    val pull: Boolean = false
)
