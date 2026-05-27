package com.rafgittools.data.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GhCliAuthImporter @Inject constructor() {
    suspend fun isGhAvailable(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val process = ProcessBuilder("which", "gh").start()
            process.waitFor() == 0
        }.getOrDefault(false)
    }

    suspend fun getGhAuthStatus(): Result<String> = withContext(Dispatchers.IO) {
        if (!isGhAvailable()) return@withContext Result.failure(Exception("gh CLI não encontrado. Use PAT ou Device Code."))
        val output = withTimeoutOrNull(3000) {
            val process = ProcessBuilder("gh", "auth", "status").redirectErrorStream(true).start()
            process.inputStream.bufferedReader().readText().take(1000)
        } ?: return@withContext Result.failure(Exception("Timeout ao consultar status do gh CLI."))
        Result.success(output)
    }

    suspend fun importToken(): Result<String> = withContext(Dispatchers.IO) {
        if (!isGhAvailable()) return@withContext Result.failure(Exception("gh CLI não encontrado. Use PAT ou Device Code."))
        val token = withTimeoutOrNull(3000) {
            val process = ProcessBuilder("gh", "auth", "token").redirectErrorStream(true).start()
            process.inputStream.bufferedReader().readText().trim()
        } ?: return@withContext Result.failure(Exception("Timeout ao importar token do gh CLI."))

        if (token.isBlank()) Result.failure(Exception("gh auth token retornou vazio.")) else Result.success(token)
    }
}
