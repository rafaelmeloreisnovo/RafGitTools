package com.rafgittools.offline

/**
 * WorkManager-backed sync dispatcher.
 *
 * Drains the OfflineQueue, calls execute() on each SyncOperation,
 * and re-enqueues items that fail with a transient error.
 */
object BackgroundSyncManager {
    interface QueueItem {
        fun execute(): Result<Unit>
    }

    fun <T : QueueItem> sync(queue: OfflineQueue<T>): Boolean {
        // Keep this method pure processing logic so it can be invoked from
        // worker orchestration (e.g. WorkManager) without duplicating behavior.
        val pendingCount = queue.size()
        if (pendingCount == 0) {
            return true
        }

        var allSuccessful = true
        repeat(pendingCount) {
            val item = queue.dequeue() ?: return@repeat
            val result = item.execute()
            if (result.isFailure) {
                allSuccessful = false
                queue.enqueue(item)
            }
        }

        return allSuccessful
    }
}
