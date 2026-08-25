package com.rafgittools.data.azuredevops

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface AzureDevOpsApiService {

    @GET("{organization}/{project}/_apis/git/repositories")
    suspend fun getRepositories(
        @Header("Authorization") authorization: String,
        @Path("organization") organization: String,
        @Path("project") project: String,
        @Query("api-version") apiVersion: String = "7.0"
    ): AzureDevOpsRepositoryList
}

data class AzureDevOpsRepositoryList(
    @SerializedName("value") val value: List<AzureDevOpsRepository>,
    @SerializedName("count") val count: Int
)

data class AzureDevOpsRepository(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("project") val project: AzureDevOpsProject?,
    @SerializedName("remoteUrl") val remoteUrl: String?,
    @SerializedName("sshUrl") val sshUrl: String?,
    @SerializedName("isDisabled") val isDisabled: Boolean?,
    @SerializedName("isInMaintenance") val isInMaintenance: Boolean?
) {
    val fullName: String get() = if (project != null) "${project.name}/$name" else name
}

data class AzureDevOpsProject(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("visibility") val visibility: String?
) {
    val isPrivate: Boolean get() = visibility == null || visibility == "private"
}
