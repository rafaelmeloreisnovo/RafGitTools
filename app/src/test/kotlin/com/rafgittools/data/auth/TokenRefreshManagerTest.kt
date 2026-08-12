package com.rafgittools.data.auth

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TokenRefreshManagerTest {

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val manager = TokenRefreshManager(authRepository)

    @Test
    fun `401 invalidates persistent auth state and requires reauthentication`() = runTest {
        coEvery { authRepository.clearAuthState() } returns Result.success(Unit)

        val state = manager.handleHttpResponse(
            responseCode = 401,
            rateLimitRemaining = null,
            rateLimitReset = null
        )

        assertThat(state).isEqualTo(
            TokenRefreshManager.TokenState.InvalidCredential(persistentStateCleared = true)
        )
        coVerify(exactly = 1) { authRepository.clearAuthState() }
    }

    @Test
    fun `401 still reports invalid credential when persistent cleanup fails`() = runTest {
        coEvery { authRepository.clearAuthState() } returns Result.failure(Exception("storage unavailable"))

        val state = manager.handleHttpResponse(
            responseCode = 401,
            rateLimitRemaining = null,
            rateLimitReset = null
        )

        assertThat(state).isEqualTo(
            TokenRefreshManager.TokenState.InvalidCredential(persistentStateCleared = false)
        )
        coVerify(exactly = 1) { authRepository.clearAuthState() }
    }

    @Test
    fun `rate limited 403 preserves session and exposes reset epoch`() = runTest {
        val state = manager.handleHttpResponse(
            responseCode = 403,
            rateLimitRemaining = "0",
            rateLimitReset = "1999999999"
        )

        assertThat(state).isEqualTo(TokenRefreshManager.TokenState.RateLimited(1999999999L))
        coVerify(exactly = 0) { authRepository.clearAuthState() }
    }

    @Test
    fun `ordinary 403 is forbidden without destroying auth state`() = runTest {
        val state = manager.handleHttpResponse(
            responseCode = 403,
            rateLimitRemaining = "42",
            rateLimitReset = null
        )

        assertThat(state).isEqualTo(TokenRefreshManager.TokenState.Forbidden)
        coVerify(exactly = 0) { authRepository.clearAuthState() }
    }

    @Test
    fun `successful response leaves token state valid`() = runTest {
        val state = manager.handleHttpResponse(
            responseCode = 200,
            rateLimitRemaining = null,
            rateLimitReset = null
        )

        assertThat(state).isEqualTo(TokenRefreshManager.TokenState.Valid)
        coVerify(exactly = 0) { authRepository.clearAuthState() }
    }

    @Test
    fun `oauth scopes parser is bounded to provided header value`() {
        assertThat(manager.parseScopesFromHeader("repo, read:user, notifications"))
            .containsExactly("repo", "read:user", "notifications")
        assertThat(manager.parseScopesFromHeader(null)).isEmpty()
        assertThat(manager.parseScopesFromHeader("   ")).isEmpty()
    }
}
