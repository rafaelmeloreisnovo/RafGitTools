package com.rafgittools.offline

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rafgittools.di.SyncOperationEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.io.File

/**
 * WorkManager worker that drains the persisted [OfflineQueue] via [BackgroundSyncManager].
 *
 * WorkManager schedules this periodically (see [schedulePeriodicSync] in Application).
 * On each run the worker loads all pending [SyncOperation]s from disk, executes them,
 * and persists the remainder (any that failed are re-queued by BackgroundSyncManager).
 *
 * Failures return [Result.retry] so WorkManager applies its back-off policy.
 * A fully-drained queue returns [Result.success].
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ep = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncOperationEntryPoint::class.java
        )
        SyncOperation.inject(ep.jGitService(), ep.githubApiService())
        return try {
            val queue = buildQueue(applicationContext)
            val allSucceeded = BackgroundSyncManager.sync(queue)
            if (allSucceeded) Result.success() else Result.retry()
        } finally {
            SyncOperation.clearInjection()
        }
    }

    companion object {
        const val WORK_NAME = "rafgittools.background_sync"

        private fun queueFile(context: Context): File =
            File(context.filesDir, "offline_queue/sync_ops.bin")

        fun buildQueue(context: Context): OfflineQueue<BackgroundSyncManager.QueueItem> {
            val storage = AtomicFileQueueStorage(
                file = queueFile(context),
                encode = { (it as SyncOperation).let(SyncOperation::encode) },
                decode = SyncOperation::decode,
            )
            return OfflineQueue(storage)
        }
    }
}
