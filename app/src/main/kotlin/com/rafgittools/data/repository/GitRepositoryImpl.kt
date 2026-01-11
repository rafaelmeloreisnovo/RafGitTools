package com.rafgittools.data.repository

import com.rafgittools.data.git.JGitService
import com.rafgittools.data.git.ResetMode as JGitResetMode
import com.rafgittools.data.git.ReflogEntry as JGitReflogEntry
import com.rafgittools.data.git.BlameLine as JGitBlameLine
import com.rafgittools.domain.model.*
import com.rafgittools.domain.repository.Credentials
import com.rafgittools.domain.repository.GitRepository as IGitRepository
import com.rafgittools.domain.repository.ResetMode
import com.rafgittools.domain.repository.ReflogEntry
import com.rafgittools.domain.repository.BlameLine
import java.io.File
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
        return try {
            // Scan common repository locations
            val repositories = mutableListOf<GitRepository>()
            
            // Check the app's files directory for repositories
            val appReposDir = File("/storage/emulated/0/RafGitTools/repositories")
            if (appReposDir.exists() && appReposDir.isDirectory) {
                appReposDir.listFiles()?.forEach { dir ->
                    if (dir.isDirectory) {
                        val gitDir = File(dir, ".git")
                        if (gitDir.exists() && gitDir.isDirectory) {
                            // This is a valid Git repository
                            getRepository(dir.absolutePath).getOrNull()?.let { repo ->
                                repositories.add(repo)
                            }
                        }
                    }
                }
            }
            
            Result.success(repositories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getRepository(path: String): Result<GitRepository> = runCatching {
        jGitService.openRepository(path).getOrThrow().use { git ->
            val repository = git.repository
            val remotes = jGitService.getRemotes(path).getOrNull()
            
            GitRepository(
                id = path,
                name = repository.directory.parentFile?.name ?: File(path).name,
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
    
    // ============================================
    // Advanced Clone Operations (Features 20-22)
    // ============================================
    
    override suspend fun cloneShallow(
        url: String,
        localPath: String,
        depth: Int,
        credentials: Credentials?
    ): Result<GitRepository> {
        return jGitService.cloneShallow(url, localPath, depth, credentials)
    }
    
    override suspend fun cloneSingleBranch(
        url: String,
        localPath: String,
        branch: String,
        credentials: Credentials?
    ): Result<GitRepository> {
        return jGitService.cloneSingleBranch(url, localPath, branch, credentials)
    }
    
    override suspend fun cloneWithSubmodules(
        url: String,
        localPath: String,
        credentials: Credentials?
    ): Result<GitRepository> {
        return jGitService.cloneWithSubmodules(url, localPath, credentials)
    }
    
    // ============================================
    // Stash Operations (Feature 40)
    // ============================================
    
    override suspend fun listStashes(repoPath: String): Result<List<GitStash>> {
        return jGitService.listStashes(repoPath)
    }
    
    override suspend fun stash(
        repoPath: String,
        message: String?,
        includeUntracked: Boolean
    ): Result<GitStash> {
        return jGitService.stash(repoPath, message, includeUntracked)
    }
    
    override suspend fun stashApply(
        repoPath: String,
        stashRef: String?
    ): Result<Unit> {
        return jGitService.stashApply(repoPath, stashRef)
    }
    
    override suspend fun stashPop(
        repoPath: String,
        stashRef: String?
    ): Result<Unit> {
        return jGitService.stashPop(repoPath, stashRef)
    }
    
    override suspend fun stashDrop(
        repoPath: String,
        stashIndex: Int
    ): Result<Unit> {
        return jGitService.stashDrop(repoPath, stashIndex)
    }
    
    override suspend fun stashClear(repoPath: String): Result<Unit> {
        return jGitService.stashClear(repoPath)
    }
    
    // ============================================
    // Tag Operations (Features 168-169)
    // ============================================
    
    override suspend fun listTags(repoPath: String): Result<List<GitTag>> {
        return jGitService.listTags(repoPath)
    }
    
    override suspend fun createLightweightTag(
        repoPath: String,
        tagName: String,
        commitSha: String?
    ): Result<GitTag> {
        return jGitService.createLightweightTag(repoPath, tagName, commitSha)
    }
    
    override suspend fun createAnnotatedTag(
        repoPath: String,
        tagName: String,
        message: String,
        tagger: GitAuthor?,
        commitSha: String?
    ): Result<GitTag> {
        return jGitService.createAnnotatedTag(repoPath, tagName, message, tagger, commitSha)
    }
    
    override suspend fun deleteTag(
        repoPath: String,
        tagName: String
    ): Result<Unit> {
        return jGitService.deleteTag(repoPath, tagName)
    }
    
    // ============================================
    // Diff Operations (Feature 39)
    // ============================================
    
    override suspend fun getDiff(
        repoPath: String,
        cached: Boolean
    ): Result<List<GitDiff>> {
        return jGitService.getDiff(repoPath, cached)
    }
    
    override suspend fun getDiffBetweenCommits(
        repoPath: String,
        oldCommitSha: String,
        newCommitSha: String
    ): Result<List<GitDiff>> {
        return jGitService.getDiffBetweenCommits(repoPath, oldCommitSha, newCommitSha)
    }
    
    // ============================================
    // File Browser Operations (Features 43-45)
    // ============================================
    
    override suspend fun listFiles(
        repoPath: String,
        path: String,
        ref: String?
    ): Result<List<GitFile>> {
        return jGitService.listFiles(repoPath, path, ref)
    }
    
    override suspend fun getFileContent(
        repoPath: String,
        filePath: String,
        ref: String?
    ): Result<FileContent> {
        return jGitService.getFileContent(repoPath, filePath, ref)
    }
    
    // ============================================
    // Rebase Operations (Features 163-165)
    // ============================================
    
    override suspend fun rebase(
        repoPath: String,
        upstream: String
    ): Result<Unit> {
        return jGitService.rebase(repoPath, upstream)
    }
    
    override suspend fun rebaseContinue(repoPath: String): Result<Unit> {
        return jGitService.rebaseContinue(repoPath)
    }
    
    override suspend fun rebaseAbort(repoPath: String): Result<Unit> {
        return jGitService.rebaseAbort(repoPath)
    }
    
    override suspend fun rebaseSkip(repoPath: String): Result<Unit> {
        return jGitService.rebaseSkip(repoPath)
    }
    
    // ============================================
    // Cherry-pick Operations (Features 166-167)
    // ============================================
    
    override suspend fun cherryPick(
        repoPath: String,
        commitSha: String
    ): Result<GitCommit> {
        return jGitService.cherryPick(repoPath, commitSha)
    }
    
    override suspend fun cherryPickContinue(repoPath: String): Result<Unit> {
        return jGitService.cherryPickContinue(repoPath)
    }
    
    override suspend fun cherryPickAbort(repoPath: String): Result<Unit> {
        return jGitService.cherryPickAbort(repoPath)
    }
    
    // ============================================
    // Reset/Revert Operations
    // ============================================
    
    override suspend fun reset(
        repoPath: String,
        commitSha: String,
        mode: ResetMode
    ): Result<Unit> {
        val jgitMode = when (mode) {
            ResetMode.SOFT -> JGitResetMode.SOFT
            ResetMode.MIXED -> JGitResetMode.MIXED
            ResetMode.HARD -> JGitResetMode.HARD
        }
        return jGitService.reset(repoPath, commitSha, jgitMode)
    }
    
    override suspend fun revert(
        repoPath: String,
        commitSha: String
    ): Result<GitCommit> {
        return jGitService.revert(repoPath, commitSha)
    }
    
    // ============================================
    // Clean Operations
    // ============================================
    
    override suspend fun clean(
        repoPath: String,
        dryRun: Boolean,
        directories: Boolean,
        force: Boolean
    ): Result<List<String>> {
        return jGitService.clean(repoPath, dryRun, directories, force)
    }
    
    // ============================================
    // Reflog/Blame Operations
    // ============================================
    
    override suspend fun getReflog(
        repoPath: String,
        ref: String,
        limit: Int
    ): Result<List<ReflogEntry>> {
        return jGitService.getReflog(repoPath, ref, limit).map { entries ->
            entries.map { ReflogEntry(it.index, it.oldId, it.newId, it.comment, it.who, it.timestamp) }
        }
    }
    
    override suspend fun blame(
        repoPath: String,
        filePath: String
    ): Result<List<BlameLine>> {
        return jGitService.blame(repoPath, filePath).map { lines ->
            lines.map { BlameLine(it.lineNumber, it.content, it.commitSha, it.author, it.timestamp) }
        }
    }
    
    // ============================================
    // Branch Operations
    // ============================================
    
    override suspend fun deleteBranch(
        repoPath: String,
        branchName: String,
        force: Boolean
    ): Result<Unit> {
        return jGitService.deleteBranch(repoPath, branchName, force)
    }
    
    override suspend fun renameBranch(
        repoPath: String,
        oldName: String,
        newName: String
    ): Result<GitBranch> {
        return jGitService.renameBranch(repoPath, oldName, newName)
    }
}
