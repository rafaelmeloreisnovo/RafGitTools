package com.rafgittools.core.localization

import java.util.Locale

/**
 * Supported languages in RafGitTools
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
