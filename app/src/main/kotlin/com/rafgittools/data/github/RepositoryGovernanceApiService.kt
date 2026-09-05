package com.rafgittools.data.github

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Narrow provider-bound surface for repository configuration + security governance.
 *
 * The screen consumes Response<T> for endpoints where 404/403 are meaningful evidence
 * states, so provider denial is preserved instead of being coerced into false.
 */
interface RepositoryGovernanceApiService {

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("user/repos")
    suspend fun listRepositories(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100,
        @Query("sort") sort: String = "updated",
        @Query("affiliation") affiliation: String = "owner,collaborator,organization_member"
    ): List<GovernanceRepositorySummary>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GovernanceRepositoryDetails

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @PATCH("repos/{owner}/{repo}")
    suspend fun updateRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: UpdateRepositoryGovernanceRequest
    ): GovernanceRepositoryDetails

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("repos/{owner}/{repo}/branches/{branch}/protection")
    suspend fun getBranchProtection(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String
    ): Response<BranchProtectionSnapshot>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @PUT("repos/{owner}/{repo}/branches/{branch}/protection")
    suspend fun updateBranchProtection(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
        @Body request: BranchProtectionRequest
    ): Response<BranchProtectionSnapshot>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @DELETE("repos/{owner}/{repo}/branches/{branch}/protection")
    suspend fun deleteBranchProtection(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String
    ): Response<Unit>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("repos/{owner}/{repo}/vulnerability-alerts")
    suspend fun checkVulnerabilityAlerts(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @PUT("repos/{owner}/{repo}/vulnerability-alerts")
    suspend fun enableVulnerabilityAlerts(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @DELETE("repos/{owner}/{repo}/vulnerability-alerts")
    suspend fun disableVulnerabilityAlerts(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("repos/{owner}/{repo}/automated-security-fixes")
    suspend fun checkAutomatedSecurityFixes(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @PUT("repos/{owner}/{repo}/automated-security-fixes")
    suspend fun enableAutomatedSecurityFixes(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @DELETE("repos/{owner}/{repo}/automated-security-fixes")
    suspend fun disableAutomatedSecurityFixes(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>
}
