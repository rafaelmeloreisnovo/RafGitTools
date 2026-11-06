package com.rafgittools.data.github

/** Deterministic, non-authorizing organization input and owner-result checks. */
object GithubOrganizationInventory {
    private val loginPattern = Regex("[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?")

    fun isValidLogin(value: String): Boolean =
        value.length <= 39 && loginPattern.matches(value)

    fun matchesOwner(organization: String, candidateOwner: String): Boolean =
        isValidLogin(organization) &&
            candidateOwner.equals(organization, ignoreCase = true)

    /**
     * Fail closed if GitHub returns a different owner. The account selector
     * must never mix repositories of organizations outside the selected one.
     */
    fun <T> requireScopedOwner(
        organization: String,
        repositories: List<T>,
        ownerOf: (T) -> String
    ): List<T> {
        require(isValidLogin(organization)) { "INVALID_ORGANIZATION_LOGIN" }
        require(repositories.all { matchesOwner(organization, ownerOf(it)) }) {
            "ORGANIZATION_OWNER_MISMATCH"
        }
        return repositories
    }
}
