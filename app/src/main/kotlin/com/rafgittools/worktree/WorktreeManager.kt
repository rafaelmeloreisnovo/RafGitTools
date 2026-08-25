package com.rafgittools.worktree

import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

/**
 * WorktreeManager — Git worktree management via the system `git` binary.
 *
 * Git worktrees allow multiple working directories for the same repository,
 * each checked out to a different branch. This is useful for parallel
 * feature development, hotfix branches, or CI staging without stash/pop cycles.
 *
 * Requires Git 2.5+ (standard on Android Termux and modern Linux).
 * Operations that require a repository reject a blank [repoPath] as invalid
 * input; a missing caller argument is not an unimplemented execution path.
 *
 * See: git-worktree(1) man page.
 */
object WorktreeManager {

    private const val GIT_TIMEOUT_SECS = 30L
    private const val REPO_PATH_REQUIRED_MSG = "repoPath must not be blank"
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ─── Internal helpers ──────────────────────────────────────────────────

    private data class GitResult(val exitCode: Int, val stdout: String, val stderr: String)

    private fun checkGitAvailable(): Boolean = runCatching {
        val p = ProcessBuilder("git", "--version").start()
        p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0
    }.getOrDefault(false)

    private fun requireRepoPath(repoPath: String): Result<Unit>? =
        if (repoPath.isBlank()) Result.failure(IllegalArgumentException(REPO_PATH_REQUIRED_MSG)) else null

    private fun runGit(repoPath: String, vararg args: String): GitResult {
        if (!checkGitAvailable()) {
            return GitResult(-1, "", "git not found — install Termux and run: pkg install git")
        }
        return try {
            val process = ProcessBuilder(listOf("git") + args.toList())
                .directory(File(repoPath))
                .redirectErrorStream(true)
                .start()
            val outputFuture = ioScope.async { process.inputStream.bufferedReader().readText() }
            val finished = process.waitFor(GIT_TIMEOUT_SECS, TimeUnit.SECONDS)
            val output = runBlocking { outputFuture.await() }
            if (!finished) {
                process.destroyForcibly()
                process.waitFor()
                return GitResult(-1, "", "git worktree timed out after ${GIT_TIMEOUT_SECS}s")
            }
            GitResult(exitCode = process.exitValue(), stdout = output.trimEnd(), stderr = "")
        } catch (e: Exception) {
            GitResult(-1, "", "Failed to run git: ${e.message}")
        }
    }

    // ─── Public API ────────────────────────────────────────────────────────

    /**
     * Create a new linked worktree at [path] checked out to [branch].
     *
     * Equivalent to `git worktree add <path> <branch>`.
     * If [createBranch] is true, a new branch named [branch] is created
     * (equivalent to `git worktree add -b <branch> <path>`).
     *
     * @param path          filesystem path for the new worktree
     * @param branch        branch name to check out in the new worktree
     * @param repoPath      absolute path to the main repository
     * @param createBranch  create the branch if it does not yet exist
     */
    fun add(
        path: String,
        branch: String,
        repoPath: String = "",
        createBranch: Boolean = false
    ): Result<Unit> {
        requireRepoPath(repoPath)?.let { return it }
        return runCatching {
            val args = if (createBranch) {
                arrayOf("worktree", "add", "-b", branch, path)
            } else {
                arrayOf("worktree", "add", path, branch)
            }
            val r = runGit(repoPath, *args)
            if (r.exitCode != 0) throw IllegalStateException("git worktree add failed: ${r.stderr}")
        }
    }

    /**
     * List all worktrees attached to the repository.
     *
     * Equivalent to `git worktree list --porcelain`.
     *
     * @param repoPath  absolute path to the main repository
     * @return list of [WorktreeInfo] entries (main + all linked worktrees)
     */
    fun list(repoPath: String = ""): Result<List<WorktreeInfo>> {
        requireRepoPath(repoPath)?.let { return Result.failure(it.exceptionOrNull()!!) }
        return runCatching {
            val r = runGit(repoPath, "worktree", "list", "--porcelain")
            if (r.exitCode != 0) throw IllegalStateException("git worktree list failed: ${r.stderr}")
            parsePorcelain(r.stdout)
        }
    }

    /**
     * Remove a linked worktree at [path].
     *
     * Equivalent to `git worktree remove [--force] <path>`.
     *
     * @param path      filesystem path of the worktree to remove
     * @param repoPath  absolute path to the main repository
     * @param force     remove even if the worktree has modified files
     */
    fun remove(path: String, repoPath: String = "", force: Boolean = false): Result<Unit> {
        requireRepoPath(repoPath)?.let { return it }
        return runCatching {
            val args = if (force) {
                arrayOf("worktree", "remove", "--force", path)
            } else {
                arrayOf("worktree", "remove", path)
            }
            val r = runGit(repoPath, *args)
            if (r.exitCode != 0) throw IllegalStateException("git worktree remove failed: ${r.stderr}")
        }
    }

    /**
     * Prune stale worktree administrative files.
     *
     * Equivalent to `git worktree prune`.
     *
     * @param repoPath  absolute path to the main repository
     */
    fun prune(repoPath: String): Result<Unit> = runCatching {
        if (repoPath.isBlank()) throw IllegalArgumentException(REPO_PATH_REQUIRED_MSG)
        val r = runGit(repoPath, "worktree", "prune")
        if (r.exitCode != 0) throw IllegalStateException("git worktree prune failed: ${r.stderr}")
    }

    /**
     * Lock a worktree to prevent pruning (e.g. on a removable device).
     *
     * Equivalent to `git worktree lock <path>`.
     *
     * @param path      filesystem path of the worktree to lock
     * @param repoPath  absolute path to the main repository
     * @param reason    optional human-readable reason stored in the lock file
     */
    fun lock(path: String, repoPath: String, reason: String = ""): Result<Unit> = runCatching {
        if (repoPath.isBlank()) throw IllegalArgumentException(REPO_PATH_REQUIRED_MSG)
        val args = if (reason.isEmpty()) {
            arrayOf("worktree", "lock", path)
        } else {
            arrayOf("worktree", "lock", "--reason", reason, path)
        }
        val r = runGit(repoPath, *args)
        if (r.exitCode != 0) throw IllegalStateException("git worktree lock failed: ${r.stderr}")
    }

    /**
     * Unlock a previously locked worktree.
     *
     * Equivalent to `git worktree unlock <path>`.
     *
     * @param path      filesystem path of the worktree to unlock
     * @param repoPath  absolute path to the main repository
     */
    fun unlock(path: String, repoPath: String): Result<Unit> = runCatching {
        if (repoPath.isBlank()) throw IllegalArgumentException(REPO_PATH_REQUIRED_MSG)
        val r = runGit(repoPath, "worktree", "unlock", path)
        if (r.exitCode != 0) throw IllegalStateException("git worktree unlock failed: ${r.stderr}")
    }

    // ─── Porcelain parser ──────────────────────────────────────────────────

    private fun parsePorcelain(output: String): List<WorktreeInfo> {
        val worktrees = mutableListOf<WorktreeInfo>()
        var wtPath = ""; var head = ""; var branch = ""; var bare = false; var locked = false
        for (line in output.lineSequence()) {
            when {
                line.startsWith("worktree ") -> {
                    if (wtPath.isNotEmpty()) {
                        worktrees.add(WorktreeInfo(wtPath, head, branch, bare, locked))
                    }
                    wtPath = line.removePrefix("worktree ")
                    head = ""; branch = ""; bare = false; locked = false
                }
                line.startsWith("HEAD ") -> head = line.removePrefix("HEAD ")
                line.startsWith("branch ") -> branch = line.removePrefix("branch ")
                line == "bare" -> bare = true
                line == "locked" || line.startsWith("locked ") -> locked = true
            }
        }
        if (wtPath.isNotEmpty()) worktrees.add(WorktreeInfo(wtPath, head, branch, bare, locked))
        return worktrees
    }

    // ─── Data types ────────────────────────────────────────────────────────

    /**
     * Metadata for a single worktree entry.
     *
     * @param path    filesystem path of the worktree
     * @param head    SHA-1 of HEAD commit in this worktree
     * @param branch  full ref name of the checked-out branch (e.g. "refs/heads/main"),
     *                or blank for a detached HEAD
     * @param bare    true if this is the bare repository
     * @param locked  true if the worktree is locked against pruning
     */
    data class WorktreeInfo(
        val path: String,
        val head: String,
        val branch: String,
        val bare: Boolean = false,
        val locked: Boolean = false
    )
}
