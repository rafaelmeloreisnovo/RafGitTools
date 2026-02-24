package com.rafgittools.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.rafgittools.core.localization.Language
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class PreferencesRepositorySyncLanguageTest {

    @Test
    fun `getLanguageSync reads only sync cache`() {
        val context = mockk<Context>()
        val sharedPreferences = mockk<SharedPreferences>()

        every {
            context.getSharedPreferences("settings_sync", Context.MODE_PRIVATE)
        } returns sharedPreferences
        every {
            sharedPreferences.getString("language_sync", Language.ENGLISH.code)
        } returns Language.PORTUGUESE.code

        val repository = PreferencesRepository(context)

        val language = repository.getLanguageSync()

        assertEquals(Language.PORTUGUESE, language)
        verify(exactly = 1) {
            sharedPreferences.getString("language_sync", Language.ENGLISH.code)
        }
    }

    @Test
    fun `getLanguageSync does not use runBlocking`() {
        val sourceFile = File("app/src/main/kotlin/com/rafgittools/data/preferences/PreferencesRepository.kt")
            .takeIf { it.exists() }
            ?: File("src/main/kotlin/com/rafgittools/data/preferences/PreferencesRepository.kt")
        val source = sourceFile.readText()

        val methodBody = source.substringAfter("fun getLanguageSync(): Language {")
            .substringBefore("}\n\n    /**\n     * Hydrate sync cache")

        assertFalse(methodBody.contains("runBlocking"))
    }
}
