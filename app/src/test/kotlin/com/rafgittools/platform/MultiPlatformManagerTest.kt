package com.rafgittools.platform

import kotlin.test.Test
import kotlin.test.assertIs

class MultiPlatformManagerTest {

    @Test
    fun `missing configuration is not an empty success`() {
        assertIs<MultiPlatformManager.ProviderQueryResult.NotConfigured>(
            MultiPlatformManager.queryGitLabProjects()
        )
    }

    @Test
    fun `configured provider without adapter is explicitly not implemented`() {
        assertIs<MultiPlatformManager.ProviderQueryResult.NotImplemented>(
            MultiPlatformManager.queryGitLabProjects(token = "token")
        )
    }
}
