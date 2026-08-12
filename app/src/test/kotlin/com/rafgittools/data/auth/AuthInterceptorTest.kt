package com.rafgittools.data.auth

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Test

class AuthInterceptorTest {

    private val tokenRefreshManager: TokenRefreshManager = mockk(relaxed = true)

    @Test
    fun `401 without refresh clears rejected credential from memory`() {
        val cache = AuthTokenCache().apply { token = TEST_CREDENTIAL }
        coEvery {
            tokenRefreshManager.handleHttpResponse(401, null, null)
        } returns TokenRefreshManager.TokenState.InvalidCredential(persistentStateCleared = true)

        val original = Request.Builder().url("https://api.github.com/user").build()
        val response: Response = mockk(relaxed = true) {
            every { code } returns 401
            every { header("X-RateLimit-Remaining") } returns null
            every { header("X-RateLimit-Reset") } returns null
        }
        val requestSlot = slot<Request>()
        val chain: Interceptor.Chain = mockk {
            every { request() } returns original
            every { proceed(capture(requestSlot)) } returns response
        }

        val returned = AuthInterceptor(cache, tokenRefreshManager).intercept(chain)

        assertThat(returned).isSameInstanceAs(response)
        assertThat(cache.token).isNull()
        assertThat(requestSlot.captured.header("Authorization")).isEqualTo("Bearer $TEST_CREDENTIAL")
        assertThat(requestSlot.captured.header("Accept")).isEqualTo("application/vnd.github+json")
        assertThat(requestSlot.captured.header("X-GitHub-Api-Version")).isEqualTo("2022-11-28")
        coVerify(exactly = 1) { tokenRefreshManager.handleHttpResponse(401, null, null) }
    }

    @Test
    fun `successful refresh retries original request exactly once with rotated token`() {
        val cache = AuthTokenCache().apply { token = TEST_CREDENTIAL }
        coEvery {
            tokenRefreshManager.handleHttpResponse(401, null, null)
        } returns TokenRefreshManager.TokenState.Refreshed(ROTATED_CREDENTIAL)

        val original = Request.Builder().url("https://api.github.com/user/repos").build()
        val firstResponse: Response = mockk(relaxed = true) {
            every { code } returns 401
            every { header("X-RateLimit-Remaining") } returns null
            every { header("X-RateLimit-Reset") } returns null
        }
        val retryResponse: Response = mockk(relaxed = true) {
            every { code } returns 200
        }
        val requests = mutableListOf<Request>()
        val chain: Interceptor.Chain = mockk {
            every { request() } returns original
            every { proceed(capture(requests)) } returnsMany listOf(firstResponse, retryResponse)
        }

        val returned = AuthInterceptor(cache, tokenRefreshManager).intercept(chain)

        assertThat(returned).isSameInstanceAs(retryResponse)
        assertThat(cache.token).isEqualTo(ROTATED_CREDENTIAL)
        assertThat(requests).hasSize(2)
        assertThat(requests[0].header("Authorization")).isEqualTo("Bearer $TEST_CREDENTIAL")
        assertThat(requests[1].header("Authorization")).isEqualTo("Bearer $ROTATED_CREDENTIAL")
        verify(exactly = 1) { firstResponse.close() }
        coVerify(exactly = 1) { tokenRefreshManager.handleHttpResponse(401, null, null) }
        coVerify(exactly = 0) { tokenRefreshManager.invalidateSession() }
    }

    @Test
    fun `second 401 after successful refresh invalidates session without refreshing again`() {
        val cache = AuthTokenCache().apply { token = TEST_CREDENTIAL }
        coEvery {
            tokenRefreshManager.handleHttpResponse(401, null, null)
        } returns TokenRefreshManager.TokenState.Refreshed(ROTATED_CREDENTIAL)
        coEvery { tokenRefreshManager.invalidateSession() } returns
            TokenRefreshManager.TokenState.InvalidCredential(persistentStateCleared = true)

        val original = Request.Builder().url("https://api.github.com/user").build()
        val firstResponse: Response = mockk(relaxed = true) {
            every { code } returns 401
            every { header("X-RateLimit-Remaining") } returns null
            every { header("X-RateLimit-Reset") } returns null
        }
        val retryResponse: Response = mockk(relaxed = true) {
            every { code } returns 401
        }
        val requests = mutableListOf<Request>()
        val chain: Interceptor.Chain = mockk {
            every { request() } returns original
            every { proceed(capture(requests)) } returnsMany listOf(firstResponse, retryResponse)
        }

        val returned = AuthInterceptor(cache, tokenRefreshManager).intercept(chain)

        assertThat(returned).isSameInstanceAs(retryResponse)
        assertThat(cache.token).isNull()
        assertThat(requests).hasSize(2)
        coVerify(exactly = 1) { tokenRefreshManager.handleHttpResponse(401, null, null) }
        coVerify(exactly = 1) { tokenRefreshManager.invalidateSession() }
    }

    @Test
    fun `rate limited 403 preserves credential in memory`() {
        val cache = AuthTokenCache().apply { token = TEST_CREDENTIAL }
        coEvery {
            tokenRefreshManager.handleHttpResponse(403, "0", "1999999999")
        } returns TokenRefreshManager.TokenState.RateLimited(1999999999L)

        val original = Request.Builder().url("https://api.github.com/user/repos").build()
        val response: Response = mockk(relaxed = true) {
            every { code } returns 403
            every { header("X-RateLimit-Remaining") } returns "0"
            every { header("X-RateLimit-Reset") } returns "1999999999"
        }
        val chain: Interceptor.Chain = mockk {
            every { request() } returns original
            every { proceed(any()) } returns response
        }

        AuthInterceptor(cache, tokenRefreshManager).intercept(chain)

        assertThat(cache.token).isEqualTo(TEST_CREDENTIAL)
        coVerify(exactly = 1) {
            tokenRefreshManager.handleHttpResponse(403, "0", "1999999999")
        }
    }

    @Test
    fun `request without cached credential bypasses lifecycle manager`() {
        val cache = AuthTokenCache()
        val original = Request.Builder().url("https://api.github.com/user").build()
        val response: Response = mockk(relaxed = true)
        val requestSlot = slot<Request>()
        val chain: Interceptor.Chain = mockk {
            every { request() } returns original
            every { proceed(capture(requestSlot)) } returns response
        }

        AuthInterceptor(cache, tokenRefreshManager).intercept(chain)

        assertThat(requestSlot.captured.header("Authorization")).isNull()
        coVerify(exactly = 0) { tokenRefreshManager.handleHttpResponse(any(), any(), any()) }
        coVerify(exactly = 0) { tokenRefreshManager.invalidateSession() }
        verify(exactly = 1) { chain.proceed(original) }
    }

    companion object {
        private const val TEST_CREDENTIAL = "TEST_CREDENTIAL_VALUE_NOT_A_REAL_TOKEN_1234567890"
        private const val ROTATED_CREDENTIAL = "ROTATED_CREDENTIAL_VALUE_NOT_A_REAL_TOKEN_1234567890"
    }
}
