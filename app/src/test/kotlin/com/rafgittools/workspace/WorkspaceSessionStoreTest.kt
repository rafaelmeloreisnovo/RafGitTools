package com.rafgittools.workspace

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkspaceSessionStoreTest {
    private fun resource(path: String, ref: String = "main") = ResourceRef(
        provider = "LOCAL_GIT",
        repositoryOrCorpus = "/repo",
        refOrGeneration = ref,
        pathOrLocator = path,
        objectId = "blob-" + path,
        visibility = ResourceVisibility.LOCAL_ONLY
    )

    @Test
    fun open_same_resource_reuses_tab_identity() {
        val store = WorkspaceSessionStore()
        val first = store.openTab(resource("a.kt"), "a.kt")
        val second = store.openTab(resource("a.kt"), "a.kt")

        assertThat(second.tabId).isEqualTo(first.tabId)
        assertThat(store.state.value.tabs).hasSize(1)
        assertThat(store.state.value.activeTabId).isEqualTo(first.tabId)
    }

    @Test
    fun opening_second_tab_creates_back_navigation() {
        val store = WorkspaceSessionStore()
        val first = store.openTab(resource("a.kt"), "a.kt")
        val second = store.openTab(resource("b.kt"), "b.kt")

        assertThat(store.state.value.activeTabId).isEqualTo(second.tabId)
        assertThat(store.state.value.backStack.map { it.tabId }).containsExactly(first.tabId)

        val back = store.navigateBack()
        assertThat(back?.tabId).isEqualTo(first.tabId)
        assertThat(store.state.value.canGoForward).isTrue()

        val forward = store.navigateForward()
        assertThat(forward?.tabId).isEqualTo(second.tabId)
    }

    @Test
    fun cursor_is_restored_by_jump_navigation() {
        val store = WorkspaceSessionStore()
        val first = store.openTab(resource("a.kt"), "a.kt")
        store.updateCursor(first.tabId, WorkspaceCursor(line = 42, column = 7))
        store.openTab(resource("b.kt"), "b.kt")

        val back = store.navigateBack()
        assertThat(back?.cursor).isEqualTo(WorkspaceCursor(42, 7))
    }

    @Test
    fun activating_tab_clears_forward_branch() {
        val store = WorkspaceSessionStore()
        val a = store.openTab(resource("a.kt"), "a.kt")
        val b = store.openTab(resource("b.kt"), "b.kt")
        val c = store.openTab(resource("c.kt"), "c.kt")

        store.navigateBack()
        assertThat(store.state.value.canGoForward).isTrue()

        store.activateTab(a.tabId)
        assertThat(store.state.value.activeTabId).isEqualTo(a.tabId)
        assertThat(store.state.value.forwardStack).isEmpty()
        assertThat(store.state.value.tabs.map { it.tabId }).containsExactly(a.tabId, b.tabId, c.tabId).inOrder()
    }

    @Test
    fun closing_active_tab_selects_neighbor_without_leaving_dead_history() {
        val store = WorkspaceSessionStore()
        val a = store.openTab(resource("a.kt"), "a.kt")
        val b = store.openTab(resource("b.kt"), "b.kt")

        val active = store.closeTab(b.tabId)
        assertThat(active?.tabId).isEqualTo(a.tabId)
        assertThat(store.state.value.tabs.map { it.tabId }).containsExactly(a.tabId)
        assertThat(store.state.value.backStack.any { it.tabId == b.tabId }).isFalse()
        assertThat(store.state.value.forwardStack.any { it.tabId == b.tabId }).isFalse()
    }

    @Test
    fun dirty_flag_is_session_metadata_only() {
        val store = WorkspaceSessionStore()
        val tab = store.openTab(resource("a.kt"), "a.kt")

        store.setDirty(tab.tabId, true)

        assertThat(store.state.value.activeTab?.dirty).isTrue()
    }

    @Test
    fun stable_id_changes_with_ref_or_path() {
        val aMain = resource("a.kt", "main").stableTabId()
        val aDev = resource("a.kt", "develop").stableTabId()
        val bMain = resource("b.kt", "main").stableTabId()

        assertThat(aMain).isNotEqualTo(aDev)
        assertThat(aMain).isNotEqualTo(bMain)
    }
}
