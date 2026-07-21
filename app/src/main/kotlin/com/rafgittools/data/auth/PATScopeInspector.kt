package com.rafgittools.data.auth

import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Inspects GitHub PAT scopes from the X-OAuth-Scopes response header.
 *
 * Closes RG8: fine-grained PATs don't emit X-OAuth-Scopes; classic PATs do.
 * This class distinguishes between the two and reports which scopes are granted.
 *
 * GitHub docs:
 *   Classic PAT  → GET /user returns X-OAuth-Scopes: repo,user,...
 *   Fine-grained → X-OAuth-Scopes is absent; must probe endpoints individually.
 */
@Singleton
class PATScopeInspector @Inject constructor() {

    data class ScopeReport(
        val tokenType: TokenType,
        val scopes: Set<String>,
        val hasRepo: Boolean,
        val hasReadUser: Boolean,
        val hasNotifications: Boolean,
        val isUnknownScopeModel: Boolean
    )

    enum class TokenType { CLASSIC_PAT, FINE_GRAINED_PAT, OAUTH_TOKEN, UNKNOWN }

    private val client = OkHttpClient.Builder().build()

    suspend fun inspectScopes(token: String): Result<ScopeReport> = runCatching {
        val request = Request.Builder()
            .url("https://api.github.com/user")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()

        val response = client.newCall(request).execute()
        response.use { resp ->
            if (!resp.isSuccessful) {
                error("GitHub rejected token: HTTP ${resp.code}")
            }

            val scopeHeader = resp.header("X-OAuth-Scopes")
            val tokenTypeHeader = resp.header("X-GitHub-Token-Type") ?: ""

            when {
                scopeHeader != null -> {
                    // Classic PAT or OAuth token — scopes are explicit
                    val scopes = scopeHeader.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                    val type = if (tokenTypeHeader.contains("fine", ignoreCase = true))
                        TokenType.FINE_GRAINED_PAT else TokenType.CLASSIC_PAT
                    ScopeReport(
                        tokenType = type,
                        scopes = scopes,
                        hasRepo = scopes.any { it == "repo" || it.startsWith("repo:") },
                        hasReadUser = scopes.any { it == "user" || it == "read:user" || it.startsWith("user:") },
                        hasNotifications = "notifications" in scopes,
                        isUnknownScopeModel = false
                    )
                }
                else -> {
                    // Fine-grained PAT — no X-OAuth-Scopes header; probe endpoints
                    val hasRepo = probeEndpoint(token, "https://api.github.com/user/repos?per_page=1")
                    val hasNotifications = probeEndpoint(token, "https://api.github.com/notifications?per_page=1")
                    ScopeReport(
                        tokenType = TokenType.FINE_GRAINED_PAT,
                        scopes = buildSet {
                            if (hasRepo) add("contents:read")
                            if (hasNotifications) add("notifications")
                        },
                        hasRepo = hasRepo,
                        hasReadUser = true, // /user succeeded above
                        hasNotifications = hasNotifications,
                        isUnknownScopeModel = true
                    )
                }
            }
        }
    }

    private fun probeEndpoint(token: String, url: String): Boolean {
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
        return runCatching {
            client.newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    fun formatScopeWarnings(report: ScopeReport): List<String> {
        val warnings = mutableListOf<String>()
        if (report.tokenType == TokenType.FINE_GRAINED_PAT) {
            warnings.add("Fine-grained PAT detectado — scopes sao inferidos por sondagem, nao declarados explicitamente.")
        }
        if (!report.hasRepo) {
            warnings.add("Token sem acesso a repositorios (scope 'repo' ausente). Operacoes de clone e push falharao.")
        }
        if (!report.hasReadUser) {
            warnings.add("Token sem leitura de perfil de usuario. Exibicao de avatar/nome pode falhar.")
        }
        return warnings
    }
}
