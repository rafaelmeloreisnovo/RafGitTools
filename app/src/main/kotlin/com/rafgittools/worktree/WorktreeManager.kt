package com.rafgittools.worktree

/**
 * WorktreeManager stub.
 *
 * Provides placeholder functions for managing Git worktrees.
 */
object WorktreeManager {
    fun add(path: String, branch: String): Boolean {
        // TODO: add new worktree at path for branch
        return true
    }

    fun list(): List<String> {
        // TODO: return list of existing worktrees
        return emptyList()
    }

    fun remove(path: String): Boolean {
        // TODO: remove the specified worktree
        return true
    }
}