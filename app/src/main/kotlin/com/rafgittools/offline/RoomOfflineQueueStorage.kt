package com.rafgittools.offline

import kotlinx.coroutines.runBlocking

class RoomOfflineQueueStorage(
    private val dao: OfflineOperationDao
) : OfflineQueueStorage<SyncOperation> {

    override fun load(): List<SyncOperation> = runBlocking {
        dao.loadAll().mapNotNull { entity ->
            runCatching { SyncOperation.decode(entity.args.toByteArray(Charsets.UTF_8)) }.getOrNull()
        }
    }

    override fun replace(items: List<SyncOperation>) = runBlocking {
        val entities = items.map { op ->
            OfflineOperationEntity(
                repoPath = repoPathOf(op),
                command = op::class.simpleName ?: "Unknown",
                args = SyncOperation.encode(op).toString(Charsets.UTF_8)
            )
        }
        dao.replaceAll(entities)
    }

    private fun repoPathOf(op: SyncOperation): String = when (op) {
        is SyncOperation.GitPush -> op.repoPath
        is SyncOperation.GitPull -> op.repoPath
        is SyncOperation.GitHubApiCall -> ""
    }
}
