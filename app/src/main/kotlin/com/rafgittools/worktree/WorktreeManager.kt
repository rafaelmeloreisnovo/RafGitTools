package com.rafgittools.worktree

/**
 * WorktreeManager stub.
 *
 * Provides placeholder functions for managing Git worktrees.
 */
object WorktreeManager {
    fun add(path: String, branch: String): Result<Unit> =
        Result.failure(NotImplementedError("Git worktree add is not implemented yet"))

    fun list(): Result<List<String>> =
        Result.failure(NotImplementedError("Git worktree list is not implemented yet"))

    fun remove(path: String): Result<Unit> =
        Result.failure(NotImplementedError("Git worktree remove is not implemented yet"))
}
