package com.rafgittools.rafgitfs.remote

/** Metadata observed directly from one or more GitHub responses. */
data class RafGitFsRemoteMetadata(
    val pagesFetched: Int = 1,
    val requestId: String? = null,
    val etag: String? = null,
    val rateLimit: RafGitFsRateLimitSnapshot = RafGitFsRateLimitSnapshot.unknown(),
    val complete: Boolean = true
)

/**
 * Evidence-aware result used by the read-only engine.
 *
 * TOKEN_VAZIO may carry a partial value, but callers must not promote it to a
 * complete observation. Failures and rate limits are distinct from absence.
 */
sealed interface RafGitFsRemoteResult<out T> {
    data class Observed<T>(
        val value: T,
        val metadata: RafGitFsRemoteMetadata
    ) : RafGitFsRemoteResult<T>

    data class NotModified(
        val metadata: RafGitFsRemoteMetadata
    ) : RafGitFsRemoteResult<Nothing>

    data class TokenVazio<T>(
        val reason: String,
        val partialValue: T? = null,
        val metadata: RafGitFsRemoteMetadata
    ) : RafGitFsRemoteResult<T>

    data class RateLimited(
        val resetAtEpochSeconds: Long?,
        val retryAfterSeconds: Long?,
        val resource: String?,
        val message: String
    ) : RafGitFsRemoteResult<Nothing>

    data class Failure(
        val statusCode: Int?,
        val message: String,
        val retryable: Boolean
    ) : RafGitFsRemoteResult<Nothing>
}
