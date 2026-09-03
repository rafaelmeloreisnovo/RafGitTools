package com.rafgittools.core.vcs

import android.content.Context
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import java.io.File

data class WorktreeInfo(
    val path: String,
    val branch: String,
    val commitHash: String,
    val isPrunable: Boolean = false
)

class WorktreeManager(private val context: Context) {

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
                    val commitHash = headRef?.objectId?.abbreviate(40)?.name ?: "unknown"

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
        val git = Git.open(File(repoPath))

        val wtFile = File(path)
        if (!wtFile.parentFile?.exists()!!) {
            wtFile.parentFile?.mkdirs()
        }

        val worktreeApi = git.worktreeAdd()
            .setPath(path)
            .setCheckoutBranch(branchName)

        if (commitHash != null) {
            worktreeApi.setCommitish(commitHash)
        }

        worktreeApi.call()

        val wtGit = Git.open(wtFile)
        val branch = wtGit.repository.branch
        val headRef = wtGit.repository.findRef("HEAD")
        val commitId = headRef?.objectId?.abbreviate(40)?.name ?: "unknown"

        val result = WorktreeInfo(
            path = path,
            branch = branch,
            commitHash = commitId,
            isPrunable = false
        )

        wtGit.close()
        git.close()

        result
    }

    fun deleteWorktree(path: String): Result<Unit> = runCatching {
        val repoPath = context.filesDir.absolutePath
        val git = Git.open(File(repoPath))

        git.worktreeRemove()
            .setForce(false)
            .setPath(path)
            .call()

        git.close()
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
        val git = Git.open(File(repoPath))

        val prunedCount = git.worktreeRemove()
            .setForce(true)
            .call()
            .size

        git.close()
        prunedCount
    }
}
