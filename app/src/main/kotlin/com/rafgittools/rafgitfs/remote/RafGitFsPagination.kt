package com.rafgittools.rafgitfs.remote

object RafGitFsPagination {
    const val DEFAULT_PAGE_SIZE = 100
    const val DEFAULT_MAX_PAGES = 20

    private val pageRegex = Regex("[?&]page=(\\d+)")

    fun nextPage(linkHeader: String?): Int? {
        if (linkHeader.isNullOrBlank()) return null
        return linkHeader
            .split(',')
            .asSequence()
            .map(String::trim)
            .firstOrNull { it.contains("rel=\"next\"") }
            ?.substringBefore(';')
            ?.trim('<', '>', ' ')
            ?.let { pageRegex.find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }
    }

    fun validateBounds(perPage: Int, maxPages: Int) {
        require(perPage in 1..100) { "perPage must be between 1 and 100" }
        require(maxPages in 1..100) { "maxPages must be between 1 and 100" }
    }
}
