package com.rafgittools.gitlfs

/**
 * LfsManager stub.
 *
 * This object provides placeholder methods for Git LFS operations.
 * Real LFS support would interface with the Git LFS client to
 * track large files, pull/push LFS objects, and manage pointer files.
 */
object LfsManager {
    fun install(): Result<Unit> =
        Result.failure(NotImplementedError("Git LFS install is not implemented yet"))

    fun track(pattern: String): Result<Unit> =
        Result.failure(NotImplementedError("Git LFS track is not implemented yet"))

    fun fetch(): Result<Unit> =
        Result.failure(NotImplementedError("Git LFS fetch is not implemented yet"))
}
