package com.rafgittools.platform

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

class MultiPlatformManagerTest {

    @Test
    fun `missing configuration is not an empty success`() = runTest {
        assertIs<MultiPlatformManager.ProviderQueryResult.NotConfigured>(
            MultiPlatformManager.queryGitLabProjects()
        )
    }

    @Test
    fun `configured provider with invalid endpoint fails closed before network`() = runTest {
        assertIs<MultiPlatformManager.ProviderQueryResult.NotConfigured>(
            MultiPlatformManager.queryGitLabProjects(
                token = "token",
                baseUrl = "not-a-url"
            )
        )
    }
}
