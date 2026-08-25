package com.rafgittools.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rafgittools.core.security.InputValidationType
import com.rafgittools.core.security.SecurityException
import com.rafgittools.core.security.SecurityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

/**
 * Repository for authentication credentials.
 *
 * Access tokens and optional GitHub App refresh tokens are encrypted with
 * Android Keystore-backed AES-GCM before DataStore persistence.
 */
@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.authDataStore
    private val securityManager = SecurityManager(context)

    companion object {
        private val ENCRYPTED_PAT_KEY = stringPreferencesKey("encrypted_pat")
        private val ENCRYPTED_REFRESH_TOKEN_KEY = stringPreferencesKey("encrypted_refresh_token")
        private val ACCESS_TOKEN_EXPIRES_AT_KEY = longPreferencesKey("access_token_expires_at_ms")
        private val REFRESH_TOKEN_EXPIRES_AT_KEY = longPreferencesKey("refresh_token_expires_at_ms")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val IS_AUTHENTICATED_KEY = stringPreferencesKey("is_authenticated")
        private val AUTH_METHOD_KEY = stringPreferencesKey("auth_method")
        private val OFFLINE_MODE_KEY = stringPreferencesKey("offline_mode")
        private const val PAT_KEY_ALIAS = "github_pat"
        private const val REFRESH_TOKEN_KEY_ALIAS = "github_refresh_token"
    }

    val isAuthenticatedFlow: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[IS_AUTHENTICATED_KEY] == "true" }

    val usernameFlow: Flow<String?> = dataStore.data
        .map { preferences -> preferences[USERNAME_KEY] }

    suspend fun isAuthenticated(): Boolean = isAuthenticatedFlow.first()

    suspend fun getUsername(): String? = usernameFlow.first()

    /**
     * Save a PAT/OAuth access token that has no refresh-token contract.
     * Any previously stored refresh token is removed so a credential switch can
     * never accidentally reuse refresh state from an older GitHub App session.
     */
    suspend fun savePat(token: String, username: String): Result<Unit> {
        return try {
            validateCredentialInputs(token, username)
            val encryptedToken = securityManager.encryptData(token, PAT_KEY_ALIAS).getOrThrow()

            dataStore.edit { preferences ->
                preferences[ENCRYPTED_PAT_KEY] = encryptedToken
                preferences[USERNAME_KEY] = username
                preferences[IS_AUTHENTICATED_KEY] = "true"
                preferences[AUTH_METHOD_KEY] = AuthMethod.PAT.name
                preferences.remove(ENCRYPTED_REFRESH_TOKEN_KEY)
                preferences.remove(ACCESS_TOKEN_EXPIRES_AT_KEY)
                preferences.remove(REFRESH_TOKEN_EXPIRES_AT_KEY)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save an OAuth/GitHub App user session. Refresh metadata is optional because
     * OAuth Apps and GitHub Apps with token expiration disabled may return only an
     * access token. A null refresh token clears any previous refresh state.
     */
    suspend fun saveOAuthSession(
        accessToken: String,
        username: String,
        refreshToken: String?,
        accessExpiresInSeconds: Long?,
        refreshExpiresInSeconds: Long?
    ): Result<Unit> {
        return try {
            validateCredentialInputs(accessToken, username)
            val normalizedRefresh = refreshToken?.trim()?.takeIf { it.isNotEmpty() }
            if (normalizedRefresh != null &&
                !securityManager.validateInput(normalizedRefresh, InputValidationType.ACCESS_TOKEN)
            ) {
                return Result.failure(SecurityException("Invalid refresh token format"))
            }

            val encryptedAccess = securityManager.encryptData(accessToken, PAT_KEY_ALIAS).getOrThrow()
            val encryptedRefresh = normalizedRefresh?.let {
                securityManager.encryptData(it, REFRESH_TOKEN_KEY_ALIAS).getOrThrow()
            }
            val now = System.currentTimeMillis()
            val accessExpiry = absoluteExpiry(now, accessExpiresInSeconds)
            val refreshExpiry = absoluteExpiry(now, refreshExpiresInSeconds)

            dataStore.edit { preferences ->
                preferences[ENCRYPTED_PAT_KEY] = encryptedAccess
                preferences[USERNAME_KEY] = username
                preferences[IS_AUTHENTICATED_KEY] = "true"

                if (encryptedRefresh != null) {
                    preferences[ENCRYPTED_REFRESH_TOKEN_KEY] = encryptedRefresh
                } else {
                    preferences.remove(ENCRYPTED_REFRESH_TOKEN_KEY)
                }

                if (accessExpiry != null) {
                    preferences[ACCESS_TOKEN_EXPIRES_AT_KEY] = accessExpiry
                } else {
                    preferences.remove(ACCESS_TOKEN_EXPIRES_AT_KEY)
                }

                if (refreshExpiry != null && encryptedRefresh != null) {
                    preferences[REFRESH_TOKEN_EXPIRES_AT_KEY] = refreshExpiry
                } else {
                    preferences.remove(REFRESH_TOKEN_EXPIRES_AT_KEY)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPat(): Result<String> {
        return try {
            val preferences = dataStore.data.first()
            val encryptedToken = preferences[ENCRYPTED_PAT_KEY]
                ?: return Result.failure(Exception("No token stored"))
            Result.success(
                securityManager.decryptData(encryptedToken, PAT_KEY_ALIAS).getOrThrow()
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRefreshToken(): Result<String> {
        return try {
            val preferences = dataStore.data.first()
            val encryptedToken = preferences[ENCRYPTED_REFRESH_TOKEN_KEY]
                ?: return Result.failure(Exception("No refresh token stored"))
            Result.success(
                securityManager.decryptData(encryptedToken, REFRESH_TOKEN_KEY_ALIAS).getOrThrow()
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAccessTokenExpiresAt(): Long? =
        dataStore.data.first()[ACCESS_TOKEN_EXPIRES_AT_KEY]

    suspend fun getRefreshTokenExpiresAt(): Long? =
        dataStore.data.first()[REFRESH_TOKEN_EXPIRES_AT_KEY]

    suspend fun saveAuthMethod(method: AuthMethod): Result<Unit> {
        return try {
            dataStore.edit { preferences -> preferences[AUTH_METHOD_KEY] = method.name }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAuthMethod(): Result<AuthMethod> {
        return try {
            val preferences = dataStore.data.first()
            val methodValue = preferences[AUTH_METHOD_KEY]
                ?: return Result.failure(Exception("No auth method stored"))
            Result.success(AuthMethod.valueOf(methodValue))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAuthMethod(): Result<Unit> {
        return try {
            dataStore.edit { preferences -> preferences.remove(AUTH_METHOD_KEY) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setOfflineMode(enabled: Boolean): Result<Unit> {
        return try {
            dataStore.edit { preferences -> preferences[OFFLINE_MODE_KEY] = enabled.toString() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isOfflineMode(): Boolean =
        dataStore.data.first()[OFFLINE_MODE_KEY] == "true"

    suspend fun clearAuthState(): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences.remove(ENCRYPTED_PAT_KEY)
                preferences.remove(ENCRYPTED_REFRESH_TOKEN_KEY)
                preferences.remove(ACCESS_TOKEN_EXPIRES_AT_KEY)
                preferences.remove(REFRESH_TOKEN_EXPIRES_AT_KEY)
                preferences.remove(USERNAME_KEY)
                preferences[IS_AUTHENTICATED_KEY] = "false"
                preferences.remove(AUTH_METHOD_KEY)
                preferences.remove(OFFLINE_MODE_KEY)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> = clearAuthState()

    private fun validateCredentialInputs(token: String, username: String) {
        if (!securityManager.validateInput(username, InputValidationType.USERNAME)) {
            throw SecurityException("Invalid username format")
        }
        if (!securityManager.validateInput(token, InputValidationType.ACCESS_TOKEN)) {
            throw SecurityException("Invalid access token format")
        }
    }

    private fun absoluteExpiry(nowMs: Long, seconds: Long?): Long? {
        val positiveSeconds = seconds?.takeIf { it > 0L } ?: return null
        return Math.addExact(nowMs, Math.multiplyExact(positiveSeconds, 1_000L))
    }
}
