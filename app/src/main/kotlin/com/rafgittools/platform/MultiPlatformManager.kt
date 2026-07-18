package com.rafgittools.platform

/**
 * MultiPlatformManager — Git-hosting provider abstraction layer.
 *
 * Provides a unified interface for interacting with multiple Git hosting
 * providers beyond GitHub. Each provider is represented by a [HostedRepository]
 * value type. HTTP calls are intentionally left to the caller's Retrofit/OkHttp
 * setup so this manager stays free of Android context dependencies.
 *
 * Current implementation status:
 *   - GitHub: fully implemented via [GithubRepository] / [GithubApiService]
 *   - GitLab: skeleton ready — needs GitLabApiService + Retrofit integration
 *   - Bitbucket: skeleton ready — needs BitbucketApiService + Retrofit integration
 *   - Gitea: skeleton ready — needs GiteaApiService + Retrofit integration
 *   - Azure DevOps: skeleton ready — needs AzureDevOpsApiService + Retrofit integration
 *
 * Native assembly health check: the `rafcore` shared library exposes two JNI
 * symbols used for platform diagnostics. Missing library → graceful no-op.
 */
object MultiPlatformManager {

    init {
        runCatching { System.loadLibrary("rafcore") }
    }

    external fun nativeAsmHealth(): Int
    external fun nativeAbiMask(): Int

    // ─── Shared domain type ────────────────────────────────────────────────

    /**
     * Minimal repository descriptor returned by all providers.
     *
     * Callers can inspect [provider] to decide which API client to use for
     * deeper queries (PRs, issues, releases, etc.).
     */
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

    // ─── Provider stubs ────────────────────────────────────────────────────

    /**
     * Retrieve repositories for the authenticated GitLab user.
     *
     * Integration path:
     *   1. Add `com.squareup.retrofit2` GitLab API service interface
     *   2. Inject base URL `https://gitlab.com/api/v4/` (or self-hosted)
     *   3. Call `GET /projects?membership=true&per_page=100`
     *   4. Map response to [HostedRepository] list with provider = GITLAB
     *
     * @param token  GitLab personal access token (scope: `read_api`)
     * @param baseUrl GitLab instance base URL; defaults to gitlab.com
     */
    fun getGitLabProjects(
        token: String = "",
        baseUrl: String = "https://gitlab.com"
    ): List<HostedRepository> {
        // STUB: GitLab API not implemented. Returns empty list.
        return emptyList()
    }

    /**
     * Retrieve repositories for the authenticated Bitbucket user.
     *
     * Integration path:
     *   1. Add Bitbucket Cloud REST API service interface (v2.0)
     *   2. Base URL: `https://api.bitbucket.org/2.0/`
     *   3. Call `GET /repositories/{workspace}` with OAuth 2.0 Bearer token
     *   4. Map response to [HostedRepository] with provider = BITBUCKET
     *
     * @param accessToken  Bitbucket OAuth 2.0 access token
     * @param workspace    Bitbucket workspace slug
     */
    fun getBitbucketRepositories(
        accessToken: String = "",
        workspace: String = ""
    ): List<HostedRepository> {
        // STUB: Bitbucket API not implemented. Returns empty list.
        return emptyList()
    }

    /**
     * Retrieve repositories for the authenticated Gitea user.
     *
     * Integration path:
     *   1. Add Gitea Swagger API service interface
     *   2. Base URL configurable per self-hosted instance
     *   3. Call `GET /api/v1/repos/search?token=<token>&limit=50`
     *   4. Map response to [HostedRepository] with provider = GITEA
     *
     * @param token    Gitea API token
     * @param baseUrl  Gitea instance base URL (e.g. "https://gitea.example.com")
     */
    fun getGiteaRepositories(
        token: String = "",
        baseUrl: String = ""
    ): List<HostedRepository> {
        // STUB: Gitea API not implemented. Returns empty list.
        return emptyList()
    }

    /**
     * Retrieve repositories for the authenticated Azure DevOps user.
     *
     * Integration path:
     *   1. Add Azure DevOps REST API service interface (api-version 7.0)
     *   2. Base URL: `https://dev.azure.com/{organization}/`
     *   3. Call `GET /{project}/_apis/git/repositories`
     *   4. Map response to [HostedRepository] with provider = AZURE_DEVOPS
     *
     * @param token        Azure DevOps Personal Access Token
     * @param organization Azure DevOps organisation slug
     * @param project      Azure DevOps project name
     */
    fun getAzureDevOpsRepos(
        token: String = "",
        organization: String = "",
        project: String = ""
    ): List<HostedRepository> {
        // STUB: Azure DevOps API not implemented. Returns empty list.
        return emptyList()
    }

    // ─── Capability queries ────────────────────────────────────────────────

    /** Returns the set of providers that have a non-empty token/configuration. */
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
}

/** Returns true when the native assembler core library is loaded and healthy. */
fun isNativeAssemblerCoreReady(): Boolean =
    runCatching { MultiPlatformManager.nativeAsmHealth() >= 8 }.getOrDefault(false)
