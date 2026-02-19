package com.rafgittools.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.accountsDataStore: DataStore<Preferences> by preferencesDataStore(name = "accounts")

/**
 * Multi-Account Manager — P33-30/31/32
 *
 * Manages multiple GitHub accounts. Each account stores:
 * - login (username)
 * - encrypted PAT (via EncryptionManager)
 * - avatarUrl (optional, for display)
 * - isActive flag
 *
 * The active account drives all API calls via AuthInterceptor.
 */
@Singleton
class MultiAccountManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val encryptionManager: EncryptionManager
) {
    companion object {
        private val ACCOUNTS_KEY = stringPreferencesKey("accounts_json")
        private val ACTIVE_ACCOUNT_KEY = stringPreferencesKey("active_account_login")
        private const val MAX_ACCOUNTS = 5
    }

    private val dataStore = context.accountsDataStore
    private val gson = Gson()

    /** Flow of all stored accounts */
    val accountsFlow: Flow<List<GitHubAccount>> = dataStore.data.map { prefs ->
        val json = prefs[ACCOUNTS_KEY] ?: return@map emptyList()
        deserializeAccounts(json)
    }

    /** Flow of the currently active account */
    val activeAccountFlow: Flow<GitHubAccount?> = dataStore.data.map { prefs ->
        val activeLogin = prefs[ACTIVE_ACCOUNT_KEY] ?: return@map null
        val json = prefs[ACCOUNTS_KEY] ?: return@map null
        deserializeAccounts(json).firstOrNull { it.login == activeLogin }
    }

    /** Add or update an account. Activates it immediately. */
    suspend fun addAccount(login: String, token: String, avatarUrl: String? = null): Result<Unit> {
        return runCatching {
            val current = getAccounts().toMutableList()
            if (current.size >= MAX_ACCOUNTS && current.none { it.login == login }) {
                throw IllegalStateException("Maximum of $MAX_ACCOUNTS accounts reached")
            }
            // Encrypt token
            val encryptedToken = encryptionManager.encrypt(token, "account_$login")
            if (encryptedToken.isBlank()) {
                throw IllegalStateException("Failed to encrypt token for account $login")
            }

            val account = GitHubAccount(
                login = login,
                encryptedToken = encryptedToken,
                avatarUrl = avatarUrl,
                addedAt = System.currentTimeMillis()
            )
            // Remove existing entry for same login, then add updated
            current.removeAll { it.login == login }
            current.add(account)

            dataStore.edit { prefs ->
                prefs[ACCOUNTS_KEY] = gson.toJson(current)
                prefs[ACTIVE_ACCOUNT_KEY] = login
            }
        }
    }

    /** Switch to an existing account. Fails silently if login not found. */
    suspend fun switchAccount(login: String): Result<Unit> = runCatching {
        val accounts = getAccounts()
        if (accounts.none { it.login == login }) {
            throw IllegalArgumentException("Account '$login' not found")
        }
        dataStore.edit { prefs -> prefs[ACTIVE_ACCOUNT_KEY] = login }
    }

    /** Remove an account. If it was active, switch to first remaining or clear. */
    suspend fun removeAccount(login: String): Result<Unit> = runCatching {
        val current = getAccounts().toMutableList()
        val removed = current.removeAll { it.login == login }
        if (!removed) return@runCatching

        val activeLogin = dataStore.data.first()[ACTIVE_ACCOUNT_KEY]
        val newActive = if (activeLogin == login) current.firstOrNull()?.login else activeLogin

        dataStore.edit { prefs ->
            prefs[ACCOUNTS_KEY] = gson.toJson(current)
            if (newActive != null) prefs[ACTIVE_ACCOUNT_KEY] = newActive
            else prefs.remove(ACTIVE_ACCOUNT_KEY)
        }
    }

    /** Decrypt and return the PAT for the active account */
    suspend fun getActiveToken(): Result<String> = runCatching {
        val active = activeAccountFlow.first()
            ?: throw IllegalStateException("No active account")
        val decrypted = encryptionManager.decrypt(active.encryptedToken, "account_${active.login}")
        if (decrypted.isBlank()) throw IllegalStateException("Failed to decrypt active account token")
        decrypted
    }

    /** Decrypt and return the PAT for a specific account */
    suspend fun getTokenForAccount(login: String): Result<String> = runCatching {
        val account = getAccounts().firstOrNull { it.login == login }
            ?: throw IllegalArgumentException("Account '$login' not found")
        val decrypted = encryptionManager.decrypt(account.encryptedToken, "account_$login")
        if (decrypted.isBlank()) throw IllegalStateException("Failed to decrypt token for account $login")
        decrypted
    }

    suspend fun getActiveAccount(): GitHubAccount? = activeAccountFlow.first()

    suspend fun getAccounts(): List<GitHubAccount> = accountsFlow.first()

    suspend fun hasAccounts(): Boolean = getAccounts().isNotEmpty()

    private fun deserializeAccounts(json: String): List<GitHubAccount> {
        return try {
            val type = object : TypeToken<List<GitHubAccount>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class GitHubAccount(
    val login: String,
    val encryptedToken: String,
    val avatarUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
