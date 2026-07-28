package com.rafgittools.rafgitfs.index

import com.rafgittools.rafgitfs.data.VirtualTreeEntryEntity
import com.rafgittools.rafgitfs.model.RafGitFsCacheState
import com.rafgittools.rafgitfs.model.RafGitFsEntryType
import com.rafgittools.rafgitfs.remote.RafGitFsTreeEntryDto

object RafGitFsTreeMapper {
    fun map(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        entries: List<RafGitFsTreeEntryDto>,
        favoritePaths: Set<String>,
        observedAt: Long
    ): List<VirtualTreeEntryEntity> = entries
        .asSequence()
        .filter { it.path.isNotBlank() }
        .map { entry ->
            val normalizedPath = entry.path.trim('/')
            val parentPath = normalizedPath.substringBeforeLast('/', missingDelimiterValue = "")
            VirtualTreeEntryEntity(
                profileId = profileId,
                repositoryFullName = repositoryFullName,
                refName = refName,
                path = normalizedPath,
                parentPath = parentPath,
                name = normalizedPath.substringAfterLast('/'),
                entryType = entryType(entry).name,
                gitSha = entry.sha,
                sizeBytes = entry.size,
                mimeType = null,
                cacheState = RafGitFsCacheState.METADATA_CACHED.name,
                localPath = null,
                isFavorite = normalizedPath in favoritePaths,
                lastIndexedAt = observedAt,
                lastAccessedAt = observedAt
            )
        }
        .distinctBy { it.path }
        .sortedWith(compareBy<VirtualTreeEntryEntity>({ it.parentPath }, { it.name.lowercase() }))
        .toList()

    fun entryType(entry: RafGitFsTreeEntryDto): RafGitFsEntryType = when {
        entry.mode == "120000" -> RafGitFsEntryType.SYMLINK
        entry.type == "tree" -> RafGitFsEntryType.DIRECTORY
        entry.type == "commit" || entry.mode == "160000" -> RafGitFsEntryType.SUBMODULE
        else -> RafGitFsEntryType.FILE
    }
}
