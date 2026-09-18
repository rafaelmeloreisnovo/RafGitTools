package com.rafgittools.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResponsiveUtilsTest {
    @Test
    fun breakpointBoundariesAreDeterministic() {
        assertEquals(WindowSize.COMPACT, windowSizeForWidth(0.dp))
        assertEquals(WindowSize.COMPACT, windowSizeForWidth(599.dp))
        assertEquals(WindowSize.MEDIUM, windowSizeForWidth(600.dp))
        assertEquals(WindowSize.MEDIUM, windowSizeForWidth(839.dp))
        assertEquals(WindowSize.EXPANDED, windowSizeForWidth(840.dp))
    }

    @Test
    fun layoutTokensMatchContract() {
        assertEquals(16.dp, responsivePaddingFor(WindowSize.COMPACT))
        assertEquals(24.dp, responsivePaddingFor(WindowSize.MEDIUM))
        assertEquals(32.dp, responsivePaddingFor(WindowSize.EXPANDED))
        assertNull(responsiveContentWidthFor(WindowSize.COMPACT))
        assertEquals(720.dp, responsiveContentWidthFor(WindowSize.MEDIUM))
        assertEquals(1200.dp, responsiveContentWidthFor(WindowSize.EXPANDED))
    }
}
