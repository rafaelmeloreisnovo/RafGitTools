package com.rafgittools.data.auth

import com.rafgittools.BuildConfig

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Retrofit
import retrofit2.http.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OAuth Device Flow Manager — P33-23
 *
 * Implements GitHub OAuth Device Authorization Grant (RFC 8628).
 * Allows users to authenticate without a browser redirect:
 * 1. App requests device_code from GitHub
 * 2. User visits github.com/login/device and enters user_code
 * 3. App polls until authorized or expired
 *
 * Docs: https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#device-flow
 */
@Singleton
class OAuthDeviceFlowManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val GITHUB_OAUTH_URL = "https://github.com/"
        private val CLIENT_ID get() = BuildConfig.GITHUB_CLIENT_ID // FIX L6: never hardcode OAuth client ID
        private const val CLIENT_ID_ERROR_MESSAGE =
            "GitHub OAuth is not configured for this build. Set a valid GITHUB_CLIENT_ID in gradle.properties or CI secrets (GITHUB_CLIENT_ID_DEV / GITHUB_CLIENT_ID_PRODUCTION) and rebuild the app."
        private const val SCOPE = "repo,read:user,notifications"
        private const val POLL_INTERVAL_MS = 5_000L
        private const val MAX_POLLS = 60 // 5 min total
    }

    private val oauthApi: GitHubOAuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(GITHUB_OAUTH_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(GitHubOAuthApi::class.java)
    }

    /**
     * Emits [DeviceFlowState] events through the full OAuth device flow.
     * Collect this Flow in your ViewModel; cancel to abort polling.
     */
    fun startDeviceFlow(): Flow<DeviceFlowState> = flow {
        val clientId = requireClientId()
        if (clientId == null) {
            emit(DeviceFlowState.Error(CLIENT_ID_ERROR_MESSAGE))
            return@flow
        }
        emit(DeviceFlowState.Requesting)

        // Step 1: Request device + user codes
        val codeResponse = try {
            oauthApi.requestDeviceCode(clientId = clientId, scope = SCOPE)
        } catch (e: Exception) {
            emit(DeviceFlowState.Error("Failed to start OAuth flow: ${e.message}"))
            return@flow
        }

        emit(DeviceFlowState.PendingUserAction(
            userCode = codeResponse.user_code,
            verificationUri = codeResponse.verification_uri,
            expiresInSeconds = codeResponse.expires_in
        ))

        // Step 2: Poll for access token
        val intervalMs = (codeResponse.interval * 1000L).coerceAtLeast(POLL_INTERVAL_MS)
        var polls = 0

        while (polls < MAX_POLLS) {
            delay(intervalMs)
            polls++

            val tokenResponse = try {
                oauthApi.pollForToken(
                    clientId = clientId,
                    deviceCode = codeResponse.device_code,
                    grantType = "urn:ietf:params:oauth:grant-type:device_code"
                )
            } catch (e: Exception) {
                emit(DeviceFlowState.Error("Poll error: ${e.message}"))
                return@flow
            }

            when (tokenResponse.error) {
                null -> {
                    // Success — save token
                    val token = tokenResponse.access_token ?: run {
                        emit(DeviceFlowState.Error("Empty access token"))
                        return@flow
                    }
                    // Fetch username from GitHub API using the new token
                    val username = fetchUsername(token) ?: "github_user"
                    authRepository.savePat(token, username)
                    emit(DeviceFlowState.Authorized(token, username))
                    return@flow
                }
                "authorization_pending" -> {
                    // Still waiting — continue polling
                    emit(DeviceFlowState.Polling(attempt = polls, max = MAX_POLLS))
                }
                "slow_down" -> {
                    // Back off — GitHub asked us to slow down
                    delay(intervalMs * 2)
                    emit(DeviceFlowState.Polling(attempt = polls, max = MAX_POLLS))
                }
                "expired_token" -> {
                    emit(DeviceFlowState.Error("Device code expired. Please try again."))
                    return@flow
                }
                "access_denied" -> {
                    emit(DeviceFlowState.Error("Access denied by user."))
                    return@flow
                }
                else -> {
                    emit(DeviceFlowState.Error("OAuth error: ${tokenResponse.error_description}"))
                    return@flow
                }
            }
        }
        emit(DeviceFlowState.Error("Timed out waiting for authorization."))
    }

    private fun requireClientId(): String? {
        val clientId = CLIENT_ID
        if (!isConfiguredClientId(clientId)) {
            return null
        }
        return clientId
    }

    private suspend fun fetchUsername(token: String): String? {
        return try {
            val userApi = Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .client(
                    okhttp3.OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            chain.proceed(
                                chain.request().newBuilder()
                                    .header("Authorization", "Bearer $token")
                                    .header("Accept", "application/vnd.github.v3+json")
                                    .build()
                            )
                        }.build()
                )
                .build()
                .create(GitHubUserApi::class.java)
            userApi.getAuthenticatedUser().login
        } catch (e: Exception) {
            null
        }
    }
}


private val INVALID_CLIENT_ID_PLACEHOLDERS = setOf(
    "your-client-id",
    "your_github_client_id",
    "placeholder",
    "changeme",
    "replace-me",
    "local-dev-client-id",
    "local-production-client-id"
)

internal fun isConfiguredClientId(value: String): Boolean {
    val normalized = value.trim()
    if (normalized.isEmpty()) {
        return false
    }

    val lower = normalized.lowercase()
    if (lower.startsWith("local-")) {
        return false
    }

    return lower !in INVALID_CLIENT_ID_PLACEHOLDERS
}

/** Retrofit interface for GitHub OAuth endpoints (no auth required) */
interface GitHubOAuthApi {
    @FormUrlEncoded
    @POST("login/device/code")
    @Headers("Accept: application/json")
    suspend fun requestDeviceCode(
        @Field("client_id") clientId: String,
        @Field("scope") scope: String
    ): DeviceCodeResponse

    @FormUrlEncoded
    @POST("login/oauth/access_token")
    @Headers("Accept: application/json")
    suspend fun pollForToken(
        @Field("client_id") clientId: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String
    ): TokenPollResponse
}

/** Retrofit interface to fetch username after getting token */
interface GitHubUserApi {
    @GET("user")
    suspend fun getAuthenticatedUser(): UserLoginResponse
}

data class DeviceCodeResponse(
    val device_code: String,
    val user_code: String,
    val verification_uri: String,
    val expires_in: Int,
    val interval: Int
)

data class TokenPollResponse(
    val access_token: String? = null,
    val token_type: String? = null,
    val scope: String? = null,
    val error: String? = null,
    val error_description: String? = null
)

data class UserLoginResponse(val login: String)

/** States emitted by the OAuth device flow */
sealed class DeviceFlowState {
    object Requesting : DeviceFlowState()
    data class PendingUserAction(
        val userCode: String,
        val verificationUri: String,
        val expiresInSeconds: Int
    ) : DeviceFlowState()
    data class Polling(val attempt: Int, val max: Int) : DeviceFlowState()
    data class Authorized(val token: String, val username: String) : DeviceFlowState()
    data class Error(val message: String) : DeviceFlowState()
}
