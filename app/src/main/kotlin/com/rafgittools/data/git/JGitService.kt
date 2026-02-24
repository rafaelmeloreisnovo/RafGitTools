package com.rafgittools.data.git

import com.rafgittools.core.logging.DiffAuditEntry
import com.rafgittools.core.logging.DiffAuditLogger
import com.rafgittools.core.logging.md5Hex
import com.rafgittools.core.security.SshSessionFactory
import com.rafgittools.domain.model.*
import com.rafgittools.domain.repository.Credentials
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.transport.RefLeaseSpec
import org.eclipse.jgit.transport.SshSessionFactory as JGitSshSessionFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JGit implementation for Git operations
 * 
 * Supports multiple authentication methods:
 * - Token authentication (recommended for GitHub)
 * - Username/password authentication
 * - SSH key authentication (Ed25519, RSA, ECDSA)
 */
@Singleton
class JGitService @Inject constructor(
    private val diffAuditLogger: DiffAuditLogger
) {
    
    companion object {
        // SSH authentication is now implemented
    }
    
    /**
     * Clone repository
     */
    suspend fun cloneRepository(
        url: String,
        localPath: String,
        credentials: Credentials?
    ): Result<GitRepository> = withContext(Dispatchers.IO) {
        runCatching {
        val directory = File(localPath)
        validateCloneTarget(directory)
        
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
                    // SSH key authentication - Feature #64, #65, #66
                    val sshSessionFactory = if (it.passphrase != null) {
                        SshSessionFactory.createWithPassphrase(it.privateKeyPath, it.passphrase)
                    } else {
                        SshSessionFactory.create(it.privateKeyPath)
                    }
                    cloneCommand.setTransportConfigCallback(createSshTransportCallback(sshSessionFactory))
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
    }
    
    /**
     * Create SSH transport callback for JGit commands
     */
    private fun createSshTransportCallback(sshSessionFactory: SshSessionFactory): TransportConfigCallback {
        return TransportConfigCallback { transport: Transport ->
            if (transport is SshTransport) {
                transport.sshSessionFactory = sshSessionFactory as JGitSshSessionFactory
            }
        }
    }
    
    /**
     * Clone repository with depth (shallow clone)
     * Feature #20 from roadmap
     */
    suspend fun cloneShallow(
        url: String,
        localPath: String,
        depth: Int = 1,
        credentials: Credentials?
    ): Result<GitRepository> = withContext(Dispatchers.IO) {
        runCatching {
        val directory = File(localPath)
        validateCloneTarget(directory)
        
        val cloneCommand = Git.cloneRepository()
            .setURI(url)
            .setDirectory(directory)
            .setDepth(depth)
        
        applyCredentials(cloneCommand, credentials)
        
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
    }
    
    /**
     * Clone single branch only
     * Feature #21 from roadmap
     */
    suspend fun cloneSingleBranch(
        url: String,
        localPath: String,
        branch: String,
        credentials: Credentials?
    ): Result<GitRepository> = withContext(Dispatchers.IO) {
        runCatching {
        val directory = File(localPath)
        validateCloneTarget(directory)
        
        val cloneCommand = Git.cloneRepository()
            .setURI(url)
            .setDirectory(directory)
            .setBranch(branch)
            .setCloneAllBranches(false)
            .setBranchesToClone(listOf("refs/heads/$branch"))
        
        applyCredentials(cloneCommand, credentials)
        
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
    }
    
    /**
     * Clone with submodules
     * Feature #22 from roadmap
     */
    suspend fun cloneWithSubmodules(
        url: String,
        localPath: String,
        credentials: Credentials?
    ): Result<GitRepository> = withContext(Dispatchers.IO) {
        runCatching {
        val directory = File(localPath)
        validateCloneTarget(directory)
        
        val cloneCommand = Git.cloneRepository()
            .setURI(url)
            .setDirectory(directory)
            .setCloneSubmodules(true)
        
        applyCredentials(cloneCommand, credentials)
        
        val git = cloneCommand.call()
        val repository = git.repository
        
        // Initialize and update submodules
        git.submoduleInit().call()
        git.submoduleUpdate().call()
        
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
    }
    
    /**
     * Helper function to apply credentials to clone command
     */
    private fun applyCredentials(
        cloneCommand: org.eclipse.jgit.api.CloneCommand,
        credentials: Credentials?
    ) {
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
                    // SSH key authentication - Feature #64, #65, #66
                    val sshSessionFactory = if (it.passphrase != null) {
                        SshSessionFactory.createWithPassphrase(it.privateKeyPath, it.passphrase)
                    } else {
                        SshSessionFactory.create(it.privateKeyPath)
                    }
                    cloneCommand.setTransportConfigCallback(createSshTransportCallback(sshSessionFactory))
                }
            }
        }
    }

    private fun validateCloneTarget(directory: File) {
        val parent = directory.parentFile
        if (parent != null && (!parent.exists() || !parent.canWrite())) {
            throw SecurityException("Permission denied for ${parent.absolutePath}")
        }
        if (directory.exists() && !directory.canWrite()) {
            throw SecurityException("Permission denied for ${directory.absolutePath}")
        }
        if (!directory.exists() && parent != null) {
            parent.mkdirs()
        }
    }
    
    /**
     * Open existing repository
     */
    suspend fun openRepository(path: String): Result<Git> = withContext(Dispatchers.IO) {
        runCatching {
        val directory = File(path)
        val builder = FileRepositoryBuilder()
        val repository = builder.setGitDir(File(directory, ".git"))
            .readEnvironment()
            .findGitDir()
            .build()
        
        Git(repository)
    }
    }
    
    /**
     * Get repository status
     */
    suspend fun getStatus(repoPath: String): Result<GitStatus> = withContext(Dispatchers.IO) {
        runCatching {
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
    }
    
    /**
     * Get commit history
     */
    suspend fun getCommits(
        repoPath: String,
        branch: String?,
        limit: Int
    ): Result<List<GitCommit>> = withContext(Dispatchers.IO) {
        runCatching {
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
    }
    
    /**
     * Get branches
     */
    suspend fun getBranches(repoPath: String): Result<List<GitBranch>> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val currentBranch = git.repository.branch
            val branches = mutableListOf<GitBranch>()
            
            // Local branches
            git.branchList().call().forEach { ref ->
                branches.add(ref.toGitBranch(
                    currentBranch = currentBranch,
                    isLocal = true,
                    isRemote = false
                ))
            }
            
            // Remote branches
            git.branchList()
                .setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE)
                .call()
                .forEach { ref ->
                    branches.add(ref.toGitBranch(
                        currentBranch = currentBranch,
                        isLocal = false,
                        isRemote = true
                    ))
                }
            
            branches
        }
    }
    }
    
    /**
     * Create branch
     */
    suspend fun createBranch(
        repoPath: String,
        branchName: String,
        startPoint: String?
    ): Result<GitBranch> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val command = git.branchCreate()
                .setName(branchName)
            
            startPoint?.let { command.setStartPoint(it) }
            
            val ref = command.call()
            ref.toGitBranch(
                currentBranch = git.repository.branch,
                isLocal = true,
                isRemote = false
            )
        }
    }
    }
    
    /**
     * Delete branch
     * Feature #32 from roadmap
     */
    suspend fun deleteBranch(
        repoPath: String,
        branchName: String,
        force: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            git.branchDelete()
                .setBranchNames(branchName)
                .setForce(force)
                .call()
            Unit
        }
    }
    }
    
    /**
     * Rename branch
     * Feature #33 from roadmap
     */
    suspend fun renameBranch(
        repoPath: String,
        oldName: String,
        newName: String
    ): Result<GitBranch> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val ref = git.branchRename()
                .setOldName(oldName)
                .setNewName(newName)
                .call()
            ref.toGitBranch(
                currentBranch = git.repository.branch,
                isLocal = true,
                isRemote = false
            )
        }
    }
    }
    
    /**
     * Checkout branch
     */
    suspend fun checkoutBranch(
        repoPath: String,
        branchName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            git.checkout()
                .setName(branchName)
                .call()
            Unit
        }
    }
    }
    
    /**
     * Stage files
     */
    suspend fun stageFiles(
        repoPath: String,
        files: List<String>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val addCommand = git.add()
            files.forEach { addCommand.addFilepattern(it) }
            addCommand.call()
            Unit
        }
    }
    }
    
    /**
     * Unstage files
     */
    suspend fun unstageFiles(
        repoPath: String,
        files: List<String>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val resetCommand = git.reset()
            files.forEach { resetCommand.addPath(it) }
            resetCommand.call()
            Unit
        }
    }
    }
    
    /**
     * Commit changes
     */
    suspend fun commit(
        repoPath: String,
        message: String,
        author: GitAuthor?
    ): Result<GitCommit> = withContext(Dispatchers.IO) {
        runCatching {
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
    }
    
    /**
     * Push to remote
     */
    suspend fun push(
        repoPath: String,
        remote: String,
        branch: String?,
        credentials: Credentials?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
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
                        // SSH key authentication - Feature #64, #65, #66
                        val sshSessionFactory = if (it.passphrase != null) {
                            SshSessionFactory.createWithPassphrase(it.privateKeyPath, it.passphrase)
                        } else {
                            SshSessionFactory.create(it.privateKeyPath)
                        }
                        command.setTransportConfigCallback(createSshTransportCallback(sshSessionFactory))
                    }
                }
            }
            
            command.call()
            Unit
        }
    }
    }

    /**
     * Force push with lease (safe force-push)
     * Feature #29 from roadmap (P33-06)
     */
    suspend fun forcePushWithLease(
        repoPath: String,
        remote: String,
        branch: String,
        expectedOldObjectId: String,
        credentials: Credentials?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val refSpec = "refs/heads/$branch:refs/heads/$branch"
            val command = git.push()
                .setRemote(remote)
                .setForce(true)
                .setRefSpecs(org.eclipse.jgit.transport.RefSpec(refSpec))
                .setRefLeaseSpecs(RefLeaseSpec(refSpec, expectedOldObjectId))

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
                        val sshSessionFactory = if (it.passphrase != null) {
                            SshSessionFactory.createWithPassphrase(it.privateKeyPath, it.passphrase)
                        } else {
                            SshSessionFactory.create(it.privateKeyPath)
                        }
                        command.setTransportConfigCallback(createSshTransportCallback(sshSessionFactory))
                    }
                }
            }

            command.call()
            Unit
        }
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
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
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
                        // SSH key authentication - Feature #64, #65, #66
                        val sshSessionFactory = if (it.passphrase != null) {
                            SshSessionFactory.createWithPassphrase(it.privateKeyPath, it.passphrase)
                        } else {
                            SshSessionFactory.create(it.privateKeyPath)
                        }
                        command.setTransportConfigCallback(createSshTransportCallback(sshSessionFactory))
                    }
                }
            }
            
            command.call()
            Unit
        }
    }
    }
    
    /**
     * Fetch from remote
     */
    suspend fun fetch(
        repoPath: String,
        remote: String,
        credentials: Credentials?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
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
                        // SSH key authentication - Feature #64, #65, #66
                        val sshSessionFactory = if (it.passphrase != null) {
                            SshSessionFactory.createWithPassphrase(it.privateKeyPath, it.passphrase)
                        } else {
                            SshSessionFactory.create(it.privateKeyPath)
                        }
                        command.setTransportConfigCallback(createSshTransportCallback(sshSessionFactory))
                    }
                }
            }
            
            command.call()
            Unit
        }
    }
    }
    
    /**
     * Merge branch
     */
    suspend fun merge(
        repoPath: String,
        branchName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val ref = git.repository.findRef(branchName)
                ?: throw IllegalArgumentException("Branch not found: $branchName")
            
            git.merge()
                .include(ref)
                .call()
            Unit
        }
    }
    }
    
    /**
     * Get remotes
     */
    suspend fun getRemotes(repoPath: String): Result<List<GitRemote>> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            git.remoteList().call().map { remote ->
                val uris = remote.urIs
                val pushUris = remote.pushURIs
                GitRemote(
                    name = remote.name,
                    url = uris.firstOrNull()?.toString() ?: "",
                    fetchUrl = uris.firstOrNull()?.toString() ?: "",
                    pushUrl = pushUris.firstOrNull()?.toString()
                        ?: uris.firstOrNull()?.toString() ?: ""
                )
            }
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
    ): Result<GitRemote> = withContext(Dispatchers.IO) {
        runCatching {
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
    
    // ============================================
    // Stash Operations
    // ============================================
    
    /**
     * List all stash entries
     */
    suspend fun listStashes(repoPath: String): Result<List<GitStash>> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val stashList = git.stashList().call()
            stashList.mapIndexed { index, revCommit ->
                GitStash(
                    index = index,
                    message = revCommit.shortMessage,
                    sha = revCommit.name,
                    branch = revCommit.fullMessage.substringAfter("WIP on ").substringBefore(":"),
                    timestamp = revCommit.commitTime.toLong() * 1000
                )
            }
        }
    }
    }
    
    /**
     * Create a new stash
     */
    suspend fun stash(
        repoPath: String,
        message: String?,
        includeUntracked: Boolean = false
    ): Result<GitStash> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val command = git.stashCreate()
            if (includeUntracked) {
                command.setIncludeUntracked(true)
            }
            
            val revCommit = command.call()
                ?: throw IllegalStateException("No local changes to stash")
            
            GitStash(
                index = 0,
                message = message ?: revCommit.shortMessage,
                sha = revCommit.name,
                branch = git.repository.branch,
                timestamp = System.currentTimeMillis()
            )
        }
    }
    }
    
    /**
     * Apply a stash
     */
    suspend fun stashApply(
        repoPath: String,
        stashRef: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val command = git.stashApply()
            stashRef?.let { command.setStashRef(it) }
            command.call()
            Unit
        }
    }
    }
    
    /**
     * Pop a stash (apply and drop)
     */
    suspend fun stashPop(
        repoPath: String,
        stashRef: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            // First apply
            val applyCommand = git.stashApply()
            stashRef?.let { applyCommand.setStashRef(it) }
            applyCommand.call()
            
            // Then drop
            git.stashDrop().call()
            Unit
        }
    }
    }
    
    /**
     * Drop a stash entry
     */
    suspend fun stashDrop(
        repoPath: String,
        stashIndex: Int = 0
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            git.stashDrop()
                .setStashRef(stashIndex)
                .call()
            Unit
        }
    }
    }
    
    /**
     * Clear all stashes
     */
    suspend fun stashClear(repoPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            // Drop all stashes one by one from index 0 (stack top)
            val stashCount = git.stashList().call().size
            repeat(stashCount) {
                git.stashDrop().setStashRef(0).call()
            }
            Unit
        }
    }
    }
    
    // ============================================
    // Tag Operations
    // ============================================
    
    /**
     * List all tags
     */
    suspend fun listTags(repoPath: String): Result<List<GitTag>> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val tags = mutableListOf<GitTag>()
            
            git.tagList().call().forEach { ref ->
                val tagName = org.eclipse.jgit.lib.Repository.shortenRefName(ref.name)
                val objectId = ref.peeledObjectId ?: ref.objectId
                
                // Try to get tag object for annotated tags
                try {
                    org.eclipse.jgit.revwalk.RevWalk(git.repository).use { revWalk ->
                        val obj = revWalk.parseAny(ref.objectId)

                        if (obj is org.eclipse.jgit.revwalk.RevTag) {
                            // Annotated tag
                            tags.add(GitTag(
                                name = tagName,
                                sha = objectId?.name ?: "",
                                message = obj.fullMessage,
                                tagger = obj.taggerIdent?.let {
                                    GitAuthor(it.name, it.emailAddress)
                                },
                                timestamp = obj.taggerIdent?.whenAsInstant?.toEpochMilli(),
                                isAnnotated = true
                            ))
                        } else {
                            // Lightweight tag
                            tags.add(GitTag(
                                name = tagName,
                                sha = objectId?.name ?: "",
                                message = null,
                                tagger = null,
                                timestamp = null,
                                isAnnotated = false
                            ))
                        }
                    }
                } catch (e: Exception) {
                    // Fallback for lightweight tags
                    tags.add(GitTag(
                        name = tagName,
                        sha = objectId?.name ?: "",
                        message = null,
                        tagger = null,
                        timestamp = null,
                        isAnnotated = false
                    ))
                }
            }
            
            tags
        }
    }
    }
    
    /**
     * Create a lightweight tag
     */
    suspend fun createLightweightTag(
        repoPath: String,
        tagName: String,
        commitSha: String? = null
    ): Result<GitTag> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val command = git.tag()
                .setName(tagName)
                .setAnnotated(false)
            
            commitSha?.let { 
                val objectId = git.repository.resolve(it)
                org.eclipse.jgit.revwalk.RevWalk(git.repository).use { revWalk ->
                    command.setObjectId(revWalk.parseCommit(objectId))
                }
            }
            
            val ref = command.call()
            
            GitTag(
                name = tagName,
                sha = ref.objectId?.name ?: "",
                message = null,
                tagger = null,
                timestamp = null,
                isAnnotated = false
            )
        }
    }
    }
    
    /**
     * Create an annotated tag
     */
    suspend fun createAnnotatedTag(
        repoPath: String,
        tagName: String,
        message: String,
        tagger: GitAuthor? = null,
        commitSha: String? = null
    ): Result<GitTag> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val command = git.tag()
                .setName(tagName)
                .setMessage(message)
                .setAnnotated(true)
            
            tagger?.let {
                command.setTagger(PersonIdent(it.name, it.email))
            }
            
            commitSha?.let { 
                val objectId = git.repository.resolve(it)
                org.eclipse.jgit.revwalk.RevWalk(git.repository).use { revWalk ->
                    command.setObjectId(revWalk.parseCommit(objectId))
                }
            }
            
            val ref = command.call()
            
            GitTag(
                name = tagName,
                sha = ref.objectId?.name ?: "",
                message = message,
                tagger = tagger,
                timestamp = System.currentTimeMillis(),
                isAnnotated = true
            )
        }
    }
    }
    
    /**
     * Delete a tag
     */
    suspend fun deleteTag(
        repoPath: String,
        tagName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            git.tagDelete()
                .setTags(tagName)
                .call()
            Unit
        }
    }
    }
    
    // ============================================
    // Diff Operations
    // ============================================
    
    /**
     * Get diff for working directory changes
     */
    suspend fun getDiff(
        repoPath: String,
        cached: Boolean = false
    ): Result<List<GitDiff>> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val diffs = if (cached) {
                git.diff()
                    .setCached(true)
                    .call()
            } else {
                git.diff()
                    .setCached(false)
                    .call()
            }
            
            diffs.map { diffEntry ->
                val changeType = when (diffEntry.changeType) {
                    org.eclipse.jgit.diff.DiffEntry.ChangeType.ADD -> DiffChangeType.ADD
                    org.eclipse.jgit.diff.DiffEntry.ChangeType.MODIFY -> DiffChangeType.MODIFY
                    org.eclipse.jgit.diff.DiffEntry.ChangeType.DELETE -> DiffChangeType.DELETE
                    org.eclipse.jgit.diff.DiffEntry.ChangeType.RENAME -> DiffChangeType.RENAME
                    org.eclipse.jgit.diff.DiffEntry.ChangeType.COPY -> DiffChangeType.COPY
                }
                
                // Get the raw diff content
                val diffContent = java.io.ByteArrayOutputStream().use { outputStream ->
                    org.eclipse.jgit.diff.DiffFormatter(outputStream).use { formatter ->
                        formatter.setRepository(git.repository)
                        formatter.format(diffEntry)
                        formatter.flush()
                    }
                    outputStream.toString("UTF-8")
                }
                val hunks = parseDiffHunks(diffContent)
                logDiffAudit(repoPath, diffEntry, diffContent)
                
                GitDiff(
                    oldPath = if (diffEntry.oldPath != "/dev/null") diffEntry.oldPath else null,
                    newPath = if (diffEntry.newPath != "/dev/null") diffEntry.newPath else null,
                    changeType = changeType,
                    oldContent = null, // Would need to load from repository
                    newContent = null,
                    hunks = hunks
                )
            }
        }
    }
    }
    
    /**
     * Get diff between two commits
     */
    suspend fun getDiffBetweenCommits(
        repoPath: String,
        oldCommitSha: String,
        newCommitSha: String
    ): Result<List<GitDiff>> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val diffs = git.repository.newObjectReader().use { reader ->
                val oldTree = org.eclipse.jgit.treewalk.CanonicalTreeParser().apply {
                    val treeId = git.repository.resolve("$oldCommitSha^{tree}")
                    reset(reader, treeId)
                }

                val newTree = org.eclipse.jgit.treewalk.CanonicalTreeParser().apply {
                    val treeId = git.repository.resolve("$newCommitSha^{tree}")
                    reset(reader, treeId)
                }

                git.diff()
                    .setOldTree(oldTree)
                    .setNewTree(newTree)
                    .call()
            }
            
            diffs.map { diffEntry ->
                val changeType = when (diffEntry.changeType) {
                    org.eclipse.jgit.diff.DiffEntry.ChangeType.ADD -> DiffChangeType.ADD
                    org.eclipse.jgit.diff.DiffEntry.ChangeType.MODIFY -> DiffChangeType.MODIFY
                    org.eclipse.jgit.diff.DiffEntry.ChangeType.DELETE -> DiffChangeType.DELETE
                    org.eclipse.jgit.diff.DiffEntry.ChangeType.RENAME -> DiffChangeType.RENAME
                    org.eclipse.jgit.diff.DiffEntry.ChangeType.COPY -> DiffChangeType.COPY
                }
                
                val diffContent = java.io.ByteArrayOutputStream().use { outputStream ->
                    org.eclipse.jgit.diff.DiffFormatter(outputStream).use { formatter ->
                        formatter.setRepository(git.repository)
                        formatter.format(diffEntry)
                        formatter.flush()
                    }
                    outputStream.toString("UTF-8")
                }
                val hunks = parseDiffHunks(diffContent)
                logDiffAudit(repoPath, diffEntry, diffContent)
                
                GitDiff(
                    oldPath = if (diffEntry.oldPath != "/dev/null") diffEntry.oldPath else null,
                    newPath = if (diffEntry.newPath != "/dev/null") diffEntry.newPath else null,
                    changeType = changeType,
                    oldContent = null,
                    newContent = null,
                    hunks = hunks
                )
            }
        }
    }
    }
    
    /**
     * Parse diff content into hunks
     */
    private fun parseDiffHunks(diffContent: String): List<DiffHunk> {
        val hunks = mutableListOf<DiffHunk>()
        val lines = diffContent.lines()
        
        var currentHunk: MutableList<DiffLine>? = null
        var oldStart = 0
        var oldLines = 0
        var newStart = 0
        var newLines = 0
        var oldLineNum = 0
        var newLineNum = 0
        
        for (line in lines) {
            when {
                line.startsWith("@@") -> {
                    // Save previous hunk
                    currentHunk?.let {
                        hunks.add(DiffHunk(oldStart, oldLines, newStart, newLines, it))
                    }
                    
                    // Parse hunk header: @@ -start,lines +start,lines @@
                    val regex = Regex("@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@")
                    regex.find(line)?.let { match ->
                        oldStart = match.groupValues[1].toIntOrNull() ?: 0
                        oldLines = match.groupValues[2].toIntOrNull() ?: 1
                        newStart = match.groupValues[3].toIntOrNull() ?: 0
                        newLines = match.groupValues[4].toIntOrNull() ?: 1
                    }
                    
                    oldLineNum = oldStart
                    newLineNum = newStart
                    currentHunk = mutableListOf()
                }
                line.startsWith("+") && !line.startsWith("+++") -> {
                    currentHunk?.add(DiffLine(
                        type = DiffLineType.ADD,
                        content = line.substring(1),
                        oldLineNumber = null,
                        newLineNumber = newLineNum++
                    ))
                }
                line.startsWith("-") && !line.startsWith("---") -> {
                    currentHunk?.add(DiffLine(
                        type = DiffLineType.DELETE,
                        content = line.substring(1),
                        oldLineNumber = oldLineNum++,
                        newLineNumber = null
                    ))
                }
                line.startsWith(" ") -> {
                    currentHunk?.add(DiffLine(
                        type = DiffLineType.CONTEXT,
                        content = line.substring(1),
                        oldLineNumber = oldLineNum++,
                        newLineNumber = newLineNum++
                    ))
                }
            }
        }
        
        // Add last hunk
        currentHunk?.let {
            hunks.add(DiffHunk(oldStart, oldLines, newStart, newLines, it))
        }
        
        return hunks
    }

    private fun logDiffAudit(
        repoPath: String,
        diffEntry: org.eclipse.jgit.diff.DiffEntry,
        diffContent: String
    ) {
        val oldPath = diffEntry.oldPath.takeUnless { it == "/dev/null" }
        val newPath = diffEntry.newPath.takeUnless { it == "/dev/null" }
        val candidatePath = newPath ?: oldPath
        val file = candidatePath?.let { java.io.File(repoPath, it) }
        val fileBytes = if (file != null && file.exists() && file.isFile) {
            file.readBytes()
        } else {
            ByteArray(0)
        }
        val entry = DiffAuditEntry(
            oldPath = oldPath,
            newPath = newPath,
            changeType = diffEntry.changeType.name,
            timestamp = System.currentTimeMillis(),
            diffSizeBytes = diffContent.toByteArray(Charsets.UTF_8).size.toLong(),
            fileSizeBytes = fileBytes.size.toLong(),
            md5 = if (fileBytes.isNotEmpty()) md5Hex(fileBytes) else ""
        )
        diffAuditLogger.logDiff(entry)
    }
    
    // ============================================
    // File Browser Operations
    // ============================================
    
    /**
     * List files in a directory of the repository
     */
    suspend fun listFiles(
        repoPath: String,
        path: String = "",
        ref: String? = null
    ): Result<List<GitFile>> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val files = mutableListOf<GitFile>()
            val resolvedRef = ref ?: Constants.HEAD
            
            org.eclipse.jgit.revwalk.RevWalk(git.repository).use { revWalk ->
                val commitId = git.repository.resolve(resolvedRef)
                val commit = revWalk.parseCommit(commitId)
                val tree = commit.tree

                org.eclipse.jgit.treewalk.TreeWalk(git.repository).use { treeWalk ->
                    treeWalk.addTree(tree)
                    treeWalk.isRecursive = false

                    if (path.isNotEmpty()) {
                        treeWalk.filter = org.eclipse.jgit.treewalk.filter.PathFilter.create(path)
                        // Need to enter the directory
                        while (treeWalk.next()) {
                            if (treeWalk.pathString == path && treeWalk.isSubtree) {
                                treeWalk.enterSubtree()
                                break
                            }
                        }
                    }

                    while (treeWalk.next()) {
                        val filePath = treeWalk.pathString
                        val fileName = filePath.substringAfterLast("/")

                        // Only include direct children, not nested
                        if (path.isEmpty() && filePath.contains("/")) continue
                        if (path.isNotEmpty() && filePath.removePrefix("$path/").contains("/")) continue

                        val mode = treeWalk.fileMode
                        val isDirectory = treeWalk.isSubtree

                        files.add(GitFile(
                            name = fileName,
                            path = filePath,
                            isDirectory = isDirectory,
                            size = if (!isDirectory) {
                                try {
                                    val loader = git.repository.open(treeWalk.getObjectId(0))
                                    loader.size
                                } catch (e: Exception) {
                                    0L
                                }
                            } else 0L,
                            mode = mode.toString(),
                            sha = treeWalk.getObjectId(0)?.name
                        ))
                    }
                }
            }
            
            files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        }
    }
    }
    
    /**
     * Get file content from repository
     */
    suspend fun getFileContent(
        repoPath: String,
        filePath: String,
        ref: String? = null
    ): Result<FileContent> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val resolvedRef = ref ?: Constants.HEAD
            
            val bytes = org.eclipse.jgit.revwalk.RevWalk(git.repository).use { revWalk ->
                val commitId = git.repository.resolve(resolvedRef)
                val commit = revWalk.parseCommit(commitId)
                val tree = commit.tree

                val objectId = org.eclipse.jgit.treewalk.TreeWalk.forPath(
                    git.repository,
                    filePath,
                    tree
                )?.use { treeWalk ->
                    treeWalk.getObjectId(0)
                } ?: throw IllegalArgumentException("File not found: $filePath")

                git.repository.open(objectId).bytes
            }
            
            // Detect if file is binary by checking first 512 bytes for null bytes
            // This is more efficient than checking 8000 bytes
            val isBinary = bytes.take(512).any { it.toInt() == 0 }
            
            val content = if (isBinary) {
                "[Binary file - ${bytes.size} bytes]"
            } else {
                String(bytes, Charsets.UTF_8)
            }
            
            val fileName = filePath.substringAfterLast("/")
            val extension = fileName.substringAfterLast(".", "")
            val language = detectLanguage(extension)
            
            FileContent(
                path = filePath,
                name = fileName,
                content = content,
                encoding = "UTF-8",
                size = bytes.size.toLong(),
                language = language,
                isBinary = isBinary
            )
        }
    }
    }
    
    /**
     * Detect programming language from file extension
     */
    private fun detectLanguage(extension: String): String? {
        return when (extension.lowercase()) {
            "kt", "kts" -> "Kotlin"
            "java" -> "Java"
            "py" -> "Python"
            "js" -> "JavaScript"
            "ts" -> "TypeScript"
            "jsx" -> "JavaScript (React)"
            "tsx" -> "TypeScript (React)"
            "html", "htm" -> "HTML"
            "css" -> "CSS"
            "scss", "sass" -> "SCSS"
            "xml" -> "XML"
            "json" -> "JSON"
            "yaml", "yml" -> "YAML"
            "md" -> "Markdown"
            "sh", "bash" -> "Shell"
            "c" -> "C"
            "cpp", "cc", "cxx" -> "C++"
            "h", "hpp" -> "C/C++ Header"
            "cs" -> "C#"
            "go" -> "Go"
            "rs" -> "Rust"
            "rb" -> "Ruby"
            "php" -> "PHP"
            "swift" -> "Swift"
            "sql" -> "SQL"
            "gradle" -> "Gradle"
            "properties" -> "Properties"
            "txt" -> "Plain Text"
            else -> null
        }
    }
    
    // ============================================
    // Rebase Operations
    // ============================================
    
    /**
     * Start interactive rebase
     */
    suspend fun rebase(
        repoPath: String,
        upstream: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            git.rebase()
                .setUpstream(upstream)
                .call()
            Unit
        }
    }
    }
    
    /**
     * Continue rebase after resolving conflicts
     */
    suspend fun rebaseContinue(repoPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            git.rebase()
                .setOperation(org.eclipse.jgit.api.RebaseCommand.Operation.CONTINUE)
                .call()
            Unit
        }
    }
    }
    
    /**
     * Abort rebase
     */
    suspend fun rebaseAbort(repoPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            git.rebase()
                .setOperation(org.eclipse.jgit.api.RebaseCommand.Operation.ABORT)
                .call()
            Unit
        }
    }
    }
    
    /**
     * Skip current commit during rebase
     */
    suspend fun rebaseSkip(repoPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            git.rebase()
                .setOperation(org.eclipse.jgit.api.RebaseCommand.Operation.SKIP)
                .call()
            Unit
        }
    }
    }
    
    // ============================================
    // Cherry-pick Operations
    // ============================================
    
    /**
     * Cherry-pick a commit
     */
    suspend fun cherryPick(
        repoPath: String,
        commitSha: String
    ): Result<GitCommit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val commitId = git.repository.resolve(commitSha)
            val commit = org.eclipse.jgit.revwalk.RevWalk(git.repository).use { revWalk ->
                revWalk.parseCommit(commitId)
            }
            
            val result = git.cherryPick()
                .include(commit)
                .call()
            
            result.newHead?.toGitCommit() 
                ?: throw IllegalStateException("Cherry-pick failed")
        }
    }
    }
    
    /**
     * Continue cherry-pick after resolving conflicts
     */
    suspend fun cherryPickContinue(repoPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            // In JGit, continuing cherry-pick requires committing manually
            // if there were conflicts
            git.commit()
                .setMessage("Cherry-pick continued after conflict resolution")
                .call()
            Unit
        }
    }
    }
    
    /**
     * Abort cherry-pick
     */
    suspend fun cherryPickAbort(repoPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            git.reset()
                .setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD)
                .setRef(Constants.HEAD)
                .call()
            Unit
        }
    }
    }
    
    // ============================================
    // Reset Operations  
    // ============================================
    
    /**
     * Reset to a specific commit
     */
    suspend fun reset(
        repoPath: String,
        commitSha: String,
        mode: ResetMode = ResetMode.MIXED
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val resetType = when (mode) {
                ResetMode.SOFT -> org.eclipse.jgit.api.ResetCommand.ResetType.SOFT
                ResetMode.MIXED -> org.eclipse.jgit.api.ResetCommand.ResetType.MIXED
                ResetMode.HARD -> org.eclipse.jgit.api.ResetCommand.ResetType.HARD
            }
            
            git.reset()
                .setRef(commitSha)
                .setMode(resetType)
                .call()
            Unit
        }
    }
    }
    
    /**
     * Revert a commit
     */
    suspend fun revert(
        repoPath: String,
        commitSha: String
    ): Result<GitCommit> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val commitId = git.repository.resolve(commitSha)
            val commit = org.eclipse.jgit.revwalk.RevWalk(git.repository).use { revWalk ->
                revWalk.parseCommit(commitId)
            }
            
            val result = git.revert()
                .include(commit)
                .call()
            
            result.toGitCommit()
        }
    }
    }
    
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
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            git.clean()
                .setDryRun(dryRun)
                .setCleanDirectories(directories)
                .setForce(force)
                .call()
                .toList()
        }
    }
    }
    
    // ============================================
    // Log/Reflog Operations
    // ============================================
    
    /**
     * Get reflog entries
     */
    suspend fun getReflog(
        repoPath: String,
        ref: String = Constants.HEAD,
        limit: Int = 50
    ): Result<List<ReflogEntry>> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            git.reflog()
                .setRef(ref)
                .call()
                .take(limit)
                .mapIndexed { index, entry ->
                    ReflogEntry(
                        index = index,
                        oldId = entry.oldId?.name ?: "",
                        newId = entry.newId?.name ?: "",
                        comment = entry.comment,
                        who = GitAuthor(
                            name = entry.who?.name ?: "Unknown",
                            email = entry.who?.emailAddress ?: ""
                        ),
                        timestamp = entry.who?.whenAsInstant?.toEpochMilli() ?: 0
                    )
                }
        }
    }
    }
    
    // ============================================
    // Blame Operations
    // ============================================
    
    /**
     * Get blame information for a file
     */
    suspend fun blame(
        repoPath: String,
        filePath: String
    ): Result<List<BlameLine>> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val result = git.blame()
                .setFilePath(filePath)
                .call()
            
            val blameLines = mutableListOf<BlameLine>()
            val resultSize = result.resultContents?.size() ?: 0
            
            for (i in 0 until resultSize) {
                val commit = result.getSourceCommit(i)
                val content = result.resultContents?.getString(i) ?: ""
                
                blameLines.add(BlameLine(
                    lineNumber = i + 1,
                    content = content,
                    commitSha = commit?.name ?: "",
                    author = commit?.let { 
                        GitAuthor(
                            name = it.authorIdent.name,
                            email = it.authorIdent.emailAddress
                        )
                    } ?: GitAuthor("Unknown", ""),
                    timestamp = commit?.commitTime?.toLong()?.times(1000) ?: 0
                ))
            }
            
            blameLines
        }
    }
    }
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
