package com.rafgittools.data.git

import com.rafgittools.domain.model.*
import com.rafgittools.domain.repository.Credentials
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JGit implementation for Git operations
 */
@Singleton
class JGitService @Inject constructor() {
    
    /**
     * Clone repository
     */
    suspend fun cloneRepository(
        url: String,
        localPath: String,
        credentials: Credentials?
    ): Result<GitRepository> = runCatching {
        val directory = File(localPath)
        
        val cloneCommand = Git.cloneRepository()
            .setURI(url)
            .setDirectory(directory)
        
        // Apply credentials if provided
        credentials?.let {
            when (it) {
                is Credentials.UsernamePassword -> {
                    cloneCommand.setCredentialsProvider(
                        UsernamePasswordCredentialsProvider(it.username, it.password)
                    )
                }
                is Credentials.Token -> {
                    cloneCommand.setCredentialsProvider(
                        UsernamePasswordCredentialsProvider(it.token, "")
                    )
                }
                is Credentials.SshKey -> {
                    // SSH key configuration would be implemented here
                }
            }
        }
        
        val git = cloneCommand.call()
        val repository = git.repository
        
        GitRepository(
            id = directory.absolutePath,
            name = directory.name,
            path = directory.absolutePath,
            remoteUrl = url,
            currentBranch = repository.branch,
            lastUpdated = System.currentTimeMillis()
        ).also {
            git.close()
        }
    }
    
    /**
     * Open existing repository
     */
    fun openRepository(path: String): Result<Git> = runCatching {
        val directory = File(path)
        val builder = FileRepositoryBuilder()
        val repository = builder.setGitDir(File(directory, ".git"))
            .readEnvironment()
            .findGitDir()
            .build()
        
        Git(repository)
    }
    
    /**
     * Get repository status
     */
    suspend fun getStatus(repoPath: String): Result<GitStatus> = runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val status = git.status().call()
            val branch = git.repository.branch ?: "Unknown"
            
            GitStatus(
                branch = branch,
                added = status.added.toList(),
                changed = status.changed.toList(),
                removed = status.removed.toList(),
                modified = status.modified.toList(),
                untracked = status.untracked.toList(),
                conflicting = status.conflicting.toList(),
                hasUncommittedChanges = !status.isClean
            )
        }
    }
    
    /**
     * Get commit history
     */
    suspend fun getCommits(
        repoPath: String,
        branch: String?,
        limit: Int
    ): Result<List<GitCommit>> = runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val ref = branch?.let { git.repository.findRef(it) }
                ?: git.repository.findRef(Constants.HEAD)
            
            val commits = git.log()
                .add(ref?.objectId ?: throw IllegalStateException("No HEAD found"))
                .setMaxCount(limit)
                .call()
            
            commits.map { it.toGitCommit() }
        }
    }
    
    /**
     * Get branches
     */
    suspend fun getBranches(repoPath: String): Result<List<GitBranch>> = runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val currentBranch = git.repository.branch
            val branches = mutableListOf<GitBranch>()
            
            // Local branches
            git.branchList().call().forEach { ref ->
                branches.add(ref.toGitBranch(currentBranch, true, false))
            }
            
            // Remote branches
            git.branchList()
                .setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE)
                .call()
                .forEach { ref ->
                    branches.add(ref.toGitBranch(currentBranch, false, true))
                }
            
            branches
        }
    }
    
    /**
     * Create branch
     */
    suspend fun createBranch(
        repoPath: String,
        branchName: String,
        startPoint: String?
    ): Result<GitBranch> = runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val command = git.branchCreate()
                .setName(branchName)
            
            startPoint?.let { command.setStartPoint(it) }
            
            val ref = command.call()
            ref.toGitBranch(git.repository.branch, true, false)
        }
    }
    
    /**
     * Checkout branch
     */
    suspend fun checkoutBranch(
        repoPath: String,
        branchName: String
    ): Result<Unit> = runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            git.checkout()
                .setName(branchName)
                .call()
            Unit
        }
    }
    
    /**
     * Stage files
     */
    suspend fun stageFiles(
        repoPath: String,
        files: List<String>
    ): Result<Unit> = runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val addCommand = git.add()
            files.forEach { addCommand.addFilepattern(it) }
            addCommand.call()
            Unit
        }
    }
    
    /**
     * Unstage files
     */
    suspend fun unstageFiles(
        repoPath: String,
        files: List<String>
    ): Result<Unit> = runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val resetCommand = git.reset()
            files.forEach { resetCommand.addPath(it) }
            resetCommand.call()
            Unit
        }
    }
    
    /**
     * Commit changes
     */
    suspend fun commit(
        repoPath: String,
        message: String,
        author: GitAuthor?
    ): Result<GitCommit> = runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val command = git.commit()
                .setMessage(message)
            
            author?.let {
                command.setAuthor(PersonIdent(it.name, it.email))
            }
            
            val revCommit = command.call()
            revCommit.toGitCommit()
        }
    }
    
    /**
     * Push to remote
     */
    suspend fun push(
        repoPath: String,
        remote: String,
        branch: String?,
        credentials: Credentials?
    ): Result<Unit> = runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val command = git.push()
                .setRemote(remote)
            
            branch?.let { command.add(it) }
            
            credentials?.let {
                when (it) {
                    is Credentials.UsernamePassword -> {
                        command.setCredentialsProvider(
                            UsernamePasswordCredentialsProvider(it.username, it.password)
                        )
                    }
                    is Credentials.Token -> {
                        command.setCredentialsProvider(
                            UsernamePasswordCredentialsProvider(it.token, "")
                        )
                    }
                    is Credentials.SshKey -> {
                        // SSH key configuration
                    }
                }
            }
            
            command.call()
            Unit
        }
    }
    
    /**
     * Pull from remote
     */
    suspend fun pull(
        repoPath: String,
        remote: String,
        branch: String?,
        credentials: Credentials?
    ): Result<Unit> = runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val command = git.pull()
                .setRemote(remote)
            
            branch?.let { command.setRemoteBranchName(it) }
            
            credentials?.let {
                when (it) {
                    is Credentials.UsernamePassword -> {
                        command.setCredentialsProvider(
                            UsernamePasswordCredentialsProvider(it.username, it.password)
                        )
                    }
                    is Credentials.Token -> {
                        command.setCredentialsProvider(
                            UsernamePasswordCredentialsProvider(it.token, "")
                        )
                    }
                    is Credentials.SshKey -> {
                        // SSH key configuration
                    }
                }
            }
            
            command.call()
            Unit
        }
    }
    
    /**
     * Fetch from remote
     */
    suspend fun fetch(
        repoPath: String,
        remote: String,
        credentials: Credentials?
    ): Result<Unit> = runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val command = git.fetch()
                .setRemote(remote)
            
            credentials?.let {
                when (it) {
                    is Credentials.UsernamePassword -> {
                        command.setCredentialsProvider(
                            UsernamePasswordCredentialsProvider(it.username, it.password)
                        )
                    }
                    is Credentials.Token -> {
                        command.setCredentialsProvider(
                            UsernamePasswordCredentialsProvider(it.token, "")
                        )
                    }
                    is Credentials.SshKey -> {
                        // SSH key configuration
                    }
                }
            }
            
            command.call()
            Unit
        }
    }
    
    /**
     * Merge branch
     */
    suspend fun merge(
        repoPath: String,
        branchName: String
    ): Result<Unit> = runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val ref = git.repository.findRef(branchName)
                ?: throw IllegalArgumentException("Branch not found: $branchName")
            
            git.merge()
                .include(ref)
                .call()
            Unit
        }
    }
    
    /**
     * Get remotes
     */
    suspend fun getRemotes(repoPath: String): Result<List<GitRemote>> = runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            git.remoteList().call().map { remote ->
                GitRemote(
                    name = remote.name,
                    url = remote.urIs.firstOrNull()?.toString() ?: "",
                    fetchUrl = remote.urIs.firstOrNull()?.toString() ?: "",
                    pushUrl = remote.pushURIs.firstOrNull()?.toString()
                        ?: remote.urIs.firstOrNull()?.toString() ?: ""
                )
            }
        }
    }
    
    /**
     * Add remote
     */
    suspend fun addRemote(
        repoPath: String,
        name: String,
        url: String
    ): Result<GitRemote> = runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val config = git.repository.config
            config.setString("remote", name, "url", url)
            config.setString("remote", name, "fetch", "+refs/heads/*:refs/remotes/$name/*")
            config.save()
            
            GitRemote(
                name = name,
                url = url,
                fetchUrl = url,
                pushUrl = url
            )
        }
    }
    
    // Extension functions
    private fun RevCommit.toGitCommit() = GitCommit(
        sha = name,
        message = fullMessage,
        author = GitAuthor(
            name = authorIdent.name,
            email = authorIdent.emailAddress
        ),
        committer = GitAuthor(
            name = committerIdent.name,
            email = committerIdent.emailAddress
        ),
        timestamp = commitTime.toLong() * 1000,
        parents = parents.map { it.name }
    )
    
    private fun Ref.toGitBranch(
        currentBranch: String?,
        isLocal: Boolean,
        isRemote: Boolean
    ) = GitBranch(
        name = name,
        shortName = org.eclipse.jgit.lib.Repository.shortenRefName(name),
        isLocal = isLocal,
        isRemote = isRemote,
        isCurrent = org.eclipse.jgit.lib.Repository.shortenRefName(name) == currentBranch,
        commitSha = objectId?.name
    )
}
