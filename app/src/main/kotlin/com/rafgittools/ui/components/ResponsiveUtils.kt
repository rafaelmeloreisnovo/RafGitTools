package com.rafgittools.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Window size classes used by RafGitTools adaptive surfaces. */
enum class WindowSize {
    COMPACT,
    MEDIUM,
    EXPANDED
}

/** Breakpoints intentionally match the existing RafGitTools UI contract. */
object ResponsiveBreakpoints {
    val COMPACT_MAX_WIDTH = 600.dp
    val MEDIUM_MAX_WIDTH = 840.dp
}

/** Pure classifier so breakpoint behavior can be unit tested without an Android runtime. */
fun windowSizeForWidth(width: Dp): WindowSize = when {
    width < ResponsiveBreakpoints.COMPACT_MAX_WIDTH -> WindowSize.COMPACT
    width < ResponsiveBreakpoints.MEDIUM_MAX_WIDTH -> WindowSize.MEDIUM
    else -> WindowSize.EXPANDED
}

fun responsivePaddingFor(windowSize: WindowSize): Dp = when (windowSize) {
    WindowSize.COMPACT -> 16.dp
    WindowSize.MEDIUM -> 24.dp
    WindowSize.EXPANDED -> 32.dp
}

fun responsiveContentWidthFor(windowSize: WindowSize): Dp? = when (windowSize) {
    WindowSize.COMPACT -> null
    WindowSize.MEDIUM -> 720.dp
    WindowSize.EXPANDED -> 1200.dp
}

/** Current window class. LocalConfiguration tracks window/configuration changes including rotation. */
@Composable
fun getWindowSize(): WindowSize {
    val configuration = LocalConfiguration.current
    return windowSizeForWidth(configuration.screenWidthDp.dp)
}

@Composable
fun getResponsivePadding(): Dp = responsivePaddingFor(getWindowSize())

@Composable
fun getResponsiveContentWidth(): Dp? = responsiveContentWidthFor(getWindowSize())

/**
 * Centers a page on medium/expanded windows while keeping compact windows full-width.
 *
 * This is a layout primitive, not proof that every screen has been device-tested at every size.
 */
@Composable
fun ResponsiveContentFrame(
    modifier: Modifier = Modifier,
    includeHorizontalPadding: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val windowSize = getWindowSize()
    val maxContentWidth = responsiveContentWidthFor(windowSize)
    val horizontalPadding = responsivePaddingFor(windowSize)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .then(
                    if (maxContentWidth == null) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.widthIn(max = maxContentWidth).fillMaxWidth()
                    }
                )
                .then(
                    if (includeHorizontalPadding) {
                        Modifier.padding(horizontal = horizontalPadding)
                    } else {
                        Modifier
                    }
                ),
            content = content
        )
    }
}
