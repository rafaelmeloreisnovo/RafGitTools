package com.rafgittools.data.preferences

import com.google.common.truth.Truth.assertThat
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Test

class PreferencesRepositorySyncLanguageRegressionTest {

    @Test
    fun getLanguageSync_doesNotUseCoroutineBlockingApis() {
        val sourcePath = resolveRepositorySourcePath()
        val source = Files.readString(sourcePath)

        val methodStart = source.indexOf("fun getLanguageSync(): Language")
        assertThat(methodStart).isAtLeast(0)

        val hydrateStart = source.indexOf("suspend fun hydrateLanguageSyncCache()")
        assertThat(hydrateStart).isGreaterThan(methodStart)

        val methodBody = source.substring(methodStart, hydrateStart)

        assertThat(methodBody).doesNotContain("runBlocking")
        assertThat(methodBody).doesNotContain("first()")
        assertThat(methodBody).doesNotContain("withContext")
    }

    private fun resolveRepositorySourcePath(): Path {
        val candidates = listOf(
            Paths.get("app/src/main/kotlin/com/rafgittools/data/preferences/PreferencesRepository.kt"),
            Paths.get("src/main/kotlin/com/rafgittools/data/preferences/PreferencesRepository.kt")
        )

        return candidates.firstOrNull { Files.exists(it) }
            ?: error("Could not locate PreferencesRepository.kt in expected paths: $candidates")
    }
}
