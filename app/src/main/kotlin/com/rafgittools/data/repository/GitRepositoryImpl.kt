package com.rafgittools.data.repository

import com.rafgittools.data.git.JGitService
import com.rafgittools.domain.model.*
import com.rafgittools.domain.repository.Credentials
import com.rafgittools.domain.repository.GitRepository as IGitRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of GitRepository interface using JGit
 */
@Singleton
class GitRepositoryImpl @Inject constructor(
    private val jGitService: JGitService
) : IGitRepository {
    
    override suspend fun cloneRepository(
        url: String,
        localPath: String,
        credentials: Credentials?
    ): Result<GitRepository> {
        return jGitService.cloneRepository(url, localPath, credentials)
    }
    
    override suspend fun getLocalRepositories(): Result<List<GitRepository>> {
        // This would typically scan a known directory for repositories
        // For now, returning empty list - can be implemented based on requirements
        return Result.success(emptyList())
    }
    
    override suspend fun getRepository(path: String): Result<GitRepository> = runCatching {
        jGitService.openRepository(path).getOrThrow().use { git ->
            val repository = git.repository
            val remotes = jGitService.getRemotes(path).getOrNull()
            
            GitRepository(
                id = path,
                name = repository.directory.parentFile.name,
                path = path,
                remoteUrl = remotes?.firstOrNull()?.url,
                currentBranch = repository.branch,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
    
    override suspend fun getStatus(repoPath: String): Result<GitStatus> {
        return jGitService.getStatus(repoPath)
    }
    
    override suspend fun getCommits(
        repoPath: String,
        branch: String?,
        limit: Int
    ): Result<List<GitCommit>> {
        return jGitService.getCommits(repoPath, branch, limit)
    }
    
    override suspend fun getBranches(repoPath: String): Result<List<GitBranch>> {
        return jGitService.getBranches(repoPath)
    }
    
    override suspend fun createBranch(
        repoPath: String,
        branchName: String,
        startPoint: String?
    ): Result<GitBranch> {
        return jGitService.createBranch(repoPath, branchName, startPoint)
    }
    
    override suspend fun checkoutBranch(
        repoPath: String,
        branchName: String
    ): Result<Unit> {
        return jGitService.checkoutBranch(repoPath, branchName)
    }
    
    override suspend fun stageFiles(
        repoPath: String,
        files: List<String>
    ): Result<Unit> {
        return jGitService.stageFiles(repoPath, files)
    }
    
    override suspend fun unstageFiles(
        repoPath: String,
        files: List<String>
    ): Result<Unit> {
        return jGitService.unstageFiles(repoPath, files)
    }
    
    override suspend fun commit(
        repoPath: String,
        message: String,
        author: GitAuthor?
    ): Result<GitCommit> {
        return jGitService.commit(repoPath, message, author)
    }
    
    override suspend fun push(
        repoPath: String,
        remote: String,
        branch: String?,
        credentials: Credentials?
    ): Result<Unit> {
        return jGitService.push(repoPath, remote, branch, credentials)
    }
    
    override suspend fun pull(
        repoPath: String,
        remote: String,
        branch: String?,
        credentials: Credentials?
    ): Result<Unit> {
        return jGitService.pull(repoPath, remote, branch, credentials)
    }
    
    override suspend fun fetch(
        repoPath: String,
        remote: String,
        credentials: Credentials?
    ): Result<Unit> {
        return jGitService.fetch(repoPath, remote, credentials)
    }
    
    override suspend fun merge(
        repoPath: String,
        branchName: String
    ): Result<Unit> {
        return jGitService.merge(repoPath, branchName)
    }
    
    override suspend fun getRemotes(repoPath: String): Result<List<GitRemote>> {
        return jGitService.getRemotes(repoPath)
    }
    
    override suspend fun addRemote(
        repoPath: String,
        name: String,
        url: String
    ): Result<GitRemote> {
        return jGitService.addRemote(repoPath, name, url)
    }
}
