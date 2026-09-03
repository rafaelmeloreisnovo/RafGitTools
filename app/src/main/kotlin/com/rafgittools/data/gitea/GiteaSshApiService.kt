package com.rafgittools.data.gitea

import com.google.gson.annotations.SerializedName
import retrofit2.http.*

/**
 * Gitea SSH Key Management API
 *
 * Extends GiteaApiService with SSH key discovery and management.
 * Allows RafGitTools to enumerate SSH keys stored in Gitea and use them
 * for cloning private repositories over SSH.
 */
interface GiteaSshApiService {

    // User SSH Keys
    @GET("user/keys")
    suspend fun getUserSshKeys(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): List<GiteaSshKey>

    @POST("user/keys")
    suspend fun addSshKey(
        @Header("Authorization") authorization: String,
        @Body request: AddGiteaSshKeyRequest
    ): GiteaSshKey

    @DELETE("user/keys/{keyId}")
    suspend fun deleteSshKey(
        @Header("Authorization") authorization: String,
        @Path("keyId") keyId: Long
    )

    @GET("user/keys/{keyId}")
    suspend fun getSshKey(
        @Header("Authorization") authorization: String,
        @Path("keyId") keyId: Long
    ): GiteaSshKey

    // Repository deployment keys (for CI/CD and automated access)
    @GET("repos/{owner}/{repo}/keys")
    suspend fun getRepositoryDeployKeys(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): List<GiteaDeployKey>

    @POST("repos/{owner}/{repo}/keys")
    suspend fun addRepositoryDeployKey(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: AddGiteaDeployKeyRequest
    ): GiteaDeployKey

    @DELETE("repos/{owner}/{repo}/keys/{keyId}")
    suspend fun deleteRepositoryDeployKey(
        @Header("Authorization") authorization: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("keyId") keyId: Long
    )
}

/**
 * Gitea SSH Key model (user key)
 */
data class GiteaSshKey(
    val id: Long,
    val key_id: Long? = null,
    val name: String? = null,
    val fingerprint: String? = null,
    @SerializedName("public_key")
    val publicKey: String,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("expires_at")
    val expiresAt: String? = null,
    @SerializedName("read_only")
    val readOnly: Boolean = false,
    val title: String? = null,
    @SerializedName("url")
    val url: String? = null
)

/**
 * Gitea Deploy Key model (for automated access to repositories)
 */
data class GiteaDeployKey(
    val id: Long,
    val key_id: Long? = null,
    val name: String? = null,
    val fingerprint: String? = null,
    @SerializedName("public_key")
    val publicKey: String,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("read_only")
    val readOnly: Boolean = false,
    val repository: GiteaRepositoryMinimal? = null,
    val title: String? = null
)

/**
 * Minimal Gitea repository (used in deploy keys)
 */
data class GiteaRepositoryMinimal(
    val id: Long,
    val name: String,
    @SerializedName("full_name")
    val fullName: String
)

/**
 * Request to add a new SSH key
 */
data class AddGiteaSshKeyRequest(
    val title: String,
    @SerializedName("key")
    val publicKey: String,
    @SerializedName("read_only")
    val readOnly: Boolean = false,
    @SerializedName("expires_at")
    val expiresAt: String? = null
)

/**
 * Request to add a new repository deploy key
 */
data class AddGiteaDeployKeyRequest(
    val title: String,
    @SerializedName("key")
    val publicKey: String,
    @SerializedName("read_only")
    val readOnly: Boolean = true
)
