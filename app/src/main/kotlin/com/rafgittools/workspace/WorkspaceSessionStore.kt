package com.rafgittools.workspace

import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ResourceVisibility {
    LOCAL_ONLY,
    PRIVATE,
    INTERNAL,
    PUBLIC,
    TOKEN_VAZIO
}

/**
 * Provider-neutral identity for something that can be opened in the workbench.
 *
 * Payload bytes are deliberately absent. A ResourceRef points to source; it is
 * not another copy of source content.
 */
data class ResourceRef(
    val provider: String,
    val repositoryOrCorpus: String,
    val refOrGeneration: String,
    val pathOrLocator: String,
    val objectId: String? = null,
    val visibility: ResourceVisibility = ResourceVisibility.TOKEN_VAZIO
) {
    init {
        require(provider.isNotBlank()) { "provider must not be blank" }
        require(repositoryOrCorpus.isNotBlank()) { "repositoryOrCorpus must not be blank" }
        require(refOrGeneration.isNotBlank()) { "refOrGeneration must not be blank" }
        require(pathOrLocator.isNotBlank()) { "pathOrLocator must not be blank" }
    }

    fun stableTabId(): String {
        val material = listOf(
            provider,
            repositoryOrCorpus,
            refOrGeneration,
            pathOrLocator,
            objectId.orEmpty()
        ).joinToString("\u001f")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(material.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "tab-${digest.take(24)}"
    }
}

data class WorkspaceCursor(
    val line: Int = 1,
    val column: Int = 0
) {
    init {
        require(line >= 1) { "line must be >= 1" }
        require(column >= 0) { "column must be >= 0" }
    }
}

data class WorkspaceTab(
    val tabId: String,
    val title: String,
    val resource: ResourceRef,
    val baseObjectId: String? = null,
    val cursor: WorkspaceCursor = WorkspaceCursor(),
    val dirty: Boolean = false
)

data class JumpEntry(
    val tabId: String,
    val cursor: WorkspaceCursor
)

data class WorkspaceSessionState(
    val tabs: List<WorkspaceTab> = emptyList(),
    val activeTabId: String? = null,
    val backStack: List<JumpEntry> = emptyList(),
    val forwardStack: List<JumpEntry> = emptyList()
) {
    val activeTab: WorkspaceTab?
        get() = activeTabId?.let { id -> tabs.firstOrNull { it.tabId == id } }

    val canGoBack: Boolean
        get() = backStack.isNotEmpty()

    val canGoForward: Boolean
        get() = forwardStack.isNotEmpty()
}

/**
 * App-scoped, in-memory workbench session.
 *
 * It stores references/navigation state only. File bodies, credentials and
 * clipboard history are intentionally outside this store.
 */
@Singleton
class WorkspaceSessionStore @Inject constructor() {
    companion object {
        private const val MAX_JUMP_HISTORY = 100
    }

    private val _state = MutableStateFlow(WorkspaceSessionState())
    val state: StateFlow<WorkspaceSessionState> = _state.asStateFlow()

    @Synchronized
    fun openTab(
        resource: ResourceRef,
        title: String,
        baseObjectId: String? = resource.objectId
    ): WorkspaceTab {
        require(title.isNotBlank()) { "title must not be blank" }
        val tabId = resource.stableTabId()
        val current = _state.value
        val existing = current.tabs.firstOrNull { it.tabId == tabId }
        if (existing != null) {
            activateTab(tabId)
            return _state.value.tabs.first { it.tabId == tabId }
        }

        val nextTab = WorkspaceTab(
            tabId = tabId,
            title = title,
            resource = resource,
            baseObjectId = baseObjectId
        )
        val previousJump = current.activeTab?.let { JumpEntry(it.tabId, it.cursor) }
        _state.value = current.copy(
            tabs = current.tabs + nextTab,
            activeTabId = tabId,
            backStack = previousJump?.let { appendBounded(current.backStack, it) }
                ?: current.backStack,
            forwardStack = emptyList()
        )
        return nextTab
    }

    @Synchronized
    fun activateTab(tabId: String): WorkspaceTab? {
        val current = _state.value
        val target = current.tabs.firstOrNull { it.tabId == tabId } ?: return null
        if (current.activeTabId == tabId) return target

        val previousJump = current.activeTab?.let { JumpEntry(it.tabId, it.cursor) }
        _state.value = current.copy(
            activeTabId = tabId,
            backStack = previousJump?.let { appendBounded(current.backStack, it) }
                ?: current.backStack,
            forwardStack = emptyList()
        )
        return target
    }

    @Synchronized
    fun closeTab(tabId: String): WorkspaceTab? {
        val current = _state.value
        val index = current.tabs.indexOfFirst { it.tabId == tabId }
        if (index < 0) return current.activeTab

        val remaining = current.tabs.filterNot { it.tabId == tabId }
        val prunedBack = current.backStack.filterNot { it.tabId == tabId }
        val prunedForward = current.forwardStack.filterNot { it.tabId == tabId }

        val nextActiveId = if (current.activeTabId != tabId) {
            current.activeTabId
        } else {
            remaining.getOrNull(index)?.tabId ?: remaining.getOrNull(index - 1)?.tabId
        }

        _state.value = current.copy(
            tabs = remaining,
            activeTabId = nextActiveId,
            backStack = prunedBack,
            forwardStack = prunedForward
        )
        return _state.value.activeTab
    }

    @Synchronized
    fun updateCursor(tabId: String, cursor: WorkspaceCursor) {
        val current = _state.value
        if (current.tabs.none { it.tabId == tabId }) return
        _state.value = current.copy(
            tabs = current.tabs.map { tab ->
                if (tab.tabId == tabId) tab.copy(cursor = cursor) else tab
            }
        )
    }

    @Synchronized
    fun setDirty(tabId: String, dirty: Boolean) {
        val current = _state.value
        if (current.tabs.none { it.tabId == tabId }) return
        _state.value = current.copy(
            tabs = current.tabs.map { tab ->
                if (tab.tabId == tabId) tab.copy(dirty = dirty) else tab
            }
        )
    }

    @Synchronized
    fun navigateBack(): WorkspaceTab? {
        val current = _state.value
        val jump = current.backStack.lastOrNull() ?: return null
        val target = current.tabs.firstOrNull { it.tabId == jump.tabId } ?: return null
        val active = current.activeTab
        val updatedTabs = current.tabs.map { tab ->
            if (tab.tabId == target.tabId) tab.copy(cursor = jump.cursor) else tab
        }
        _state.value = current.copy(
            tabs = updatedTabs,
            activeTabId = target.tabId,
            backStack = current.backStack.dropLast(1),
            forwardStack = active?.let {
                appendBounded(current.forwardStack, JumpEntry(it.tabId, it.cursor))
            } ?: current.forwardStack
        )
        return _state.value.activeTab
    }

    @Synchronized
    fun navigateForward(): WorkspaceTab? {
        val current = _state.value
        val jump = current.forwardStack.lastOrNull() ?: return null
        val target = current.tabs.firstOrNull { it.tabId == jump.tabId } ?: return null
        val active = current.activeTab
        val updatedTabs = current.tabs.map { tab ->
            if (tab.tabId == target.tabId) tab.copy(cursor = jump.cursor) else tab
        }
        _state.value = current.copy(
            tabs = updatedTabs,
            activeTabId = target.tabId,
            backStack = active?.let {
                appendBounded(current.backStack, JumpEntry(it.tabId, it.cursor))
            } ?: current.backStack,
            forwardStack = current.forwardStack.dropLast(1)
        )
        return _state.value.activeTab
    }

    @Synchronized
    fun clear() {
        _state.value = WorkspaceSessionState()
    }

    private fun appendBounded(
        stack: List<JumpEntry>,
        entry: JumpEntry
    ): List<JumpEntry> = (stack + entry).takeLast(MAX_JUMP_HISTORY)
}
