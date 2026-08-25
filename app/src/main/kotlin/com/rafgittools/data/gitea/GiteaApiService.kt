package com.rafgittools.data.gitea

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface GiteaApiService {

    @GET("api/v1/user/repos")
    suspend fun getUserRepos(
        @Header("Authorization") authorization: String,
        @Query("limit") limit: Int = 50,
        @Query("page") page: Int = 1
    ): List<GiteaRepository>
}

data class GiteaRepository(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("description") val description: String?,
    @SerializedName("clone_url") val cloneUrl: String,
    @SerializedName("ssh_url") val sshUrl: String?,
    @SerializedName("private") val isPrivate: Boolean
)
