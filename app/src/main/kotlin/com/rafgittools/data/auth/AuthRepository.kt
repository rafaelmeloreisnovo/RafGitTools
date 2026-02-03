package com.rafgittools.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rafgittools.core.security.InputValidationType
import com.rafgittools.core.security.SecurityManager
import com.rafgittools.core.security.SecurityException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

/**
 * Repository for managing authentication credentials
 * 
 * Provides secure storage and retrieval of Personal Access Tokens (PAT)
 * using encrypted preferences with Android Keystore.
 */
@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.authDataStore
    private val securityManager = SecurityManager(context)
    
    companion object {
        private val ENCRYPTED_PAT_KEY = stringPreferencesKey("encrypted_pat")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val IS_AUTHENTICATED_KEY = stringPreferencesKey("is_authenticated")
        private const val PAT_KEY_ALIAS = "github_pat"
    }
    
    /**
     * Flow that emits the current authentication state
     */
    val isAuthenticatedFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[IS_AUTHENTICATED_KEY] == "true"
        }
    
    /**
     * Flow that emits the current username
     */
    val usernameFlow: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[USERNAME_KEY]
        }
    
    /**
     * Check if user is currently authenticated
     */
    suspend fun isAuthenticated(): Boolean {
        return isAuthenticatedFlow.first()
    }
    
    /**
     * Get the currently stored username
     */
    suspend fun getUsername(): String? {
        return usernameFlow.first()
    }
    
    /**
     * Save Personal Access Token securely
     * 
     * The token is encrypted using AES-256-GCM before storage
     * 
     * @param token The GitHub Personal Access Token
     * @param username The GitHub username
     * @return Result indicating success or failure
     */
    suspend fun savePat(token: String, username: String): Result<Unit> {
        return try {
            if (!securityManager.validateInput(username, InputValidationType.USERNAME)) {
                return Result.failure(SecurityException("Invalid username format"))
            }
            if (!securityManager.validateInput(token, InputValidationType.ACCESS_TOKEN)) {
                return Result.failure(SecurityException("Invalid personal access token format"))
            }

            // Encrypt the token before storing
            val encryptedToken = securityManager.encryptData(token, PAT_KEY_ALIAS)
                .getOrThrow()
            
            dataStore.edit { preferences ->
                preferences[ENCRYPTED_PAT_KEY] = encryptedToken
                preferences[USERNAME_KEY] = username
                preferences[IS_AUTHENTICATED_KEY] = "true"
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Retrieve the stored Personal Access Token
     * 
     * The token is decrypted from secure storage
     * 
     * @return Result containing the decrypted token or an error
     */
    suspend fun getPat(): Result<String> {
        return try {
            val preferences = dataStore.data.first()
            val encryptedToken = preferences[ENCRYPTED_PAT_KEY]
                ?: return Result.failure(Exception("No token stored"))
            
            val decryptedToken = securityManager.decryptData(encryptedToken, PAT_KEY_ALIAS)
                .getOrThrow()
            
            Result.success(decryptedToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Clear all authentication data
     * 
     * Removes the stored token and username, setting authenticated to false
     */
    suspend fun logout(): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences.remove(ENCRYPTED_PAT_KEY)
                preferences.remove(USERNAME_KEY)
                preferences[IS_AUTHENTICATED_KEY] = "false"
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
