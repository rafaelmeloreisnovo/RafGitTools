package com.rafgittools.rafgitfs.write

import com.rafgittools.rafgitfs.data.StagedOperationDao
import com.rafgittools.rafgitfs.data.SyncConflictDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RafGitFsConflictResolver @Inject constructor(
    private val conflictDao: SyncConflictDao,
    private val stagedDao: StagedOperationDao,
    private val workspaceStore: RafGitFsWorkspaceStore
) {
    suspend fun resolve(
        workspaceId: String,
        conflictId: String,
        resolution: Resolution,
        confirmation: String
    ): Boolean {
        if (confirmation != "RESOLVE ${conflictId.takeLast(12)} ${resolution.name}") return false
        val conflict = conflictDao.getById(conflictId) ?: return false
        if (conflict.resolvedAt != null) return false
        if (conflict.workspaceId != null && conflict.workspaceId != workspaceId) return false
        when (resolution) {
            Resolution.USE_REMOTE -> {
                val operation = stagedDao.listForWorkspace(workspaceId)
                    .firstOrNull { it.path == conflict.path && it.operationType.startsWith("UPSERT_FILE:") }
                if (operation != null && !workspaceStore.rollbackFile(workspaceId, operation.operationId)) return false
            }
            Resolution.USE_LOCAL,
            Resolution.MANUAL -> Unit
        }
        return conflictDao.resolve(conflictId, resolution.name) == 1
    }

    enum class Resolution { USE_LOCAL, USE_REMOTE, MANUAL }
}
