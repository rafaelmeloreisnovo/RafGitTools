package com.rafgittools.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Custom Theme Manager implementing Feature #221 from the roadmap.
 * 
 * Provides custom theme support with:
 * - Predefined color themes (GitHub, GitLab, Bitbucket, etc.)
 * - Color customization options
 * - Theme persistence
 */

/**
 * Predefined custom themes for the application
 */
enum class CustomTheme(
    val displayName: String,
    val lightColorScheme: ColorScheme,
    val darkColorScheme: ColorScheme
) {
    /**
     * Default GitHub-inspired theme
     */
    GITHUB(
        displayName = "GitHub",
        lightColorScheme = lightColorScheme(
            primary = Color(0xFF0969DA),
            secondary = Color(0xFF6E40C9),
            tertiary = Color(0xFF218BFF),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF6F8FA),
            error = Color(0xFFCF222E),
            onPrimary = Color(0xFFFFFFFF),
            onSecondary = Color(0xFFFFFFFF),
            onBackground = Color(0xFF1F2328),
            onSurface = Color(0xFF1F2328),
            onError = Color(0xFFFFFFFF)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFF58A6FF),
            secondary = Color(0xFFBC8CFF),
            tertiary = Color(0xFF58A6FF),
            background = Color(0xFF0D1117),
            surface = Color(0xFF161B22),
            error = Color(0xFFFF7B72),
            onPrimary = Color(0xFF0D1117),
            onSecondary = Color(0xFF0D1117),
            onBackground = Color(0xFFC9D1D9),
            onSurface = Color(0xFFC9D1D9),
            onError = Color(0xFF0D1117)
        )
    ),
    
    /**
     * GitLab-inspired theme
     */
    GITLAB(
        displayName = "GitLab",
        lightColorScheme = lightColorScheme(
            primary = Color(0xFFFC6D26),
            secondary = Color(0xFF6B4FBB),
            tertiary = Color(0xFF1F75CB),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFFAFAFA),
            error = Color(0xFFDD2B0E),
            onPrimary = Color(0xFFFFFFFF),
            onSecondary = Color(0xFFFFFFFF),
            onBackground = Color(0xFF303030),
            onSurface = Color(0xFF303030),
            onError = Color(0xFFFFFFFF)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFFFC6D26),
            secondary = Color(0xFF9B8FFF),
            tertiary = Color(0xFF63A6E9),
            background = Color(0xFF1F1F1F),
            surface = Color(0xFF2D2D2D),
            error = Color(0xFFFF5F49),
            onPrimary = Color(0xFF1F1F1F),
            onSecondary = Color(0xFF1F1F1F),
            onBackground = Color(0xFFE5E5E5),
            onSurface = Color(0xFFE5E5E5),
            onError = Color(0xFF1F1F1F)
        )
    ),
    
    /**
     * Bitbucket-inspired theme
     */
    BITBUCKET(
        displayName = "Bitbucket",
        lightColorScheme = lightColorScheme(
            primary = Color(0xFF0052CC),
            secondary = Color(0xFF6554C0),
            tertiary = Color(0xFF00B8D9),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF4F5F7),
            error = Color(0xFFDE350B),
            onPrimary = Color(0xFFFFFFFF),
            onSecondary = Color(0xFFFFFFFF),
            onBackground = Color(0xFF172B4D),
            onSurface = Color(0xFF172B4D),
            onError = Color(0xFFFFFFFF)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFF579DFF),
            secondary = Color(0xFF9F8FEF),
            tertiary = Color(0xFF6CC3E0),
            background = Color(0xFF161A1D),
            surface = Color(0xFF22272B),
            error = Color(0xFFFF5630),
            onPrimary = Color(0xFF161A1D),
            onSecondary = Color(0xFF161A1D),
            onBackground = Color(0xFFB6C2CF),
            onSurface = Color(0xFFB6C2CF),
            onError = Color(0xFF161A1D)
        )
    ),
    
    /**
     * Azure DevOps-inspired theme
     */
    AZURE_DEVOPS(
        displayName = "Azure DevOps",
        lightColorScheme = lightColorScheme(
            primary = Color(0xFF0078D4),
            secondary = Color(0xFF5C2D91),
            tertiary = Color(0xFF0099BC),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF3F2F1),
            error = Color(0xFFD13438),
            onPrimary = Color(0xFFFFFFFF),
            onSecondary = Color(0xFFFFFFFF),
            onBackground = Color(0xFF323130),
            onSurface = Color(0xFF323130),
            onError = Color(0xFFFFFFFF)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFF2899F5),
            secondary = Color(0xFF8764B8),
            tertiary = Color(0xFF30B0C7),
            background = Color(0xFF1B1A19),
            surface = Color(0xFF252423),
            error = Color(0xFFF1707B),
            onPrimary = Color(0xFF1B1A19),
            onSecondary = Color(0xFF1B1A19),
            onBackground = Color(0xFFE1DFDD),
            onSurface = Color(0xFFE1DFDD),
            onError = Color(0xFF1B1A19)
        )
    ),
    
    /**
     * Dracula theme - popular dark theme
     */
    DRACULA(
        displayName = "Dracula",
        lightColorScheme = lightColorScheme(
            primary = Color(0xFFBD93F9),
            secondary = Color(0xFFFF79C6),
            tertiary = Color(0xFF8BE9FD),
            background = Color(0xFFF8F8F2),
            surface = Color(0xFFE8E8E2),
            error = Color(0xFFFF5555),
            onPrimary = Color(0xFF282A36),
            onSecondary = Color(0xFF282A36),
            onBackground = Color(0xFF282A36),
            onSurface = Color(0xFF282A36),
            onError = Color(0xFFF8F8F2)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFFBD93F9),
            secondary = Color(0xFFFF79C6),
            tertiary = Color(0xFF8BE9FD),
            background = Color(0xFF282A36),
            surface = Color(0xFF44475A),
            error = Color(0xFFFF5555),
            onPrimary = Color(0xFF282A36),
            onSecondary = Color(0xFF282A36),
            onBackground = Color(0xFFF8F8F2),
            onSurface = Color(0xFFF8F8F2),
            onError = Color(0xFFF8F8F2)
        )
    ),
    
    /**
     * Nord theme - Arctic, north-bluish color palette
     */
    NORD(
        displayName = "Nord",
        lightColorScheme = lightColorScheme(
            primary = Color(0xFF5E81AC),
            secondary = Color(0xFFB48EAD),
            tertiary = Color(0xFF88C0D0),
            background = Color(0xFFECEFF4),
            surface = Color(0xFFE5E9F0),
            error = Color(0xFFBF616A),
            onPrimary = Color(0xFFECEFF4),
            onSecondary = Color(0xFFECEFF4),
            onBackground = Color(0xFF2E3440),
            onSurface = Color(0xFF2E3440),
            onError = Color(0xFFECEFF4)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFF88C0D0),
            secondary = Color(0xFFB48EAD),
            tertiary = Color(0xFF81A1C1),
            background = Color(0xFF2E3440),
            surface = Color(0xFF3B4252),
            error = Color(0xFFBF616A),
            onPrimary = Color(0xFF2E3440),
            onSecondary = Color(0xFF2E3440),
            onBackground = Color(0xFFECEFF4),
            onSurface = Color(0xFFD8DEE9),
            onError = Color(0xFFECEFF4)
        )
    ),
    
    /**
     * Solarized theme
     */
    SOLARIZED(
        displayName = "Solarized",
        lightColorScheme = lightColorScheme(
            primary = Color(0xFF268BD2),
            secondary = Color(0xFF6C71C4),
            tertiary = Color(0xFF2AA198),
            background = Color(0xFFFDF6E3),
            surface = Color(0xFFEEE8D5),
            error = Color(0xFFDC322F),
            onPrimary = Color(0xFFFDF6E3),
            onSecondary = Color(0xFFFDF6E3),
            onBackground = Color(0xFF657B83),
            onSurface = Color(0xFF586E75),
            onError = Color(0xFFFDF6E3)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFF268BD2),
            secondary = Color(0xFF6C71C4),
            tertiary = Color(0xFF2AA198),
            background = Color(0xFF002B36),
            surface = Color(0xFF073642),
            error = Color(0xFFDC322F),
            onPrimary = Color(0xFF002B36),
            onSecondary = Color(0xFF002B36),
            onBackground = Color(0xFF839496),
            onSurface = Color(0xFF93A1A1),
            onError = Color(0xFFFDF6E3)
        )
    ),
    
    /**
     * Monokai Pro theme - popular code editor theme
     */
    MONOKAI(
        displayName = "Monokai Pro",
        lightColorScheme = lightColorScheme(
            primary = Color(0xFFA9DC76),
            secondary = Color(0xFFFF6188),
            tertiary = Color(0xFF78DCE8),
            background = Color(0xFFFCFCFA),
            surface = Color(0xFFF0F0ED),
            error = Color(0xFFFF6188),
            onPrimary = Color(0xFF2D2A2E),
            onSecondary = Color(0xFF2D2A2E),
            onBackground = Color(0xFF2D2A2E),
            onSurface = Color(0xFF2D2A2E),
            onError = Color(0xFFFCFCFA)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFFA9DC76),
            secondary = Color(0xFFFF6188),
            tertiary = Color(0xFF78DCE8),
            background = Color(0xFF2D2A2E),
            surface = Color(0xFF403E41),
            error = Color(0xFFFF6188),
            onPrimary = Color(0xFF2D2A2E),
            onSecondary = Color(0xFF2D2A2E),
            onBackground = Color(0xFFFCFCFA),
            onSurface = Color(0xFFFCFCFA),
            onError = Color(0xFFFCFCFA)
        )
    );
    
    companion object {
        /**
         * Get theme by name (case-insensitive)
         */
        fun fromName(name: String): CustomTheme? {
            return entries.find { it.name.equals(name, ignoreCase = true) }
        }
        
        /**
         * Get theme by display name
         */
        fun fromDisplayName(displayName: String): CustomTheme? {
            return entries.find { it.displayName.equals(displayName, ignoreCase = true) }
        }
        
        /**
         * Get all available themes
         */
        fun getAllThemes(): List<CustomTheme> = entries
    }
}

/**
 * Custom color configuration for user-defined themes
 */
data class CustomColorConfig(
    val primaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val errorColor: Color,
    val onPrimaryColor: Color,
    val onSecondaryColor: Color,
    val onBackgroundColor: Color,
    val onSurfaceColor: Color,
    val onErrorColor: Color
) {
    /**
     * Convert to Material3 ColorScheme
     */
    fun toColorScheme(): ColorScheme {
        return lightColorScheme(
            primary = primaryColor,
            secondary = secondaryColor,
            tertiary = tertiaryColor,
            background = backgroundColor,
            surface = surfaceColor,
            error = errorColor,
            onPrimary = onPrimaryColor,
            onSecondary = onSecondaryColor,
            onBackground = onBackgroundColor,
            onSurface = onSurfaceColor,
            onError = onErrorColor
        )
    }
    
    companion object {
        /**
         * Create from a predefined custom theme
         */
        fun fromCustomTheme(theme: CustomTheme, isDark: Boolean): CustomColorConfig {
            val colorScheme = if (isDark) theme.darkColorScheme else theme.lightColorScheme
            return CustomColorConfig(
                primaryColor = colorScheme.primary,
                secondaryColor = colorScheme.secondary,
                tertiaryColor = colorScheme.tertiary,
                backgroundColor = colorScheme.background,
                surfaceColor = colorScheme.surface,
                errorColor = colorScheme.error,
                onPrimaryColor = colorScheme.onPrimary,
                onSecondaryColor = colorScheme.onSecondary,
                onBackgroundColor = colorScheme.onBackground,
                onSurfaceColor = colorScheme.onSurface,
                onErrorColor = colorScheme.onError
            )
        }
    }
}
