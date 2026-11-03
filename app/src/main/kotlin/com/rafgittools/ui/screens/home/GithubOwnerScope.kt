package com.rafgittools.ui.screens.home

/**
 * Read-only owner scope: null is an explicit browse-all filter, never a
 * permission grant. A selected owner with zero matches must return empty.
 */
internal object GithubOwnerScope {
    fun matches(selectedOwner: String?, candidateOwner: String): Boolean =
        selectedOwner == null || (
            selectedOwner.isNotBlank() &&
                candidateOwner.equals(selectedOwner, ignoreCase = true)
        )

    fun <T> filter(
        items: List<T>,
        selectedOwner: String?,
        ownerOf: (T) -> String
    ): List<T> = items.filter { matches(selectedOwner, ownerOf(it)) }
}
