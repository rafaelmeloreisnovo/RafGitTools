package com.rafgittools.platform

import com.rafgittools.data.bitbucket.BitbucketApiService
import com.rafgittools.data.gitlab.GitLabApiService
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

/**
 * Git-hosting provider abstraction layer.
 *
 * GitHub is implemented through the existing GithubRepository/GithubApiService.
 * GitLab (v4 API) and Bitbucket (v2 API) are implemented via their own Retrofit
 * service interfaces. Gitea and Azure DevOps return NotImplemented until added.
 */
object MultiPlatformManager {

    init {
        runCatching { System.loadLibrary("rafcore") }
    }

    external fun nativeAsmHealth(): Int
    external fun nativeAbiMask(): Int

    data class HostedRepository(
        val id: String,
        val name: String,
        val fullName: String,
        val description: String?,
        val cloneUrl: String,
        val sshUrl: String?,
        val isPrivate: Boolean,
        val provider: Provider
    )

    enum class Provider { GITHUB, GITLAB, BITBUCKET, GITEA, AZURE_DEVOPS }

    sealed interface ProviderQueryResult {
        data class Success(val repositories: List<HostedRepository>) : ProviderQueryResult
        data class NotConfigured(val provider: Provider, val reason: String) : ProviderQueryResult
        data class NotImplemented(val provider: Provider, val integrationPath: String) : ProviderQueryResult
        data class AuthenticationError(val provider: Provider, val message: String) : ProviderQueryResult
        data class NetworkError(val provider: Provider, val message: String) : ProviderQueryResult
    }

    fun queryGitLabProjects(
        token: String = "",
        baseUrl: String = "https://gitlab.com"
    ): ProviderQueryResult {
        if (token.isBlank()) {
            return ProviderQueryResult.NotConfigured(Provider.GITLAB, "GitLab token is missing")
        }
        if (!isHttpUrl(baseUrl)) {
            return ProviderQueryResult.NotConfigured(Provider.GITLAB, "GitLab base URL is invalid")
        }
        return try {
            val service = buildRetrofit(baseUrl).create(GitLabApiService::class.java)
            val projects = runBlocking { service.getUserProjects(token) }
            ProviderQueryResult.Success(projects.map { p ->
                HostedRepository(
                    id = p.id.toString(),
                    name = p.name,
                    fullName = p.fullPath,
                    description = p.description,
                    cloneUrl = p.cloneUrlHttp,
                    sshUrl = p.cloneUrlSsh,
                    isPrivate = p.isPrivate,
                    provider = Provider.GITLAB
                )
            })
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                ProviderQueryResult.AuthenticationError(Provider.GITLAB, "GitLab auth failed: ${e.message()}")
            } else {
                ProviderQueryResult.NetworkError(Provider.GITLAB, "GitLab HTTP ${e.code()}: ${e.message()}")
            }
        } catch (e: IOException) {
            ProviderQueryResult.NetworkError(Provider.GITLAB, "GitLab network error: ${e.message}")
        }
    }

    fun queryBitbucketRepositories(
        accessToken: String = "",
        workspace: String = ""
    ): ProviderQueryResult {
        if (accessToken.isBlank() || workspace.isBlank()) {
            return ProviderQueryResult.NotConfigured(
                Provider.BITBUCKET,
                "Bitbucket access token and workspace are required"
            )
        }
        return try {
            val service = buildRetrofit("https://api.bitbucket.org").create(BitbucketApiService::class.java)
            val page = runBlocking {
                service.getWorkspaceRepositories(
                    authorization = "Bearer $accessToken",
                    workspace = workspace
                )
            }
            ProviderQueryResult.Success(page.values.map { r ->
                HostedRepository(
                    id = r.uuid,
                    name = r.name,
                    fullName = r.fullName,
                    description = r.description,
                    cloneUrl = r.links?.httpsCloneUrl() ?: "",
                    sshUrl = r.links?.sshCloneUrl(),
                    isPrivate = r.isPrivate,
                    provider = Provider.BITBUCKET
                )
            })
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                ProviderQueryResult.AuthenticationError(Provider.BITBUCKET, "Bitbucket auth failed: ${e.message()}")
            } else {
                ProviderQueryResult.NetworkError(Provider.BITBUCKET, "Bitbucket HTTP ${e.code()}: ${e.message()}")
            }
        } catch (e: IOException) {
            ProviderQueryResult.NetworkError(Provider.BITBUCKET, "Bitbucket network error: ${e.message}")
        }
    }

    fun queryGiteaRepositories(
        token: String = "",
        baseUrl: String = ""
    ): ProviderQueryResult {
        if (token.isBlank() || !isHttpUrl(baseUrl)) {
            return ProviderQueryResult.NotConfigured(
                Provider.GITEA,
                "Gitea token and valid instance URL are required"
            )
        }
        return ProviderQueryResult.NotImplemented(
            Provider.GITEA,
            "Add configurable Gitea API adapter and GET /api/v1/user/repos"
        )
    }

    fun queryAzureDevOpsRepos(
        token: String = "",
        organization: String = "",
        project: String = ""
    ): ProviderQueryResult {
        if (token.isBlank() || organization.isBlank() || project.isBlank()) {
            return ProviderQueryResult.NotConfigured(
                Provider.AZURE_DEVOPS,
                "Azure token, organization and project are required"
            )
        }
        return ProviderQueryResult.NotImplemented(
            Provider.AZURE_DEVOPS,
            "Add Azure DevOps API 7.0 adapter and GET /{project}/_apis/git/repositories"
        )
    }

    /**
     * Compatibility methods retained for existing callers.
     *
     * They now throw for non-success states instead of collapsing
     * NOT_IMPLEMENTED and NOT_CONFIGURED into an empty list.
     */
    @Deprecated("Use queryGitLabProjects for typed status")
    fun getGitLabProjects(token: String = "", baseUrl: String = "https://gitlab.com"): List<HostedRepository> =
        requireSuccess(queryGitLabProjects(token, baseUrl))

    @Deprecated("Use queryBitbucketRepositories for typed status")
    fun getBitbucketRepositories(accessToken: String = "", workspace: String = ""): List<HostedRepository> =
        requireSuccess(queryBitbucketRepositories(accessToken, workspace))

    @Deprecated("Use queryGiteaRepositories for typed status")
    fun getGiteaRepositories(token: String = "", baseUrl: String = ""): List<HostedRepository> =
        requireSuccess(queryGiteaRepositories(token, baseUrl))

    @Deprecated("Use queryAzureDevOpsRepos for typed status")
    fun getAzureDevOpsRepos(
        token: String = "",
        organization: String = "",
        project: String = ""
    ): List<HostedRepository> = requireSuccess(queryAzureDevOpsRepos(token, organization, project))

    fun configuredProviders(
        gitLabToken: String = "",
        bitbucketToken: String = "",
        giteaToken: String = "",
        azureToken: String = ""
    ): Set<Provider> = buildSet {
        if (gitLabToken.isNotEmpty()) add(Provider.GITLAB)
        if (bitbucketToken.isNotEmpty()) add(Provider.BITBUCKET)
        if (giteaToken.isNotEmpty()) add(Provider.GITEA)
        if (azureToken.isNotEmpty()) add(Provider.AZURE_DEVOPS)
    }

    private fun requireSuccess(result: ProviderQueryResult): List<HostedRepository> = when (result) {
        is ProviderQueryResult.Success -> result.repositories
        is ProviderQueryResult.NotConfigured -> throw IllegalStateException(result.reason)
        is ProviderQueryResult.NotImplemented -> throw UnsupportedOperationException(result.integrationPath)
        is ProviderQueryResult.AuthenticationError -> throw SecurityException(result.message)
        is ProviderQueryResult.NetworkError -> throw IllegalStateException(result.message)
    }

    private fun buildRetrofit(baseUrl: String): Retrofit {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun isHttpUrl(value: String): Boolean =
        value.startsWith("https://") || value.startsWith("http://127.0.0.1") || value.startsWith("http://localhost")
}

/** Returns true when the native assembler core library is loaded and healthy. */
fun isNativeAssemblerCoreReady(): Boolean =
    runCatching { MultiPlatformManager.nativeAsmHealth() >= 8 }.getOrDefault(false)
