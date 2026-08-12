package com.rafgittools.data.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GhCliAuthImporter @Inject constructor() {
    suspend fun isGhAvailable(): Boolean = withContext(Dispatchers.IO) {
        withTimeoutOrNull(PROCESS_TIMEOUT_MS) {
            runCatching {
                val process = ProcessBuilder("gh", "--version")
                    .redirectErrorStream(true)
                    .start()
                process.inputStream.bufferedReader().use { it.readText() }
                process.waitFor() == 0
            }.getOrDefault(false)
        } ?: false
    }

    suspend fun getGhAuthStatus(): Result<String> = withContext(Dispatchers.IO) {
        if (!isGhAvailable()) {
            return@withContext Result.failure(Exception("gh CLI não encontrado. Use PAT ou Device Code."))
        }

        withTimeoutOrNull(PROCESS_TIMEOUT_MS) {
            runCatching {
                val process = ProcessBuilder(
                    "gh", "auth", "status", "--active", "--hostname", GITHUB_HOST
                ).redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().use { it.readText().take(MAX_STATUS_CHARS) }
                val exitCode = process.waitFor()
                check(exitCode == 0) { "gh CLI não confirmou uma sessão ativa para github.com." }
                output
            }
        } ?: Result.failure(Exception("Timeout ao consultar status do gh CLI."))
    }

    suspend fun importToken(): Result<String> = withContext(Dispatchers.IO) {
        if (!isGhAvailable()) {
            return@withContext Result.failure(Exception("gh CLI não encontrado. Use PAT ou Device Code."))
        }

        withTimeoutOrNull(PROCESS_TIMEOUT_MS) {
            runCatching {
                // stderr is intentionally not merged into stdout: an error message must never
                // be interpreted as a credential.
                val process = ProcessBuilder(
                    "gh", "auth", "token", "--hostname", GITHUB_HOST
                ).start()
                val token = process.inputStream.bufferedReader().use { it.readText().trim() }
                process.errorStream.bufferedReader().use { it.readText() } // drain, never return/log
                val exitCode = process.waitFor()
                check(exitCode == 0) { "gh CLI não forneceu o token da sessão ativa." }
                check(token.isNotBlank()) { "gh auth token retornou vazio." }
                check(token.length in MIN_TOKEN_LENGTH..MAX_TOKEN_LENGTH && token.none(Char::isWhitespace)) {
                    "gh CLI retornou uma credencial fora do contrato esperado."
                }
                token
            }
        } ?: Result.failure(Exception("Timeout ao importar token do gh CLI."))
    }

    companion object {
        private const val GITHUB_HOST = "github.com"
        private const val PROCESS_TIMEOUT_MS = 3_000L
        private const val MAX_STATUS_CHARS = 1_000
        private const val MIN_TOKEN_LENGTH = 20
        private const val MAX_TOKEN_LENGTH = 200
    }
}
