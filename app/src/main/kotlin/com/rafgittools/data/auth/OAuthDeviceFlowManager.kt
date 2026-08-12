package com.rafgittools.data.auth

import com.rafgittools.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
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

@Singleton
class GitHubOAuthConfig @Inject constructor() {
    fun requireClientId(): String {
        val clientId = BuildConfig.GITHUB_CLIENT_ID.trim()
        if (!isConfiguredClientId(clientId)) {
            throw IllegalStateException(CLIENT_ID_ERROR_MESSAGE)
        }
        return clientId
    }

    companion object {
        const val CLIENT_ID_ERROR_MESSAGE =
            "GitHub OAuth não está configurado nesta compilação. Defina GITHUB_CLIENT_ID_DEV ou " +
                "GITHUB_CLIENT_ID_PRODUCTION e gere o APK novamente. Você ainda pode usar um token de acesso."
    }
}

/** OAuth transport intentionally independent from the authenticated API client. */
@Singleton
class GitHubOAuthApiClient @Inject constructor() {
    val api: GitHubOAuthApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl("https://github.com/")
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(GitHubOAuthApi::class.java)
    }
}

/**
 * OAuth Device Flow Manager — P33-23/P33-25.
 *
 * The same GitHub device endpoints can authorize OAuth Apps and GitHub Apps.
 * OAuth Apps typically return an access token only. GitHub Apps configured for
 * expiring user access tokens can additionally return access-token expiry and a
 * rotating refresh token. Those optional fields are preserved when present.
 */
@Singleton
class OAuthDeviceFlowManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val oauthApiClient: GitHubOAuthApiClient,
    private val oauthConfig: GitHubOAuthConfig
) {
    companion object {
        private const val SCOPE = "repo read:user read:org notifications"
        private const val DEFAULT_POLL_INTERVAL_MS = 5_000L
        private const val SLOW_DOWN_INCREMENT_MS = 5_000L
        private const val MAX_POLLS = 60
    }

    /** Refresh tokens rotate, therefore all refresh consumption is serialized. */
    private val refreshMutex = Mutex()

    private val sharedUserHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** Validate an access token directly against GitHub `/user`. */
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

    /** Emits states for the full OAuth/GitHub App device flow. */
    fun startDeviceFlow(): Flow<DeviceFlowState> = flow {
        val clientId = try {
            oauthConfig.requireClientId()
        } catch (error: IllegalStateException) {
            emit(DeviceFlowState.Error(error.message ?: GitHubOAuthConfig.CLIENT_ID_ERROR_MESSAGE))
            return@flow
        }
        emit(DeviceFlowState.Requesting)

        val codeResponse = try {
            oauthApiClient.api.requestDeviceCode(clientId = clientId, scope = SCOPE)
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
                oauthApiClient.api.pollForToken(
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
                    authRepository.saveOAuthSession(
                        accessToken = token,
                        username = username,
                        refreshToken = tokenResponse.refresh_token,
                        accessExpiresInSeconds = tokenResponse.expires_in,
                        refreshExpiresInSeconds = tokenResponse.refresh_token_expires_in
                    ).onFailure { error ->
                        emit(DeviceFlowState.Error("Falha ao proteger a credencial no aparelho: ${error.message}"))
                        return@flow
                    }
                    emit(DeviceFlowState.Authorized(token, username))
                    return@flow
                }

                "authorization_pending" ->
                    emit(DeviceFlowState.Polling(attempt = polls, max = MAX_POLLS))

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
                    emit(DeviceFlowState.Error("O Device Flow não está habilitado no app GitHub configurado."))
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

    /**
     * Rotate a stored GitHub App user token when Device Flow supplied a refresh
     * token. [rejectedAccessToken] identifies the credential that triggered 401.
     *
     * Concurrent 401s are serialized. After acquiring the lock, if another call
     * already replaced the rejected access token, this call reuses the newly
     * persisted token instead of consuming the rotating refresh token again.
     */
    suspend fun refreshStoredSession(
        rejectedAccessToken: String? = null
    ): Result<RefreshedSession> = refreshMutex.withLock {
        runCatching {
            if (!rejectedAccessToken.isNullOrBlank()) {
                val currentAccess = authRepository.getPat().getOrNull()
                if (!currentAccess.isNullOrBlank() && currentAccess != rejectedAccessToken) {
                    val username = authRepository.getUsername()
                        ?: throw IllegalStateException(
                            "Authenticated username is missing after concurrent token rotation"
                        )
                    return@runCatching RefreshedSession(
                        accessToken = currentAccess,
                        username = username
                    )
                }
            }

            val refreshToken = authRepository.getRefreshToken().getOrThrow()
            val refreshExpiresAt = authRepository.getRefreshTokenExpiresAt()
            if (refreshExpiresAt != null && System.currentTimeMillis() >= refreshExpiresAt) {
                throw IllegalStateException("Stored refresh token has expired")
            }

            val clientId = oauthConfig.requireClientId()
            val response = oauthApiClient.api.refreshToken(
                clientId = clientId,
                grantType = "refresh_token",
                refreshToken = refreshToken
            )

            if (response.error != null) {
                throw IllegalStateException(
                    response.error_description ?: "GitHub refresh failed: ${response.error}"
                )
            }

            val accessToken = response.access_token
                ?: throw IllegalStateException("GitHub refresh response did not include an access token")
            val username = authRepository.getUsername()
                ?: throw IllegalStateException("Authenticated username is missing during token rotation")

            authRepository.saveOAuthSession(
                accessToken = accessToken,
                username = username,
                refreshToken = response.refresh_token,
                accessExpiresInSeconds = response.expires_in,
                refreshExpiresInSeconds = response.refresh_token_expires_in
            ).getOrThrow()

            RefreshedSession(accessToken = accessToken, username = username)
        }
    }

    private suspend fun fetchUsername(token: String): String? {
        return try {
            val authedClient = sharedUserHttpClient.newBuilder()
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
            val userApi = Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .client(authedClient)
                .build()
                .create(GitHubUserApi::class.java)
            userApi.getAuthenticatedUser().login
        } catch (_: Exception) {
            null
        }
    }
}

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

    @FormUrlEncoded
    @POST("login/oauth/access_token")
    @Headers("Accept: application/json")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("grant_type") grantType: String,
        @Field("refresh_token") refreshToken: String
    ): TokenPollResponse
}

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
    val expires_in: Long? = null,
    val refresh_token: String? = null,
    val refresh_token_expires_in: Long? = null,
    val token_type: String? = null,
    val scope: String? = null,
    val error: String? = null,
    val error_description: String? = null
)

data class UserLoginResponse(val login: String)

data class RefreshedSession(
    val accessToken: String,
    val username: String
)

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
