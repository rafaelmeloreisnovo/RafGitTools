package com.rafgittools.domain.repository

import com.rafgittools.domain.model.*
import com.rafgittools.domain.model.GitRepository as DomainGitRepository

/**
 * Repository interface for Git operations
 */
interface GitRepository {
    
    /**
     * Clone a repository from a remote URL
     */
    suspend fun cloneRepository(
        url: String,
        localPath: String,
        credentials: Credentials? = null
    ): Result<DomainGitRepository>
    
    /**
     * Get all local repositories
     */
    suspend fun getLocalRepositories(): Result<List<DomainGitRepository>>
    
    /**
     * Get repository by path
     */
    suspend fun getRepository(path: String): Result<DomainGitRepository>
    
    /**
     * Get repository status
     */
    suspend fun getStatus(repoPath: String): Result<GitStatus>
    
    /**
     * Get commit history
     */
    suspend fun getCommits(
        repoPath: String,
        branch: String? = null,
        limit: Int = 50
    ): Result<List<GitCommit>>
    
    /**
     * Get all branches
     */
    suspend fun getBranches(repoPath: String): Result<List<GitBranch>>
    
    /**
     * Create a new branch
     */
    suspend fun createBranch(
        repoPath: String,
        branchName: String,
        startPoint: String? = null
    ): Result<GitBranch>
    
    /**
     * Checkout branch
     */
    suspend fun checkoutBranch(
        repoPath: String,
        branchName: String
    ): Result<Unit>
    
    /**
     * Stage files for commit
     */
    suspend fun stageFiles(
        repoPath: String,
        files: List<String>
    ): Result<Unit>
    
    /**
     * Unstage files
     */
    suspend fun unstageFiles(
        repoPath: String,
        files: List<String>
    ): Result<Unit>
    
    /**
     * Commit changes
     */
    suspend fun commit(
        repoPath: String,
        message: String,
        author: GitAuthor? = null
    ): Result<GitCommit>
    
    /**
     * Push changes to remote
     */
    suspend fun push(
        repoPath: String,
        remote: String = "origin",
        branch: String? = null,
        credentials: Credentials? = null
    ): Result<Unit>
    

    /**
     * Force push with lease (safe force-push)
     */
    suspend fun forcePushWithLease(
        repoPath: String,
        remote: String = "origin",
        branch: String,
        expectedOldObjectId: String,
        credentials: Credentials? = null
    ): Result<Unit>

    /**
     * Pull changes from remote
     */
    suspend fun pull(
        repoPath: String,
        remote: String = "origin",
        branch: String? = null,
        credentials: Credentials? = null
    ): Result<Unit>
    
    /**
     * Fetch from remote
     */
    suspend fun fetch(
        repoPath: String,
        remote: String = "origin",
        credentials: Credentials? = null
    ): Result<Unit>
    
    /**
     * Merge branch
     */
    suspend fun merge(
        repoPath: String,
        branchName: String
    ): Result<Unit>
    
    /**
     * Get remotes
     */
    suspend fun getRemotes(repoPath: String): Result<List<GitRemote>>
    
    /**
     * Add remote
     */
    suspend fun addRemote(
        repoPath: String,
        name: String,
        url: String
    ): Result<GitRemote>
    
    // ============================================
    // Advanced Clone Operations (Features 20-22)
    // ============================================
    
    /**
     * Clone with depth (shallow clone)
     */
    suspend fun cloneShallow(
        url: String,
        localPath: String,
        depth: Int = 1,
        credentials: Credentials? = null
    ): Result<DomainGitRepository>
    
    /**
     * Clone single branch
     */
    suspend fun cloneSingleBranch(
        url: String,
        localPath: String,
        branch: String,
        credentials: Credentials? = null
    ): Result<DomainGitRepository>
    
    /**
     * Clone with submodules
     */
    suspend fun cloneWithSubmodules(
        url: String,
        localPath: String,
        credentials: Credentials? = null
    ): Result<DomainGitRepository>
    
    // ============================================
    // Stash Operations (Feature 40)
    // ============================================
    
    /**
     * List all stashes
     */
    suspend fun listStashes(repoPath: String): Result<List<GitStash>>
    
    /**
     * Create a new stash
     */
    suspend fun stash(
        repoPath: String,
        message: String? = null,
        includeUntracked: Boolean = false
    ): Result<GitStash>
    
    /**
     * Apply a stash
     */
    suspend fun stashApply(
        repoPath: String,
        stashRef: String? = null
    ): Result<Unit>
    
    /**
     * Pop a stash (apply and drop)
     */
    suspend fun stashPop(
        repoPath: String,
        stashRef: String? = null
    ): Result<Unit>
    
    /**
     * Drop a stash entry
     */
    suspend fun stashDrop(
        repoPath: String,
        stashIndex: Int = 0
    ): Result<Unit>
    
    /**
     * Clear all stashes
     */
    suspend fun stashClear(repoPath: String): Result<Unit>
    
    // ============================================
    // Tag Operations (Features 168-169)
    // ============================================
    
    /**
     * List all tags
     */
    suspend fun listTags(repoPath: String): Result<List<GitTag>>
    
    /**
     * Create a lightweight tag
     */
    suspend fun createLightweightTag(
        repoPath: String,
        tagName: String,
        commitSha: String? = null
    ): Result<GitTag>
    
    /**
     * Create an annotated tag
     */
    suspend fun createAnnotatedTag(
        repoPath: String,
        tagName: String,
        message: String,
        tagger: GitAuthor? = null,
        commitSha: String? = null
    ): Result<GitTag>
    
    /**
     * Delete a tag
     */
    suspend fun deleteTag(
        repoPath: String,
        tagName: String
    ): Result<Unit>
    
    // ============================================
    // Diff Operations (Feature 39)
    // ============================================
    
    /**
     * Get diff for working directory changes
     */
    suspend fun getDiff(
        repoPath: String,
        cached: Boolean = false
    ): Result<List<GitDiff>>
    
    /**
     * Get diff between two commits
     */
    suspend fun getDiffBetweenCommits(
        repoPath: String,
        oldCommitSha: String,
        newCommitSha: String
    ): Result<List<GitDiff>>
    
    // ============================================
    // File Browser Operations (Features 43-45)
    // ============================================
    
    /**
     * List files in a directory of the repository
     */
    suspend fun listFiles(
        repoPath: String,
        path: String = "",
        ref: String? = null
    ): Result<List<GitFile>>
    
    /**
     * Get file content from repository
     */
    suspend fun getFileContent(
        repoPath: String,
        filePath: String,
        ref: String? = null
    ): Result<FileContent>
    
    // ============================================
    // Rebase Operations (Features 163-165)
    // ============================================
    
    /**
     * Start rebase
     */
    suspend fun rebase(
        repoPath: String,
        upstream: String
    ): Result<Unit>
    
    /**
     * Continue rebase after resolving conflicts
     */
    suspend fun rebaseContinue(repoPath: String): Result<Unit>
    
    /**
     * Abort rebase
     */
    suspend fun rebaseAbort(repoPath: String): Result<Unit>
    
    /**
     * Skip current commit during rebase
     */
    suspend fun rebaseSkip(repoPath: String): Result<Unit>
    
    // ============================================
    // Cherry-pick Operations (Features 166-167)
    // ============================================
    
    /**
     * Cherry-pick a commit
     */
    suspend fun cherryPick(
        repoPath: String,
        commitSha: String
    ): Result<GitCommit>
    
    /**
     * Continue cherry-pick after resolving conflicts
     */
    suspend fun cherryPickContinue(repoPath: String): Result<Unit>
    
    /**
     * Abort cherry-pick
     */
    suspend fun cherryPickAbort(repoPath: String): Result<Unit>
    
    // ============================================
    // Reset/Revert Operations
    // ============================================
    
    /**
     * Reset to a specific commit
     */
    suspend fun reset(
        repoPath: String,
        commitSha: String,
        mode: ResetMode = ResetMode.MIXED
    ): Result<Unit>
    
    /**
     * Revert a commit
     */
    suspend fun revert(
        repoPath: String,
        commitSha: String
    ): Result<GitCommit>
    
    // ============================================
    // Clean Operations
    // ============================================
    
    /**
     * Clean untracked files
     */
    suspend fun clean(
        repoPath: String,
        dryRun: Boolean = false,
        directories: Boolean = false,
        force: Boolean = true
    ): Result<List<String>>
    
    // ============================================
    // Reflog/Blame Operations
    // ============================================
    
    /**
     * Get reflog entries
     */
    suspend fun getReflog(
        repoPath: String,
        ref: String = "HEAD",
        limit: Int = 50
    ): Result<List<ReflogEntry>>
    
    /**
     * Get blame information for a file
     */
    suspend fun blame(
        repoPath: String,
        filePath: String
    ): Result<List<BlameLine>>
    
    // ============================================
    // Branch Operations
    // ============================================
    
    /**
     * Delete branch
     */
    suspend fun deleteBranch(
        repoPath: String,
        branchName: String,
        force: Boolean = false
    ): Result<Unit>
    
    /**
     * Rename branch
     */
    suspend fun renameBranch(
        repoPath: String,
        oldName: String,
        newName: String
    ): Result<GitBranch>
}

/**
 * Reset mode enum
 */
enum class ResetMode {
    SOFT,
    MIXED,
    HARD
}

/**
 * Reflog entry model
 */
data class ReflogEntry(
    val index: Int,
    val oldId: String,
    val newId: String,
    val comment: String,
    val who: GitAuthor,
    val timestamp: Long
)

/**
 * Blame line model
 */
data class BlameLine(
    val lineNumber: Int,
    val content: String,
    val commitSha: String,
    val author: GitAuthor,
    val timestamp: Long
)

/**
 * Credentials for authentication
 */
sealed class Credentials {
    data class UsernamePassword(
        val username: String,
        val password: String
    ) : Credentials()
    
    data class Token(
        val token: String
    ) : Credentials()
    
    data class SshKey(
        val privateKeyPath: String,
        val passphrase: String? = null
    ) : Credentials()
}
