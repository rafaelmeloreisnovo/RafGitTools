package com.rafgittools.rafgitfs.index

import com.rafgittools.data.cache.RepositoryNameCache
import com.rafgittools.data.cache.RepositoryNameCacheDao
import com.rafgittools.domain.model.github.GithubCodeSearchItem
import com.rafgittools.domain.model.github.GithubRepository
import com.rafgittools.rafgitfs.data.RepositoryRefDao
import com.rafgittools.rafgitfs.data.RepositoryRefEntity
import com.rafgittools.rafgitfs.data.StorageProfileDao
import com.rafgittools.rafgitfs.data.VirtualTreeDao
import com.rafgittools.rafgitfs.data.VirtualTreeEntryEntity
import com.rafgittools.rafgitfs.remote.RafGitFsGithubRemoteDataSource
import com.rafgittools.rafgitfs.remote.RafGitFsRemoteMetadata
import com.rafgittools.rafgitfs.remote.RafGitFsRemoteResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-only GitHub -> Room v6 indexer.
 *
 * GitHub remains authoritative. Every persisted SHA comes from a response
 * classified as OBSERVED or as explicitly partial TOKEN_VAZIO evidence.
 */
@Singleton
class RafGitFsGithubIndexer @Inject constructor(
    private val remote: RafGitFsGithubRemoteDataSource,
    private val storageProfileDao: StorageProfileDao,
    private val repositoryCacheDao: RepositoryNameCacheDao,
    private val repositoryRefDao: RepositoryRefDao,
    private val virtualTreeDao: VirtualTreeDao
) {
    suspend fun refreshRepositories(
        profileId: String
    ): RafGitFsRemoteResult<RafGitFsRepositoryRefresh> {
        profileGate(profileId)?.let { return it }
        return when (val result = remote.listRepositories()) {
            is RafGitFsRemoteResult.Observed -> {
                persistRepositories(result.value)
                RafGitFsRemoteResult.Observed(
                    RafGitFsRepositoryRefresh(
                        repositoriesObserved = result.value.size,
                        pagesFetched = result.metadata.pagesFetched,
                        complete = true
                    ),
                    result.metadata
                )
            }
            is RafGitFsRemoteResult.TokenVazio -> {
                val partial = result.partialValue.orEmpty()
                persistRepositories(partial)
                RafGitFsRemoteResult.TokenVazio(
                    reason = result.reason,
                    partialValue = RafGitFsRepositoryRefresh(
                        repositoriesObserved = partial.size,
                        pagesFetched = result.metadata.pagesFetched,
                        complete = false
                    ),
                    metadata = result.metadata.copy(complete = false)
                )
            }
            is RafGitFsRemoteResult.Failure -> result
            is RafGitFsRemoteResult.RateLimited -> result
            is RafGitFsRemoteResult.NotModified -> result
        }
    }

    suspend fun refreshRefs(
        profileId: String,
        repositoryFullName: String
    ): RafGitFsRemoteResult<RafGitFsRefRefresh> {
        profileGate(profileId)?.let { return it }
        val branchesResult = remote.listBranches(repositoryFullName)
        val branches = when (branchesResult) {
            is RafGitFsRemoteResult.Observed -> branchesResult.value to true
            is RafGitFsRemoteResult.TokenVazio -> branchesResult.partialValue.orEmpty() to false
            is RafGitFsRemoteResult.Failure -> return branchesResult
            is RafGitFsRemoteResult.RateLimited -> return branchesResult
            is RafGitFsRemoteResult.NotModified -> return branchesResult
        }
        val tagsResult = remote.listTags(repositoryFullName)
        val tags = when (tagsResult) {
            is RafGitFsRemoteResult.Observed -> tagsResult.value to true
            is RafGitFsRemoteResult.TokenVazio -> tagsResult.partialValue.orEmpty() to false
            is RafGitFsRemoteResult.Failure -> return tagsResult
            is RafGitFsRemoteResult.RateLimited -> return tagsResult
            is RafGitFsRemoteResult.NotModified -> return tagsResult
        }

        val now = System.currentTimeMillis()
        val defaultBranch = repositoryCacheDao.getRepositoryByFullName(repositoryFullName)?.defaultBranch
        val refs = buildList {
            branches.first.forEach { branch ->
                add(
                    RepositoryRefEntity(
                        profileId = profileId,
                        repositoryFullName = repositoryFullName,
                        refName = branch.name,
                        refType = "BRANCH",
                        gitSha = branch.commit.sha,
                        isDefault = branch.name == defaultBranch,
                        lastIndexedAt = now
                    )
                )
            }
            tags.first.forEach { tag ->
                add(
                    RepositoryRefEntity(
                        profileId = profileId,
                        repositoryFullName = repositoryFullName,
                        refName = tag.name,
                        refType = "TAG",
                        gitSha = tag.commit.sha,
                        isDefault = false,
                        lastIndexedAt = now
                    )
                )
            }
        }.distinctBy { "${it.refType}:${it.refName}" }

        repositoryRefDao.upsertAll(refs)
        repositoryRefDao.deleteStale(profileId, repositoryFullName, now)
        val complete = branches.second && tags.second
        val report = RafGitFsRefRefresh(
            repositoryFullName = repositoryFullName,
            branchesObserved = branches.first.size,
            tagsObserved = tags.first.size,
            complete = complete,
            indexedAt = now
        )
        val metadata = combineMetadata(
            metadataOf(branchesResult),
            metadataOf(tagsResult),
            complete
        )
        return if (complete) {
            RafGitFsRemoteResult.Observed(report, metadata)
        } else {
            RafGitFsRemoteResult.TokenVazio(
                reason = "REF_INDEX_PARTIAL",
                partialValue = report,
                metadata = metadata.copy(complete = false)
            )
        }
    }

    suspend fun refreshTree(
        profileId: String,
        repositoryFullName: String,
        refName: String
    ): RafGitFsRemoteResult<RafGitFsTreeRefresh> {
        profileGate(profileId)?.let { return it }
        val commitResult = remote.resolveCommit(repositoryFullName, refName)
        val commit = when (commitResult) {
            is RafGitFsRemoteResult.Observed -> commitResult.value
            is RafGitFsRemoteResult.TokenVazio -> return RafGitFsRemoteResult.TokenVazio(
                commitResult.reason,
                null,
                commitResult.metadata
            )
            is RafGitFsRemoteResult.Failure -> return commitResult
            is RafGitFsRemoteResult.RateLimited -> return commitResult
            is RafGitFsRemoteResult.NotModified -> return commitResult
        }

        val cachedRef = repositoryRefDao.get(profileId, repositoryFullName, refName)
        val cachedEntries = virtualTreeDao.countForRef(profileId, repositoryFullName, refName)
        if (cachedRef?.gitSha == commit.sha && cachedEntries > 0) {
            return RafGitFsRemoteResult.NotModified(
                metadataOf(commitResult).copy(complete = true)
            )
        }

        val treeSha = commit.commit.tree.sha
        val treeResult = remote.getTree(repositoryFullName, treeSha)
        val tree = when (treeResult) {
            is RafGitFsRemoteResult.Observed -> treeResult.value to true
            is RafGitFsRemoteResult.TokenVazio -> {
                val partial = treeResult.partialValue ?: return RafGitFsRemoteResult.TokenVazio(
                    treeResult.reason,
                    null,
                    treeResult.metadata
                )
                partial to false
            }
            is RafGitFsRemoteResult.Failure -> return treeResult
            is RafGitFsRemoteResult.RateLimited -> return treeResult
            is RafGitFsRemoteResult.NotModified -> return treeResult
        }

        val now = System.currentTimeMillis()
        val favorites = virtualTreeDao.listFavoritePaths(profileId, repositoryFullName, refName).toSet()
        val entries = RafGitFsTreeMapper.map(
            profileId = profileId,
            repositoryFullName = repositoryFullName,
            refName = refName,
            entries = tree.first.tree,
            favoritePaths = favorites,
            observedAt = now
        )
        virtualTreeDao.upsertAll(entries)
        virtualTreeDao.deleteStale(profileId, repositoryFullName, refName, now)
        repositoryRefDao.upsertAll(
            listOf(
                RepositoryRefEntity(
                    profileId = profileId,
                    repositoryFullName = repositoryFullName,
                    refName = refName,
                    refType = cachedRef?.refType ?: "BRANCH",
                    gitSha = commit.sha,
                    isDefault = cachedRef?.isDefault ?: false,
                    lastIndexedAt = now
                )
            )
        )

        val report = RafGitFsTreeRefresh(
            repositoryFullName = repositoryFullName,
            refName = refName,
            commitSha = commit.sha,
            treeSha = treeSha,
            entriesIndexed = entries.size,
            changed = true,
            complete = tree.second,
            indexedAt = now
        )
        return if (tree.second) {
            RafGitFsRemoteResult.Observed(report, metadataOf(treeResult))
        } else {
            RafGitFsRemoteResult.TokenVazio(
                reason = "TREE_INDEX_PARTIAL",
                partialValue = report,
                metadata = metadataOf(treeResult).copy(complete = false)
            )
        }
    }

    suspend fun readContent(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        path: String,
        maxBytes: Long = RafGitFsContentDecoder.DEFAULT_MAX_IN_MEMORY_BYTES
    ): RafGitFsRemoteResult<RafGitFsContentSnapshot> {
        profileGate(profileId)?.let { return it }
        val entry = virtualTreeDao.getEntry(profileId, repositoryFullName, refName, path.trim('/'))
            ?: return tokenVazio("TREE_ENTRY_NOT_INDEXED")
        if (entry.entryType !in setOf("FILE", "SYMLINK")) {
            return tokenVazio("ENTRY_NOT_READABLE:${entry.entryType}")
        }
        val sha = entry.gitSha ?: return tokenVazio("ENTRY_SHA_MISSING")
        val blobResult = remote.getBlob(repositoryFullName, sha)
        val blob = when (blobResult) {
            is RafGitFsRemoteResult.Observed -> blobResult.value
            is RafGitFsRemoteResult.TokenVazio -> return RafGitFsRemoteResult.TokenVazio(
                blobResult.reason,
                null,
                blobResult.metadata
            )
            is RafGitFsRemoteResult.Failure -> return blobResult
            is RafGitFsRemoteResult.RateLimited -> return blobResult
            is RafGitFsRemoteResult.NotModified -> return blobResult
        }
        val decoded = try {
            RafGitFsContentDecoder.decode(blob, maxBytes)
        } catch (error: IllegalArgumentException) {
            return RafGitFsRemoteResult.TokenVazio(
                reason = "BLOB_DECODE_BLOCKED:${error.message}",
                partialValue = null,
                metadata = metadataOf(blobResult).copy(complete = false)
            )
        }
        val snapshot = RafGitFsContentSnapshot(
            repositoryFullName = repositoryFullName,
            refName = refName,
            path = entry.path,
            blobSha = blob.sha,
            sizeBytes = blob.size,
            bytes = decoded.bytes,
            textUtf8 = decoded.textUtf8,
            observedAt = System.currentTimeMillis()
        )
        if (blob.sha != sha || decoded.bytes.size.toLong() != blob.size) {
            return RafGitFsRemoteResult.TokenVazio(
                reason = "BLOB_INTEGRITY_MISMATCH",
                partialValue = snapshot,
                metadata = metadataOf(blobResult).copy(complete = false)
            )
        }
        return RafGitFsRemoteResult.Observed(snapshot, metadataOf(blobResult))
    }

    fun observeChildren(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        parentPath: String
    ): Flow<List<VirtualTreeEntryEntity>> = virtualTreeDao.observeChildren(
        profileId,
        repositoryFullName,
        refName,
        parentPath.trim('/')
    )

    suspend fun searchLocal(
        profileId: String,
        query: String,
        limit: Int = 100
    ): List<VirtualTreeEntryEntity> {
        require(query.isNotBlank()) { "query must not be blank" }
        return virtualTreeDao.search(profileId, query.trim(), limit.coerceIn(1, 500))
    }

    suspend fun searchRemote(
        repositoryFullName: String,
        query: String
    ): RafGitFsRemoteResult<List<GithubCodeSearchItem>> = remote.searchCode(repositoryFullName, query)

    private suspend fun persistRepositories(repositories: List<GithubRepository>) {
        if (repositories.isEmpty()) return
        val now = System.currentTimeMillis()
        repositoryCacheDao.insertRepositories(
            repositories.distinctBy { it.id }.map { repository ->
                RepositoryNameCache(
                    id = repository.id,
                    name = repository.name,
                    fullName = repository.fullName,
                    ownerLogin = repository.owner.login,
                    description = repository.description,
                    language = repository.language,
                    stargazersCount = repository.stargazersCount,
                    forksCount = repository.forksCount,
                    isPrivate = repository.isPrivate,
                    updatedAt = now,
                    watchersCount = repository.watchersCount,
                    openIssuesCount = repository.openIssuesCount,
                    isFork = repository.isFork,
                    defaultBranch = repository.defaultBranch,
                    createdAtGh = repository.createdAt,
                    updatedAtGh = repository.updatedAt
                )
            }
        )
    }

    private suspend fun <T> profileGate(profileId: String): RafGitFsRemoteResult<T>? {
        val profile = storageProfileDao.getById(profileId)
            ?: return tokenVazio("PROFILE_NOT_FOUND")
        if (!profile.isEnabled) return tokenVazio("PROFILE_DISABLED")
        if (profile.provider != "GITHUB") return tokenVazio("PROFILE_PROVIDER_NOT_GITHUB")
        if (profile.claimAllowed) return tokenVazio("PROFILE_CLAIM_PROMOTION_BLOCKED")
        return null
    }

    private fun metadataOf(result: RafGitFsRemoteResult<*>): RafGitFsRemoteMetadata = when (result) {
        is RafGitFsRemoteResult.Observed -> result.metadata
        is RafGitFsRemoteResult.TokenVazio -> result.metadata
        is RafGitFsRemoteResult.NotModified -> result.metadata
        is RafGitFsRemoteResult.Failure,
        is RafGitFsRemoteResult.RateLimited -> RafGitFsRemoteMetadata(complete = false)
    }

    private fun combineMetadata(
        first: RafGitFsRemoteMetadata,
        second: RafGitFsRemoteMetadata,
        complete: Boolean
    ) = RafGitFsRemoteMetadata(
        pagesFetched = first.pagesFetched + second.pagesFetched,
        requestId = listOfNotNull(first.requestId, second.requestId).joinToString(",").ifBlank { null },
        etag = second.etag ?: first.etag,
        rateLimit = second.rateLimit.takeUnless { it == com.rafgittools.rafgitfs.remote.RafGitFsRateLimitSnapshot.unknown() }
            ?: first.rateLimit,
        complete = complete
    )

    private fun <T> tokenVazio(reason: String): RafGitFsRemoteResult<T> =
        RafGitFsRemoteResult.TokenVazio(
            reason = reason,
            partialValue = null,
            metadata = RafGitFsRemoteMetadata(complete = false)
        )
}
