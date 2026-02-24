package com.rafgittools.data.auth

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenRefreshManagerTest {

    private val context: Context = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val tokenRefreshManager = TokenRefreshManager(context, authRepository)

    @Test
    fun `refreshOAuthToken returns failure result and does not throw`() = runTest {
        val result = tokenRefreshManager.refreshOAuthToken(
            clientId = "client-id",
            clientSecret = "client-secret",
            refreshToken = "refresh-token"
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalStateException)
        assertEquals(
            "OAuth token refresh not yet supported. GitHub PATs don't use refresh tokens. " +
                "Use OAuthDeviceFlowManager.startDeviceFlow() to re-authenticate.",
            exception?.message
        )
    }
}
