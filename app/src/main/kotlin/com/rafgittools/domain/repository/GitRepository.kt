package com.rafgittools.domain.repository

import com.rafgittools.domain.model.*

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
    ): Result<com.rafgittools.domain.model.GitRepository>
    
    /**
     * Get all local repositories
     */
    suspend fun getLocalRepositories(): Result<List<com.rafgittools.domain.model.GitRepository>>
    
    /**
     * Get repository by path
     */
    suspend fun getRepository(path: String): Result<com.rafgittools.domain.model.GitRepository>
    
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
}

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
