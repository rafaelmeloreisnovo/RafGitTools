package com.rafgittools.core.localization

import java.util.Locale

/**
 * Supported languages in RafGitTools
 * 
 * Note: The displayName is in the native language (not localized) by design,
 * as this is a common UX pattern for language selectors to show language names
 * in their native form (e.g., "English", "Português", "Español") regardless of
 * the currently selected UI language. This helps users identify their preferred
 * language even when the UI is in a different language.
 */
enum class Language(val code: String, val locale: Locale, val displayName: String) {
    ENGLISH("en", Locale.ENGLISH, "English"),
    PORTUGUESE("pt", Locale("pt", "BR"), "Português"),
    SPANISH("es", Locale("es"), "Español");
    
    companion object {
        fun fromCode(code: String): Language {
            return values().find { it.code == code } ?: ENGLISH
        }
        
        fun fromLocale(locale: Locale): Language {
            return values().find { it.locale.language == locale.language } ?: ENGLISH
        }
    }
}
