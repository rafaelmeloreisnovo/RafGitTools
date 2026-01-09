package com.rafgittools.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window size classes for responsive design
 */
enum class WindowSize {
    COMPACT,   // Phones in portrait
    MEDIUM,    // Tablets in portrait, phones in landscape
    EXPANDED   // Tablets in landscape, desktops
}

/**
 * Responsive breakpoints
 */
object ResponsiveBreakpoints {
    val COMPACT_MAX_WIDTH = 600.dp
    val MEDIUM_MAX_WIDTH = 840.dp
}

/**
 * Get the current window size class based on screen width
 */
@Composable
fun getWindowSize(): WindowSize {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    return when {
        screenWidth < ResponsiveBreakpoints.COMPACT_MAX_WIDTH -> WindowSize.COMPACT
        screenWidth < ResponsiveBreakpoints.MEDIUM_MAX_WIDTH -> WindowSize.MEDIUM
        else -> WindowSize.EXPANDED
    }
}

/**
 * Get responsive padding based on window size
 */
@Composable
fun getResponsivePadding(): Dp {
    return when (getWindowSize()) {
        WindowSize.COMPACT -> 16.dp
        WindowSize.MEDIUM -> 24.dp
        WindowSize.EXPANDED -> 32.dp
    }
}

/**
 * Get responsive content width
 */
@Composable
fun getResponsiveContentWidth(): Dp? {
    return when (getWindowSize()) {
        WindowSize.COMPACT -> null  // Full width
        WindowSize.MEDIUM -> 720.dp
        WindowSize.EXPANDED -> 1200.dp
    }
}
