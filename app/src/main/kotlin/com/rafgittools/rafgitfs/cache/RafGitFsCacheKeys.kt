package com.rafgittools.rafgitfs.cache

import java.security.MessageDigest

object RafGitFsCacheKeys {
    fun key(identity: RafGitFsCacheIdentity): String {
        val canonical = listOf(
            identity.profileId,
            identity.repositoryFullName,
            identity.refName,
            identity.path,
            identity.blobSha.lowercase()
        ).joinToString("\u0000")
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            }
    }

    fun relativePath(cacheKey: String): String {
        require(cacheKey.length == 64 && cacheKey.all { it in "0123456789abcdef" })
        return "${cacheKey.take(2)}/${cacheKey.drop(2).take(2)}/$cacheKey.bin"
    }
}
