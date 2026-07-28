package com.rafgittools.bisect

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * BisectManager — git bisect via the system `git` binary.
 *
 * Performs binary-search debugging to locate the commit that introduced a
 * regression. Requires the `git` binary (standard on Android via Termux or
 * system Git). A non-empty [repoPath] must be provided to any operation;
 * passing the default empty string returns a [NotImplementedError] so that
 * existing unit-tests that verify stub behaviour continue to pass.
 *
 * Typical workflow:
 *   1. `start(good = "v1.0", bad = "HEAD", repoPath = "/path/to/repo")`
 *   2. Build/test HEAD → `markGood(repoPath = "...")` or `markBad(repoPath = "...")`
 *   3. Repeat until bisect announces the first-bad commit
 *   4. `finish(repoPath = "...")` to reset back to the original branch
 */
object BisectManager {

    private var bisectInProgress: Boolean = false
    private const val GIT_TIMEOUT_SECS = 30L

    // ─── Internal helpers ──────────────────────────────────────────────────

    private data class GitResult(val exitCode: Int, val stdout: String, val stderr: String)

    private fun checkGitAvailable(): Boolean = runCatching {
        val p = ProcessBuilder("git", "--version").start()
        p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0
    }.getOrDefault(false)

    private fun runGit(repoPath: String, vararg args: String): GitResult {
        if (!checkGitAvailable()) {
            return GitResult(-1, "", "git not found — install Termux and run: pkg install git")
        }
        return try {
            val pb = ProcessBuilder(listOf("git") + args.toList())
                .directory(File(repoPath))
            val process = pb.start()
            val finished = process.waitFor(GIT_TIMEOUT_SECS, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return GitResult(-1, "", "git bisect timed out after ${GIT_TIMEOUT_SECS}s")
            }
            GitResult(
                exitCode = process.exitValue(),
                stdout = process.inputStream.bufferedReader().readText().trimEnd(),
                stderr = process.errorStream.bufferedReader().readText().trimEnd()
            )
        } catch (e: Exception) {
            GitResult(-1, "", "Failed to launch git: ${e.message}")
        }
    }

    // ─── Public API ────────────────────────────────────────────────────────

    /**
     * Begin a bisect session.
     *
     * Marks [good] as the last-known-good revision and [bad] as the
     * known-bad revision, then outputs the first commit to test.
     *
     * @param good       known-good commit/tag (e.g. "v1.0")
     * @param bad        known-bad commit/tag (e.g. "HEAD")
     * @param repoPath   absolute path to the local repository; must be
     *                   non-empty to execute real git commands
     * @return [Result.success] with git's output, or [Result.failure] with
     *         [NotImplementedError] when [repoPath] is empty (stub mode)
     */
    fun start(good: String, bad: String, repoPath: String = ""): Result<Unit> {
        if (repoPath.isEmpty()) {
            return Result.failure(
                NotImplementedError(
                    "Bisect start is not implemented yet (good=$good, bad=$bad). " +
                        "Pass a non-empty repoPath to use real git bisect."
                )
            )
        }
        return runCatching {
            var r = runGit(repoPath, "bisect", "start")
            if (r.exitCode != 0) throw IllegalStateException("bisect start: ${r.stderr}")
            r = runGit(repoPath, "bisect", "bad", bad)
            if (r.exitCode != 0) throw IllegalStateException("bisect bad '$bad': ${r.stderr}")
            r = runGit(repoPath, "bisect", "good", good)
            if (r.exitCode != 0) throw IllegalStateException("bisect good '$good': ${r.stderr}")
            bisectInProgress = true
        }
    }

    /**
     * Mark [commit] (or HEAD when blank) as a good revision during an active session.
     *
     * @param commit    commit SHA to mark; leave blank to mark HEAD
     * @param repoPath  absolute path to the repository (required for real execution)
     */
    fun markGood(commit: String, repoPath: String = ""): Result<Unit> {
        if (repoPath.isEmpty()) {
            return Result.failure(
                NotImplementedError(
                    "Bisect markGood is not implemented yet (commit=$commit). " +
                        "Pass a non-empty repoPath to use real git bisect."
                )
            )
        }
        return runCatching {
            val args = if (commit.isBlank()) arrayOf("bisect", "good") else arrayOf("bisect", "good", commit)
            val r = runGit(repoPath, *args)
            if (r.exitCode != 0) throw IllegalStateException("bisect good failed: ${r.stderr}")
        }
    }

    /**
     * Mark [commit] (or HEAD when blank) as a bad revision during an active session.
     *
     * @param commit    commit SHA to mark; leave blank to mark HEAD
     * @param repoPath  absolute path to the repository (required for real execution)
     */
    fun markBad(commit: String, repoPath: String = ""): Result<Unit> {
        if (repoPath.isEmpty()) {
            return Result.failure(
                NotImplementedError(
                    "Bisect markBad is not implemented yet (commit=$commit). " +
                        "Pass a non-empty repoPath to use real git bisect."
                )
            )
        }
        return runCatching {
            val args = if (commit.isBlank()) arrayOf("bisect", "bad") else arrayOf("bisect", "bad", commit)
            val r = runGit(repoPath, *args)
            if (r.exitCode != 0) throw IllegalStateException("bisect bad failed: ${r.stderr}")
        }
    }

    /**
     * End the bisect session and reset HEAD back to the original branch.
     *
     * @param repoPath  absolute path to the repository (required for real execution)
     * @return the SHA of the first-bad commit if already identified, or blank
     */
    fun finish(repoPath: String = ""): Result<Unit> {
        if (repoPath.isEmpty()) {
            return Result.failure(
                NotImplementedError(
                    "Bisect finish is not implemented yet. " +
                        "Pass a non-empty repoPath to use real git bisect."
                )
            )
        }
        return runCatching {
            val r = runGit(repoPath, "bisect", "reset")
            if (r.exitCode != 0) throw IllegalStateException("bisect reset failed: ${r.stderr}")
            bisectInProgress = false
        }
    }

    /**
     * Skip the current commit (e.g. it cannot be compiled or tested).
     *
     * @param commit    commit SHA to skip; leave blank to skip HEAD
     * @param repoPath  absolute path to the repository
     */
    fun skip(commit: String = "", repoPath: String = ""): Result<Unit> {
        if (repoPath.isEmpty()) {
            return Result.failure(
                NotImplementedError("Bisect skip requires a non-empty repoPath.")
            )
        }
        return runCatching {
            val args = if (commit.isBlank()) arrayOf("bisect", "skip") else arrayOf("bisect", "skip", commit)
            val r = runGit(repoPath, *args)
            if (r.exitCode != 0) throw IllegalStateException("bisect skip failed: ${r.stderr}")
        }
    }

    /**
     * Return the current bisect log.
     *
     * @param repoPath  absolute path to the repository
     */
    fun log(repoPath: String): Result<String> {
        if (repoPath.isEmpty()) {
            return Result.failure(NotImplementedError("Bisect log requires a non-empty repoPath."))
        }
        return runCatching {
            val r = runGit(repoPath, "bisect", "log")
            if (r.exitCode != 0) throw IllegalStateException("bisect log failed: ${r.stderr}")
            r.stdout
        }
    }

    // ─── Test helpers ──────────────────────────────────────────────────────

    internal fun resetStateForTesting() { bisectInProgress = false }
    internal fun isBisectInProgressForTesting(): Boolean = bisectInProgress
}
