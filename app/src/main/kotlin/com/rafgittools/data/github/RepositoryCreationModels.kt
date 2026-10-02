package com.rafgittools.data.github

import com.google.gson.annotations.SerializedName

/**
 * Request used by the repository bootstrap screen.
 * Visibility and README initialization are fixed in code; the UI cannot create a public repo.
 */
data class CreateUserRepositoryRequest(
    val name: String,
    val description: String? = null
) {
    @SerializedName("private")
    val isPrivate: Boolean = true

    @SerializedName("auto_init")
    val autoInit: Boolean = true
}

private val GITHUB_REPOSITORY_NAME = Regex("[A-Za-z0-9._-]{1,100}")

internal fun normalizeGitHubRepositoryName(raw: String): String? {
    val normalized = raw.trim()
    if (normalized == "." || normalized == "..") return null
    return normalized.takeIf(GITHUB_REPOSITORY_NAME::matches)
}
