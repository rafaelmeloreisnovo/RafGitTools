package com.rafgittools.core.vcs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.eclipse.jgit.api.Git
import java.io.File

data class WorktreeInfo(
    val path: String,
    val branch: String,
    val commitHash: String,
    val isPrunable: Boolean = false
)

class WorktreeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private fun runGit(repoPath: String, vararg args: String): String {
        val process = ProcessBuilder(listOf("git") + args.toList())
            .directory(File(repoPath))
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw IllegalStateException("git ${args.joinToString(" ")} failed ($exitCode): ${output.trim()}")
        }
        return output
    }

    fun listWorktrees(): Result<List<WorktreeInfo>> = runCatching {
        val repoPath = context.filesDir.absolutePath
        val git = Git.open(File(repoPath))
        val repo = git.repository

        val worktrees = mutableListOf<WorktreeInfo>()
        val worktreeDir = File(repo.directory.parentFile, ".git/worktrees")

        if (worktreeDir.exists()) {
            worktreeDir.listFiles()?.forEach { wtDir ->
                val gitFile = File(wtDir, "gitdir")
                if (gitFile.exists()) {
                    val wtPath = gitFile.readText().trim()
                    val wtGit = Git.open(File(wtPath))
                    val branch = wtGit.repository.branch
                    val headRef = wtGit.repository.findRef("HEAD")
                    val commitHash = headRef?.objectId?.abbreviate(40)?.name() ?: "unknown"

                    worktrees.add(
                        WorktreeInfo(
                            path = wtPath,
                            branch = branch,
                            commitHash = commitHash,
                            isPrunable = false
                        )
                    )
                    wtGit.close()
                }
            }
        }

        git.close()
        worktrees
    }

    fun createWorktree(
        path: String,
        branchName: String,
        commitHash: String? = null
    ): Result<WorktreeInfo> = runCatching {
        val repoPath = context.filesDir.absolutePath
        val wtFile = File(path)
        wtFile.parentFile?.mkdirs()

        if (commitHash != null) {
            runGit(repoPath, "worktree", "add", "-b", branchName, path, commitHash)
        } else {
            runGit(repoPath, "worktree", "add", path, branchName)
        }

        val wtGit = Git.open(wtFile)
        val branch = wtGit.repository.branch
        val headRef = wtGit.repository.findRef("HEAD")
        val commitId = headRef?.objectId?.abbreviate(40)?.name() ?: "unknown"
        wtGit.close()

        WorktreeInfo(
            path = path,
            branch = branch,
            commitHash = commitId,
            isPrunable = false
        )
    }

    fun deleteWorktree(path: String): Result<Unit> = runCatching {
        val repoPath = context.filesDir.absolutePath
        runGit(repoPath, "worktree", "remove", path)
        Unit
    }

    fun getBranchInfo(worktreePath: String): Result<String> = runCatching {
        val wtFile = File(worktreePath)
        val wtGit = Git.open(wtFile)
        val branch = wtGit.repository.branch
        wtGit.close()
        branch
    }

    fun pruneWorktrees(): Result<Int> = runCatching {
        val repoPath = context.filesDir.absolutePath
        val adminDir = File(File(repoPath, ".git"), "worktrees")
        val before = adminDir.listFiles()?.size ?: 0
        runGit(repoPath, "worktree", "prune")
        val after = adminDir.listFiles()?.size ?: 0
        (before - after).coerceAtLeast(0)
    }
}
