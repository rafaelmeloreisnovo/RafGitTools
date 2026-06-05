package com.rafgittools.bisect

/**
 * BisectManager stub.
 *
 * Provides placeholder methods for performing git bisect to find
 * problematic commits. Real implementation would integrate with
 * JGit to automate bisect operations.
 */
object BisectManager {
    private var bisectInProgress: Boolean = false

    fun start(good: String, bad: String): Result<Unit> {
        return Result.failure(
            NotImplementedError("Bisect start is not implemented yet (good=$good, bad=$bad)")
        )
    }

    fun markGood(commit: String): Result<Unit> {
        return Result.failure(
            NotImplementedError("Bisect markGood is not implemented yet (commit=$commit)")
        )
    }

    fun markBad(commit: String): Result<Unit> {
        return Result.failure(
            NotImplementedError("Bisect markBad is not implemented yet (commit=$commit)")
        )
    }

    fun finish(): Result<Unit> {
        return Result.failure(
            NotImplementedError("Bisect finish is not implemented yet")
        )
    }

    internal fun resetStateForTesting() {
        bisectInProgress = false
    }

    internal fun isBisectInProgressForTesting(): Boolean = bisectInProgress
}
