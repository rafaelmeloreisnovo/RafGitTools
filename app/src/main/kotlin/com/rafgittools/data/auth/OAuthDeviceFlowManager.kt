package com.rafgittools.data.auth

import android.content.Context
import com.rafgittools.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Retrofit
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

private val CLIENT_ID_PLACEHOLDERS = setOf(
    "local-dev-client-id",
    "local-production-client-id",
    "your-client-id",
    "your_github_client_id",
    "placeholder",
    "changeme",
    "replace-me"
)

internal fun isConfiguredClientId(value: String): Boolean {
    val normalized = value.trim()
    if (normalized.isBlank()) return false
    if (normalized.startsWith("local-", ignoreCase = true)) return false
    return normalized.lowercase() !in CLIENT_ID_PLACEHOLDERS
}

/**
 * OAuth Device Flow Manager — P33-23
 *
 * Implements GitHub OAuth Device Authorization Grant (RFC 8628).
 * Allows users to authenticate without typing their GitHub password in the app:
 * 1. App requests device_code from GitHub
 * 2. User visits github.com/login/device and enters user_code
 * 3. App polls until authorized or expired
 *
 * Docs: https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#device-flow
 */
@Singleton
class OAuthDeviceFlowManager @Inject constructor(
    @Suppress("UNUSED_PARAMETER") @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val GITHUB_OAUTH_URL = "https://github.com/"
        private val CLIENT_ID get() = BuildConfig.GITHUB_CLIENT_ID
        private const val CLIENT_ID_ERROR_MESSAGE =
            "GitHub OAuth não está configurado nesta compilação. Defina GITHUB_CLIENT_ID_DEV ou " +
                "GITHUB_CLIENT_ID_PRODUCTION e gere o APK novamente. Você ainda pode usar um token de acesso."
        private const val SCOPE = "repo read:user notifications"
        private const val DEFAULT_POLL_INTERVAL_MS = 5_000L
        private const val SLOW_DOWN_INCREMENT_MS = 5_000L
        private const val MAX_POLLS = 60
    }

    private val oauthApi: GitHubOAuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(GITHUB_OAUTH_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(GitHubOAuthApi::class.java)
    }

    /**
     * Validates an access token directly against GitHub's `/user` endpoint.
     * No cached user is accepted as proof of a valid token.
     */
    suspend fun validateToken(token: String): Result<String> {
        val normalized = token.trim()
        if (normalized.isBlank()) {
            return Result.failure(IllegalArgumentException("O token do GitHub está vazio."))
        }

        val username = fetchUsername(normalized)
            ?: return Result.failure(
                IllegalArgumentException(
                    "O GitHub recusou o token. Verifique se ele está ativo e possui permissão de leitura do usuário."
                )
            )

        return Result.success(username)
    }

    /**
     * Emits [DeviceFlowState] events through the full OAuth device flow.
     * Collect this Flow in your ViewModel; cancel to abort polling.
     */
    fun startDeviceFlow(): Flow<DeviceFlowState> = flow {
        val clientId = try {
            requireClientId()
        } catch (_: IllegalStateException) {
            emit(DeviceFlowState.Error(CLIENT_ID_ERROR_MESSAGE))
            return@flow
        }
        emit(DeviceFlowState.Requesting)

        val codeResponse = try {
            oauthApi.requestDeviceCode(clientId = clientId, scope = SCOPE)
        } catch (e: Exception) {
            emit(DeviceFlowState.Error("Não foi possível iniciar o login no GitHub: ${e.message}"))
            return@flow
        }

        emit(
            DeviceFlowState.PendingUserAction(
                userCode = codeResponse.user_code,
                verificationUri = codeResponse.verification_uri,
                expiresInSeconds = codeResponse.expires_in
            )
        )

        var intervalMs = (codeResponse.interval * 1000L).coerceAtLeast(DEFAULT_POLL_INTERVAL_MS)
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
                emit(DeviceFlowState.Error("Falha ao consultar autorização: ${e.message}"))
                return@flow
            }

            when (tokenResponse.error) {
                null -> {
                    val token = tokenResponse.access_token ?: run {
                        emit(DeviceFlowState.Error("O GitHub retornou um token vazio."))
                        return@flow
                    }
                    val username = validateToken(token).getOrElse { error ->
                        emit(DeviceFlowState.Error(error.message ?: "Não foi possível validar a identidade no GitHub."))
                        return@flow
                    }
                    authRepository.savePat(token, username).onFailure { error ->
                        emit(DeviceFlowState.Error("Falha ao proteger a credencial no aparelho: ${error.message}"))
                        return@flow
                    }
                    emit(DeviceFlowState.Authorized(token, username))
                    return@flow
                }

                "authorization_pending" -> {
                    emit(DeviceFlowState.Polling(attempt = polls, max = MAX_POLLS))
                }

                "slow_down" -> {
                    intervalMs += SLOW_DOWN_INCREMENT_MS
                    emit(DeviceFlowState.Polling(attempt = polls, max = MAX_POLLS))
                }

                "expired_token", "token_expired" -> {
                    emit(DeviceFlowState.Error("O código expirou. Inicie o login novamente."))
                    return@flow
                }

                "access_denied" -> {
                    emit(DeviceFlowState.Error("A autorização foi cancelada no GitHub."))
                    return@flow
                }

                "device_flow_disabled" -> {
                    emit(DeviceFlowState.Error("O Device Flow não está habilitado no OAuth App configurado."))
                    return@flow
                }

                else -> {
                    emit(
                        DeviceFlowState.Error(
                            tokenResponse.error_description ?: "Erro OAuth: ${tokenResponse.error}"
                        )
                    )
                    return@flow
                }
            }
        }
        emit(DeviceFlowState.Error("Tempo esgotado aguardando a autorização."))
    }

    private fun requireClientId(): String {
        val clientId = CLIENT_ID
        if (!isConfiguredClientId(clientId)) {
            throw IllegalStateException(CLIENT_ID_ERROR_MESSAGE)
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
                                    .header("Accept", "application/vnd.github+json")
                                    .header("X-GitHub-Api-Version", "2022-11-28")
                                    .build()
                            )
                        }
                        .build()
                )
                .build()
                .create(GitHubUserApi::class.java)
            userApi.getAuthenticatedUser().login
        } catch (_: Exception) {
            null
        }
    }
}

/** Retrofit interface for GitHub OAuth endpoints (no auth required). */
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

/** Retrofit interface to fetch the username after receiving a token. */
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
