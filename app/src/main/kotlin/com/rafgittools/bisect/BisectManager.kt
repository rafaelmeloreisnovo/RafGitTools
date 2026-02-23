package com.rafgittools.bisect

/**
 * BisectManager stub.
 *
 * Provides placeholder methods for performing git bisect to find
 * problematic commits. Real implementation would integrate with
 * JGit to automate bisect operations.
 */
object BisectManager {
    fun start(good: String, bad: String): Boolean {
        // TODO: start bisect between good and bad commit
        return true
    }

    fun markGood(commit: String): Boolean {
        // TODO: mark commit as good
        return true
    }

    fun markBad(commit: String): Boolean {
        // TODO: mark commit as bad
        return true
    }

    fun finish(): Boolean {
        // TODO: finish bisect and reset state
        return true
    }
}