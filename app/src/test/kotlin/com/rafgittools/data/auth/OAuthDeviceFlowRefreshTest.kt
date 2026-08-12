package com.rafgittools.data.auth

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class OAuthDeviceFlowRefreshTest {

    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val api: GitHubOAuthApi = mockk(relaxed = true)
    private val apiClient: GitHubOAuthApiClient = mockk(relaxed = true)
    private val oauthConfig: GitHubOAuthConfig = mockk(relaxed = true)
    private lateinit var manager: OAuthDeviceFlowManager

    @Before
    fun setup() {
        every { apiClient.api } returns api
        every { oauthConfig.requireClientId() } returns TEST_CLIENT_ID
        manager = OAuthDeviceFlowManager(authRepository, apiClient, oauthConfig)
    }

    @Test
    fun `stored device-flow refresh token rotates access and refresh credentials`() = runTest {
        coEvery { authRepository.getRefreshToken() } returns Result.success(OLD_REFRESH_TOKEN)
        coEvery { authRepository.getRefreshTokenExpiresAt() } returns System.currentTimeMillis() + 60_000L
        coEvery { authRepository.getUsername() } returns "rafael"
        coEvery {
            api.refreshToken(TEST_CLIENT_ID, "refresh_token", OLD_REFRESH_TOKEN)
        } returns TokenPollResponse(
            access_token = NEW_ACCESS_TOKEN,
            expires_in = 28_800L,
            refresh_token = NEW_REFRESH_TOKEN,
            refresh_token_expires_in = 15_897_600L,
            token_type = "bearer"
        )
        coEvery {
            authRepository.saveOAuthSession(
                NEW_ACCESS_TOKEN,
                "rafael",
                NEW_REFRESH_TOKEN,
                28_800L,
                15_897_600L
            )
        } returns Result.success(Unit)

        val result = manager.refreshStoredSession(OLD_ACCESS_TOKEN)

        assertThat(result.getOrNull()).isEqualTo(RefreshedSession(NEW_ACCESS_TOKEN, "rafael"))
        coVerify(exactly = 1) {
            api.refreshToken(TEST_CLIENT_ID, "refresh_token", OLD_REFRESH_TOKEN)
        }
        coVerify(exactly = 1) {
            authRepository.saveOAuthSession(
                NEW_ACCESS_TOKEN,
                "rafael",
                NEW_REFRESH_TOKEN,
                28_800L,
                15_897_600L
            )
        }
    }

    @Test
    fun `concurrent stale 401 reuses token already rotated by another request`() = runTest {
        coEvery { authRepository.getPat() } returns Result.success(NEW_ACCESS_TOKEN)
        coEvery { authRepository.getUsername() } returns "rafael"

        val result = manager.refreshStoredSession(OLD_ACCESS_TOKEN)

        assertThat(result.getOrNull()).isEqualTo(RefreshedSession(NEW_ACCESS_TOKEN, "rafael"))
        coVerify(exactly = 0) { authRepository.getRefreshToken() }
        coVerify(exactly = 0) { api.refreshToken(any(), any(), any()) }
        coVerify(exactly = 0) { authRepository.saveOAuthSession(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `session without refresh token never calls refresh endpoint`() = runTest {
        coEvery { authRepository.getRefreshToken() } returns Result.failure(Exception("No refresh token stored"))

        val result = manager.refreshStoredSession()

        assertThat(result.isFailure).isTrue()
        coVerify(exactly = 0) { api.refreshToken(any(), any(), any()) }
        coVerify(exactly = 0) { authRepository.saveOAuthSession(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `expired refresh token fails before network mutation`() = runTest {
        coEvery { authRepository.getRefreshToken() } returns Result.success(OLD_REFRESH_TOKEN)
        coEvery { authRepository.getRefreshTokenExpiresAt() } returns System.currentTimeMillis() - 1L

        val result = manager.refreshStoredSession()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("expired")
        coVerify(exactly = 0) { api.refreshToken(any(), any(), any()) }
        coVerify(exactly = 0) { authRepository.saveOAuthSession(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `bad refresh token response is not persisted`() = runTest {
        coEvery { authRepository.getRefreshToken() } returns Result.success(OLD_REFRESH_TOKEN)
        coEvery { authRepository.getRefreshTokenExpiresAt() } returns null
        coEvery {
            api.refreshToken(TEST_CLIENT_ID, "refresh_token", OLD_REFRESH_TOKEN)
        } returns TokenPollResponse(
            error = "bad_refresh_token",
            error_description = "The refresh token is invalid"
        )

        val result = manager.refreshStoredSession()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("invalid")
        coVerify(exactly = 0) { authRepository.saveOAuthSession(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `refresh response without new refresh token clears future refresh capability on save`() = runTest {
        coEvery { authRepository.getRefreshToken() } returns Result.success(OLD_REFRESH_TOKEN)
        coEvery { authRepository.getRefreshTokenExpiresAt() } returns null
        coEvery { authRepository.getUsername() } returns "rafael"
        coEvery {
            api.refreshToken(TEST_CLIENT_ID, "refresh_token", OLD_REFRESH_TOKEN)
        } returns TokenPollResponse(
            access_token = NEW_ACCESS_TOKEN,
            expires_in = 28_800L,
            refresh_token = null,
            refresh_token_expires_in = null
        )
        coEvery {
            authRepository.saveOAuthSession(NEW_ACCESS_TOKEN, "rafael", null, 28_800L, null)
        } returns Result.success(Unit)

        val result = manager.refreshStoredSession()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) {
            authRepository.saveOAuthSession(NEW_ACCESS_TOKEN, "rafael", null, 28_800L, null)
        }
    }

    companion object {
        private const val TEST_CLIENT_ID = "Iv1.TEST_CLIENT_ID_NOT_REAL"
        private const val OLD_ACCESS_TOKEN = "ghu_OLD_ACCESS_VALUE_NOT_REAL_1234567890"
        private const val OLD_REFRESH_TOKEN = "ghr_TEST_REFRESH_VALUE_NOT_REAL_1234567890"
        private const val NEW_REFRESH_TOKEN = "ghr_ROTATED_REFRESH_VALUE_NOT_REAL_1234567890"
        private const val NEW_ACCESS_TOKEN = "ghu_ROTATED_ACCESS_VALUE_NOT_REAL_1234567890"
    }
}
