package com.rafgittools.data.bitbucket

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface BitbucketApiService {

    @GET("2.0/repositories/{workspace}")
    suspend fun getWorkspaceRepositories(
        @Header("Authorization") authorization: String,
        @Path("workspace") workspace: String,
        @Query("page") page: Int = 1,
        @Query("pagelen") pageLen: Int = 50,
        @Query("sort") sort: String = "-updated_on"
    ): BitbucketPage<BitbucketRepository>
}

data class BitbucketPage<T>(
    @SerializedName("values") val values: List<T>,
    @SerializedName("next") val nextPageUrl: String?,
    @SerializedName("size") val totalSize: Int?
)

data class BitbucketRepository(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("name") val name: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("description") val description: String?,
    @SerializedName("is_private") val isPrivate: Boolean,
    @SerializedName("links") val links: BitbucketLinks?
)

data class BitbucketLinks(
    @SerializedName("clone") val clone: List<BitbucketCloneLink>?
) {
    fun httpsCloneUrl(): String? =
        clone?.firstOrNull { it.name == "https" }?.href

    fun sshCloneUrl(): String? =
        clone?.firstOrNull { it.name == "ssh" }?.href
}

data class BitbucketCloneLink(
    @SerializedName("name") val name: String,
    @SerializedName("href") val href: String
)
