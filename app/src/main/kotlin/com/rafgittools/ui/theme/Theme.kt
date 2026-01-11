package com.rafgittools.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = InfoDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = ErrorDark,
    onPrimary = OnPrimaryDark,
    onSecondary = OnSecondaryDark,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark,
    onError = OnErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Info,
    background = Background,
    surface = Surface,
    error = Error,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onBackground = OnBackground,
    onSurface = OnSurface,
    onError = OnError
)

/**
 * AMOLED Black color scheme - Feature #220 from roadmap
 * Pure black background for AMOLED displays to save battery
 * and reduce eye strain in dark environments.
 */
private val AmoledBlackColorScheme = darkColorScheme(
    primary = PrimaryAmoled,
    secondary = SecondaryAmoled,
    tertiary = InfoAmoled,
    background = BackgroundAmoled,
    surface = SurfaceAmoled,
    surfaceVariant = SurfaceVariantAmoled,
    error = ErrorAmoled,
    onPrimary = OnPrimaryAmoled,
    onSecondary = OnSecondaryAmoled,
    onBackground = OnBackgroundAmoled,
    onSurface = OnSurfaceAmoled,
    onSurfaceVariant = OnSurfaceVariantAmoled,
    onError = OnErrorAmoled,
    outline = OutlineAmoled
)

/**
 * Theme mode for the application
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    AMOLED,
    SYSTEM // Follow system setting
}

@Composable
fun RafGitToolsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    // AMOLED black theme - Feature #220
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.AMOLED -> AmoledBlackColorScheme
        ThemeMode.LIGHT -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicLightColorScheme(LocalContext.current)
            } else {
                LightColorScheme
            }
        }
        ThemeMode.DARK -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(LocalContext.current)
            } else {
                DarkColorScheme
            }
        }
        ThemeMode.SYSTEM -> {
            when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }
                darkTheme -> DarkColorScheme
                else -> LightColorScheme
            }
        }
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Safely unwrap context to find Activity
            var context = view.context
            while (context is ContextWrapper && context !is Activity) {
                context = context.baseContext
            }
            (context as? Activity)?.window?.let { window ->
                window.statusBarColor = colorScheme.primary.toArgb()
                val isLightTheme = themeMode == ThemeMode.LIGHT || 
                    (themeMode == ThemeMode.SYSTEM && !darkTheme)
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLightTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
