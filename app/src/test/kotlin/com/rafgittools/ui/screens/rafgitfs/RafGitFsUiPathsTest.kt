package com.rafgittools.ui.screens.rafgitfs

import org.junit.Assert.assertEquals
import org.junit.Test

class RafGitFsUiPathsTest {

    @Test
    fun rootRouteRoundTripIsStable() {
        assertEquals(RafGitFsUiPaths.ROOT_ROUTE_VALUE, RafGitFsUiPaths.routeValue(""))
        assertEquals("", RafGitFsUiPaths.fromRoute(RafGitFsUiPaths.ROOT_ROUTE_VALUE))
    }

    @Test
    fun nestedPathProducesOrderedBreadcrumbs() {
        val breadcrumbs = RafGitFsUiPaths.breadcrumbs("app/src/main")
        assertEquals(listOf("root", "app", "src", "main"), breadcrumbs.map { it.label })
        assertEquals(listOf("", "app", "app/src", "app/src/main"), breadcrumbs.map { it.path })
    }

    @Test
    fun parentNeverEscapesRoot() {
        assertEquals("", RafGitFsUiPaths.parent(""))
        assertEquals("", RafGitFsUiPaths.parent("README.md"))
        assertEquals("docs", RafGitFsUiPaths.parent("docs/README.md"))
    }

    @Test
    fun byteFormattingUsesBinaryUnits() {
        assertEquals("512 B", RafGitFsUiPaths.formatBytes(512))
        assertEquals("1.0 KiB", RafGitFsUiPaths.formatBytes(1024))
        assertEquals("1.0 MiB", RafGitFsUiPaths.formatBytes(1024L * 1024L))
    }
}
