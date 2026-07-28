package com.rafgittools.gitlfs

import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

/**
 * LfsManager — Git LFS operations via the system `git-lfs` binary.
 *
 * Requires `git-lfs` to be installed on the device (Termux: `pkg install git-lfs`).
 * All methods return a descriptive [IllegalStateException] when git-lfs is not found,
 * and a [NotImplementedError] when [repoPath] is empty so that stub-mode callers
 * continue to receive a clear signal.
 *
 * Git LFS documentation: https://git-lfs.com
 */
object LfsManager {

    private const val LFS_TIMEOUT_SECS = 60L
    private const val LFS_NOT_FOUND_MSG =
        "git-lfs not found. On Termux run: pkg install git-lfs"
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ─── Internal helpers ──────────────────────────────────────────────────

    private data class CmdResult(val exitCode: Int, val stdout: String, val stderr: String)

    private fun isLfsAvailable(): Boolean = runCatching {
        val p = ProcessBuilder("git", "lfs", "version")
            .redirectErrorStream(true)
            .start()
        p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0
    }.getOrDefault(false)

    private fun runLfs(repoPath: String, vararg args: String): CmdResult {
        return try {
            val process = ProcessBuilder(listOf("git", "lfs") + args.toList())
                .directory(File(repoPath))
                .redirectErrorStream(true)
                .start()
            val outputFuture = ioScope.async { process.inputStream.bufferedReader().readText() }
            val finished = process.waitFor(LFS_TIMEOUT_SECS, TimeUnit.SECONDS)
            val output = runBlocking { outputFuture.await() }
            if (!finished) {
                process.destroyForcibly()
                process.waitFor()
                return CmdResult(-1, "", "git lfs timed out after ${LFS_TIMEOUT_SECS}s")
            }
            CmdResult(exitCode = process.exitValue(), stdout = output.trimEnd(), stderr = "")
        } catch (e: Exception) {
            CmdResult(-1, "", "Failed to run git lfs: ${e.message}")
        }
    }

    // ─── Public API ────────────────────────────────────────────────────────

    /**
     * Initialise Git LFS in the repository at [repoPath].
     *
     * Equivalent to `git lfs install`. Writes LFS hooks and configures
     * the repository's `.gitattributes` filter entries.
     *
     * @param repoPath  absolute path to the local git repository
     */
    fun install(repoPath: String = ""): Result<Unit> {
        if (repoPath.isEmpty()) {
            return Result.failure(NotImplementedError("Git LFS install is not implemented yet"))
        }
        return runCatching {
            if (!isLfsAvailable()) throw IllegalStateException(LFS_NOT_FOUND_MSG)
            val r = runLfs(repoPath, "install")
            if (r.exitCode != 0) throw IllegalStateException("git lfs install failed: ${r.stderr}")
        }
    }

    /**
     * Track files matching [pattern] with Git LFS.
     *
     * Equivalent to `git lfs track "<pattern>"`. Appends a rule to
     * `.gitattributes` — remember to `git add .gitattributes`.
     *
     * @param pattern   glob pattern (e.g. "*.psd", "assets/**/*.bin")
     * @param repoPath  absolute path to the local git repository
     */
    fun track(pattern: String, repoPath: String = ""): Result<Unit> {
        if (repoPath.isEmpty()) {
            return Result.failure(NotImplementedError("Git LFS track is not implemented yet"))
        }
        return runCatching {
            if (!isLfsAvailable()) throw IllegalStateException(LFS_NOT_FOUND_MSG)
            val r = runLfs(repoPath, "track", pattern)
            if (r.exitCode != 0) throw IllegalStateException("git lfs track failed: ${r.stderr}")
        }
    }

    /**
     * Fetch LFS objects for the current branch from the remote.
     *
     * Equivalent to `git lfs fetch [remote]`.
     *
     * @param repoPath  absolute path to the local git repository
     * @param remote    git remote name (defaults to "origin")
     */
    fun fetch(repoPath: String = "", remote: String = "origin"): Result<Unit> {
        if (repoPath.isEmpty()) {
            return Result.failure(NotImplementedError("Git LFS fetch is not implemented yet"))
        }
        return runCatching {
            if (!isLfsAvailable()) throw IllegalStateException(LFS_NOT_FOUND_MSG)
            val r = runLfs(repoPath, "fetch", remote)
            if (r.exitCode != 0) throw IllegalStateException("git lfs fetch failed: ${r.stderr}")
        }
    }

    /**
     * Pull LFS objects (fetch + checkout) for the current branch.
     *
     * Equivalent to `git lfs pull [remote]`.
     *
     * @param repoPath  absolute path to the local git repository
     * @param remote    git remote name (defaults to "origin")
     */
    fun pull(repoPath: String, remote: String = "origin"): Result<Unit> = runCatching {
        if (!isLfsAvailable()) throw IllegalStateException(LFS_NOT_FOUND_MSG)
        val r = runLfs(repoPath, "pull", remote)
        if (r.exitCode != 0) throw IllegalStateException("git lfs pull failed: ${r.stderr}")
    }

    /**
     * Push LFS objects to the remote.
     *
     * Equivalent to `git lfs push [remote] [ref]`.
     *
     * @param repoPath  absolute path to the local git repository
     * @param remote    git remote name (defaults to "origin")
     * @param ref       branch or commit ref to push (defaults to current HEAD)
     */
    fun push(repoPath: String, remote: String = "origin", ref: String = "HEAD"): Result<Unit> = runCatching {
        if (!isLfsAvailable()) throw IllegalStateException(LFS_NOT_FOUND_MSG)
        val r = runLfs(repoPath, "push", remote, ref)
        if (r.exitCode != 0) throw IllegalStateException("git lfs push failed: ${r.stderr}")
    }

    /**
     * List LFS-tracked file patterns defined in `.gitattributes`.
     *
     * @param repoPath  absolute path to the local git repository
     * @return list of tracked glob patterns
     */
    fun listTracked(repoPath: String): Result<List<String>> = runCatching {
        if (!isLfsAvailable()) throw IllegalStateException(LFS_NOT_FOUND_MSG)
        val r = runLfs(repoPath, "track")
        if (r.exitCode != 0) throw IllegalStateException("git lfs track list failed: ${r.stderr}")
        r.stdout.lines()
            .filter { it.trimStart().startsWith("*") || it.trimStart().startsWith("*.") }
            .map { it.trim() }
    }

    /**
     * Display LFS environment info for the repository (for diagnostics).
     *
     * @param repoPath  absolute path to the local git repository
     */
    fun env(repoPath: String): Result<String> = runCatching {
        if (!isLfsAvailable()) throw IllegalStateException(LFS_NOT_FOUND_MSG)
        val r = runLfs(repoPath, "env")
        if (r.exitCode != 0) throw IllegalStateException("git lfs env failed: ${r.stderr}")
        r.stdout
    }

    /** Returns true when the git-lfs binary is accessible on this device. */
    fun isAvailable(): Boolean = isLfsAvailable()
}
