package com.rafgittools.rafgitfs.remote

import okhttp3.Headers

data class RafGitFsRateLimitSnapshot(
    val limit: Long?,
    val remaining: Long?,
    val used: Long?,
    val resetAtEpochSeconds: Long?,
    val retryAfterSeconds: Long?,
    val resource: String?
) {
    val exhausted: Boolean get() = remaining == 0L

    companion object {
        fun unknown() = RafGitFsRateLimitSnapshot(null, null, null, null, null, null)

        fun from(headers: Headers): RafGitFsRateLimitSnapshot = RafGitFsRateLimitSnapshot(
            limit = headers["X-RateLimit-Limit"]?.toLongOrNull(),
            remaining = headers["X-RateLimit-Remaining"]?.toLongOrNull(),
            used = headers["X-RateLimit-Used"]?.toLongOrNull(),
            resetAtEpochSeconds = headers["X-RateLimit-Reset"]?.toLongOrNull(),
            retryAfterSeconds = headers["Retry-After"]?.toLongOrNull(),
            resource = headers["X-RateLimit-Resource"]
        )
    }
}
