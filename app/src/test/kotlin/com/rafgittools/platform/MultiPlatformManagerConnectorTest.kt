package com.rafgittools.platform

import com.rafgittools.data.forgejo.ForgejoRepository
import com.rafgittools.data.gitea.GiteaSshKey
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Unit tests for new connectors: Forgejo and Gitea SSH
 *
 * Tests validate:
 * - Token validation logic
 * - URL format validation
 * - Error handling (NotConfigured, AuthenticationError, NetworkError)
 * - Provider enum includes new connectors
 */
class MultiPlatformManagerConnectorTest {

    @Before
    fun setup() {
        // Initialize any test fixtures
    }

    @Test
    fun `Provider enum contains Forgejo`() {
        val providers = MultiPlatformManager.Provider.values()
        assert(providers.contains(MultiPlatformManager.Provider.FORGEJO))
    }

    @Test
    fun `Provider enum contains Gitea SSH`() {
        val providers = MultiPlatformManager.Provider.values()
        assert(providers.contains(MultiPlatformManager.Provider.GITEA_SSH))
    }

    @Test
    fun `queryForgejoRepositories returns NotConfigured when token is blank`() = runBlocking {
        val result = MultiPlatformManager.queryForgejoRepositories(
            token = "",
            baseUrl = "https://forgejo.example.com"
        )
        assertIs<MultiPlatformManager.ProviderQueryResult.NotConfigured>(result)
        assertEquals(MultiPlatformManager.Provider.FORGEJO, result.provider)
    }

    @Test
    fun `queryForgejoRepositories returns NotConfigured when baseUrl is blank`() = runBlocking {
        val result = MultiPlatformManager.queryForgejoRepositories(
            token = "test-token",
            baseUrl = ""
        )
        assertIs<MultiPlatformManager.ProviderQueryResult.NotConfigured>(result)
        assertEquals(MultiPlatformManager.Provider.FORGEJO, result.provider)
    }

    @Test
    fun `queryForgejoRepositories returns NotConfigured for invalid URL`() = runBlocking {
        val result = MultiPlatformManager.queryForgejoRepositories(
            token = "test-token",
            baseUrl = "not-a-url"
        )
        assertIs<MultiPlatformManager.ProviderQueryResult.NotConfigured>(result)
    }

    @Test
    fun `queryGiteaSshKeys returns NotConfigured when token is blank`() = runBlocking {
        val result = MultiPlatformManager.queryGiteaSshKeys(
            token = "",
            baseUrl = "https://gitea.example.com"
        )
        assertIs<MultiPlatformManager.ProviderQueryResult.NotConfigured>(result)
        assertEquals(MultiPlatformManager.Provider.GITEA_SSH, result.provider)
    }

    @Test
    fun `queryGiteaSshKeys returns NotConfigured when baseUrl is blank`() = runBlocking {
        val result = MultiPlatformManager.queryGiteaSshKeys(
            token = "test-token",
            baseUrl = ""
        )
        assertIs<MultiPlatformManager.ProviderQueryResult.NotConfigured>(result)
        assertEquals(MultiPlatformManager.Provider.GITEA_SSH, result.provider)
    }

    @Test
    fun `configuredProviders includes Forgejo when token provided`() {
        val providers = MultiPlatformManager.configuredProviders(
            forgejoToken = "test-token"
        )
        assert(providers.contains(MultiPlatformManager.Provider.FORGEJO))
    }

    @Test
    fun `configuredProviders includes Gitea SSH when token provided`() {
        val providers = MultiPlatformManager.configuredProviders(
            giteaSshToken = "test-token"
        )
        assert(providers.contains(MultiPlatformManager.Provider.GITEA_SSH))
    }

    @Test
    fun `configuredProviders excludes providers without tokens`() {
        val providers = MultiPlatformManager.configuredProviders(
            forgejoToken = "",
            giteaSshToken = ""
        )
        assert(!providers.contains(MultiPlatformManager.Provider.FORGEJO))
        assert(!providers.contains(MultiPlatformManager.Provider.GITEA_SSH))
    }

    @Test
    fun `configuredProviders handles multiple providers`() {
        val providers = MultiPlatformManager.configuredProviders(
            gitLabToken = "gitlab-token",
            forgejoToken = "forgejo-token",
            giteaSshToken = "gitea-token",
            azureToken = "azure-token"
        )
        assertEquals(4, providers.size)
        assert(providers.contains(MultiPlatformManager.Provider.GITLAB))
        assert(providers.contains(MultiPlatformManager.Provider.FORGEJO))
        assert(providers.contains(MultiPlatformManager.Provider.GITEA_SSH))
        assert(providers.contains(MultiPlatformManager.Provider.AZURE_DEVOPS))
    }
}
