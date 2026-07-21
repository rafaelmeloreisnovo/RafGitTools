package com.rafgittools.data.gitlab

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface GitLabApiService {

    @GET("api/v4/projects")
    suspend fun getUserProjects(
        @Header("PRIVATE-TOKEN") token: String,
        @Query("membership") membership: Boolean = true,
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
        @Query("order_by") orderBy: String = "last_activity_at"
    ): List<GitLabProject>

    @GET("api/v4/projects")
    suspend fun searchProjects(
        @Header("PRIVATE-TOKEN") token: String,
        @Query("search") query: String,
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): List<GitLabProject>
}

data class GitLabProject(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("path_with_namespace") val fullPath: String,
    @SerializedName("description") val description: String?,
    @SerializedName("http_url_to_repo") val cloneUrlHttp: String,
    @SerializedName("ssh_url_to_repo") val cloneUrlSsh: String?,
    @SerializedName("visibility") val visibility: String
) {
    val isPrivate: Boolean get() = visibility == "private"
}
