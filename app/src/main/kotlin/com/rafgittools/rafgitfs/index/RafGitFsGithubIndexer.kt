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
import com.rafgittools.rafgitfs.model.RafGitFsCacheState
import com.rafgittools.rafgitfs.model.RafGitFsEntryType
import com.rafgittools.rafgitfs.remote.RafGitFsGithubRemoteDataSource
import com.rafgittools.rafgitfs.remote.RafGitFsRateLimitSnapshot
import com.rafgittools.rafgitfs.remote.RafGitFsRemoteMetadata
import com.rafgittools.rafgitfs.remote.RafGitFsRemoteResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Read-only GitHub -> Room v6 indexer. GitHub remains authoritative. */
@Singleton
class RafGitFsGithubIndexer @Inject constructor(
    private val remote: RafGitFsGithubRemoteDataSource,
    private val storageProfileDao: StorageProfileDao,
    private val repositoryCacheDao: RepositoryNameCacheDao,
    private val repositoryRefDao: RepositoryRefDao,
    private val virtualTreeDao: VirtualTreeDao
) {
    suspend fun refreshRepositories(profileId: String): RafGitFsRemoteResult<RafGitFsRepositoryRefresh> {
        profileGate<RafGitFsRepositoryRefresh>(profileId)?.let { return it }
        return when (val result = remote.listRepositories()) {
            is RafGitFsRemoteResult.Observed -> {
                persistRepositories(result.value)
                RafGitFsRemoteResult.Observed(
                    RafGitFsRepositoryRefresh(result.value.size, result.metadata.pagesFetched, true),
                    result.metadata
                )
            }
            is RafGitFsRemoteResult.TokenVazio -> {
                val partial = result.partialValue.orEmpty()
                persistRepositories(partial)
                RafGitFsRemoteResult.TokenVazio(
                    result.reason,
                    RafGitFsRepositoryRefresh(partial.size, result.metadata.pagesFetched, false),
                    result.metadata.copy(complete = false)
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
        profileGate<RafGitFsRefRefresh>(profileId)?.let { return it }
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
                        profileId, repositoryFullName, branch.name, "BRANCH", branch.commit.sha,
                        branch.name == defaultBranch, now
                    )
                )
            }
            tags.first.forEach { tag ->
                add(RepositoryRefEntity(profileId, repositoryFullName, tag.name, "TAG", tag.commit.sha, false, now))
            }
        }.distinctBy { "${it.refType}:${it.refName}" }

        repositoryRefDao.upsertAll(refs)
        val complete = branches.second && tags.second
        if (complete) repositoryRefDao.deleteStale(profileId, repositoryFullName, now)

        val report = RafGitFsRefRefresh(
            repositoryFullName, branches.first.size, tags.first.size, complete, now
        )
        val metadata = combineMetadata(metadataOf(branchesResult), metadataOf(tagsResult), complete)
        return if (complete) RafGitFsRemoteResult.Observed(report, metadata)
        else RafGitFsRemoteResult.TokenVazio("REF_INDEX_PARTIAL", report, metadata.copy(complete = false))
    }

    suspend fun refreshTree(
        profileId: String,
        repositoryFullName: String,
        refName: String
    ): RafGitFsRemoteResult<RafGitFsTreeRefresh> {
        profileGate<RafGitFsTreeRefresh>(profileId)?.let { return it }
        val commitResult = remote.resolveCommit(repositoryFullName, refName)
        val commit = when (commitResult) {
            is RafGitFsRemoteResult.Observed -> commitResult.value
            is RafGitFsRemoteResult.TokenVazio -> return RafGitFsRemoteResult.TokenVazio(commitResult.reason, null, commitResult.metadata)
            is RafGitFsRemoteResult.Failure -> return commitResult
            is RafGitFsRemoteResult.RateLimited -> return commitResult
            is RafGitFsRemoteResult.NotModified -> return commitResult
        }

        val indexedCommitSha = virtualTreeDao.getIndexedCommitSha(profileId, repositoryFullName, refName)
        if (indexedCommitSha == commit.sha) {
            return RafGitFsRemoteResult.NotModified(metadataOf(commitResult).copy(complete = true))
        }

        val treeSha = commit.commit.tree.sha
        val treeResult = remote.getTree(repositoryFullName, treeSha)
        val tree = when (treeResult) {
            is RafGitFsRemoteResult.Observed -> treeResult.value to true
            is RafGitFsRemoteResult.TokenVazio -> {
                val partial = treeResult.partialValue
                    ?: return RafGitFsRemoteResult.TokenVazio(treeResult.reason, null, treeResult.metadata)
                partial to false
            }
            is RafGitFsRemoteResult.Failure -> return treeResult
            is RafGitFsRemoteResult.RateLimited -> return treeResult
            is RafGitFsRemoteResult.NotModified -> return treeResult
        }

        val now = System.currentTimeMillis()
        val favorites = virtualTreeDao.listFavoritePaths(profileId, repositoryFullName, refName).toSet()
        val entries = RafGitFsTreeMapper.map(
            profileId, repositoryFullName, refName, tree.first.tree, favorites, now
        )
        if (entries.isNotEmpty()) virtualTreeDao.upsertAll(entries)

        if (tree.second) {
            virtualTreeDao.upsertAll(listOf(snapshotMarker(profileId, repositoryFullName, refName, commit.sha, now)))
            virtualTreeDao.deleteStale(profileId, repositoryFullName, refName, now)
            val cachedRef = repositoryRefDao.get(profileId, repositoryFullName, refName)
            repositoryRefDao.upsertAll(
                listOf(
                    RepositoryRefEntity(
                        profileId, repositoryFullName, refName, cachedRef?.refType ?: "BRANCH",
                        commit.sha, cachedRef?.isDefault ?: false, now
                    )
                )
            )
        }

        val report = RafGitFsTreeRefresh(
            repositoryFullName, refName, commit.sha, treeSha, entries.size, true, tree.second, now
        )
        return if (tree.second) RafGitFsRemoteResult.Observed(report, metadataOf(treeResult))
        else RafGitFsRemoteResult.TokenVazio(
            "TREE_INDEX_PARTIAL", report, metadataOf(treeResult).copy(complete = false)
        )
    }

    suspend fun readContent(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        path: String,
        maxBytes: Long = RafGitFsContentDecoder.DEFAULT_MAX_IN_MEMORY_BYTES
    ): RafGitFsRemoteResult<RafGitFsContentSnapshot> {
        profileGate<RafGitFsContentSnapshot>(profileId)?.let { return it }
        val entry = virtualTreeDao.getEntry(profileId, repositoryFullName, refName, path.trim('/'))
            ?: return tokenVazio("TREE_ENTRY_NOT_INDEXED")
        if (entry.entryType !in setOf("FILE", "SYMLINK")) return tokenVazio("ENTRY_NOT_READABLE:${entry.entryType}")
        val sha = entry.gitSha ?: return tokenVazio("ENTRY_SHA_MISSING")
        val blobResult = remote.getBlob(repositoryFullName, sha)
        val blob = when (blobResult) {
            is RafGitFsRemoteResult.Observed -> blobResult.value
            is RafGitFsRemoteResult.TokenVazio -> return RafGitFsRemoteResult.TokenVazio(blobResult.reason, null, blobResult.metadata)
            is RafGitFsRemoteResult.Failure -> return blobResult
            is RafGitFsRemoteResult.RateLimited -> return blobResult
            is RafGitFsRemoteResult.NotModified -> return blobResult
        }
        val decoded = try {
            RafGitFsContentDecoder.decode(blob, maxBytes)
        } catch (error: IllegalArgumentException) {
            return RafGitFsRemoteResult.TokenVazio(
                "BLOB_DECODE_BLOCKED:${error.message}", null, metadataOf(blobResult).copy(complete = false)
            )
        }
        val snapshot = RafGitFsContentSnapshot(
            repositoryFullName, refName, entry.path, blob.sha, blob.size,
            decoded.bytes, decoded.textUtf8, System.currentTimeMillis()
        )
        if (blob.sha != sha || decoded.bytes.size.toLong() != blob.size) {
            return RafGitFsRemoteResult.TokenVazio(
                "BLOB_INTEGRITY_MISMATCH", snapshot, metadataOf(blobResult).copy(complete = false)
            )
        }
        return RafGitFsRemoteResult.Observed(snapshot, metadataOf(blobResult))
    }

    fun observeChildren(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        parentPath: String
    ): Flow<List<VirtualTreeEntryEntity>> =
        virtualTreeDao.observeChildren(profileId, repositoryFullName, refName, parentPath.trim('/'))

    suspend fun searchLocal(profileId: String, query: String, limit: Int = 100): List<VirtualTreeEntryEntity> {
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
                    repository.id, repository.name, repository.fullName, repository.owner.login,
                    repository.description, repository.language, repository.stargazersCount,
                    repository.forksCount, repository.isPrivate, now, repository.watchersCount,
                    repository.openIssuesCount, repository.isFork, repository.defaultBranch,
                    repository.createdAt, repository.updatedAt
                )
            }
        )
    }

    private fun snapshotMarker(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        commitSha: String,
        observedAt: Long
    ) = VirtualTreeEntryEntity(
        profileId = profileId,
        repositoryFullName = repositoryFullName,
        refName = refName,
        path = "",
        parentPath = "",
        name = "",
        entryType = RafGitFsEntryType.DIRECTORY.name,
        gitSha = commitSha,
        sizeBytes = null,
        mimeType = "application/x-rafgitfs-index-snapshot",
        cacheState = RafGitFsCacheState.METADATA_CACHED.name,
        localPath = null,
        isFavorite = false,
        lastIndexedAt = observedAt,
        lastAccessedAt = observedAt
    )

    private suspend fun <T> profileGate(profileId: String): RafGitFsRemoteResult<T>? {
        val profile = storageProfileDao.getById(profileId) ?: return tokenVazio("PROFILE_NOT_FOUND")
        if (!profile.isEnabled) return tokenVazio("PROFILE_DISABLED")
        if (profile.provider != "GITHUB") return tokenVazio("PROFILE_PROVIDER_NOT_GITHUB")
        if (profile.claimAllowed) return tokenVazio("PROFILE_CLAIM_PROMOTION_BLOCKED")
        return null
    }

    private fun metadataOf(result: RafGitFsRemoteResult<*>): RafGitFsRemoteMetadata = when (result) {
        is RafGitFsRemoteResult.Observed -> result.metadata
        is RafGitFsRemoteResult.TokenVazio -> result.metadata
        is RafGitFsRemoteResult.NotModified -> result.metadata
        is RafGitFsRemoteResult.Failure, is RafGitFsRemoteResult.RateLimited -> RafGitFsRemoteMetadata(complete = false)
    }

    private fun combineMetadata(
        first: RafGitFsRemoteMetadata,
        second: RafGitFsRemoteMetadata,
        complete: Boolean
    ) = RafGitFsRemoteMetadata(
        pagesFetched = first.pagesFetched + second.pagesFetched,
        requestId = listOfNotNull(first.requestId, second.requestId).joinToString(",").ifBlank { null },
        etag = second.etag ?: first.etag,
        rateLimit = if (second.rateLimit.isKnown()) second.rateLimit else first.rateLimit,
        complete = complete
    )

    private fun RafGitFsRateLimitSnapshot.isKnown(): Boolean =
        limit != null || remaining != null || resetAtEpochSeconds != null || retryAfterSeconds != null

    private fun <T> tokenVazio(reason: String): RafGitFsRemoteResult<T> =
        RafGitFsRemoteResult.TokenVazio(reason, null, RafGitFsRemoteMetadata(complete = false))
}
