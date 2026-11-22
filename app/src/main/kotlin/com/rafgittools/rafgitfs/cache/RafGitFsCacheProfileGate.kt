package com.rafgittools.rafgitfs.cache

import kotlinx.coroutines.sync.Mutex

/**
 * Serializes cache mutations by profile in this application process.
 * Fixed stripes bound memory use; different profiles may share a stripe.
 */
internal class RafGitFsCacheProfileGate {
    private val stripes = Array(64) { Mutex() }

    suspend fun <T> withProfile(profileId: String, action: suspend () -> T): T {
        require(profileId.isNotBlank()) { "profileId must not be blank" }
        val stripe = (profileId.hashCode() and Int.MAX_VALUE) % stripes.size
        val mutex = stripes[stripe]
        mutex.lock()
        try {
            return action()
        } finally {
            mutex.unlock()
        }
    }
}
