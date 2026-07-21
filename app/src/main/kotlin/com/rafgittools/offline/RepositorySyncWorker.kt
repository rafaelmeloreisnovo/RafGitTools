package com.rafgittools.offline

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rafgittools.data.cache.LocalRepositoryEntity
import com.rafgittools.di.RepositorySyncEntryPoint
import com.rafgittools.domain.model.SyncState
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.BranchTrackingStatus
import java.io.File

/**
 * WorkManager worker that periodically fetches from remotes and updates
 * the sync state (SYNCED / BEHIND / AHEAD / DIVERGED) for all known local repos.
 *
 * Credentials are intentionally omitted — fetch is best-effort. If the remote
 * requires auth and none is configured in the JGit credential chain, the fetch
 * is skipped and only the current tracking-branch state is recorded.
 */
class RepositorySyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val WORK_NAME = "rafgittools.repository_sync"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val dao = EntryPointAccessors
            .fromApplication(applicationContext, RepositorySyncEntryPoint::class.java)
            .localRepositoryDao()

        val reposDir = File(applicationContext.getExternalFilesDir(null), "repositories")

        // Discover git repos on disk and register any that aren't tracked yet
        if (reposDir.exists() && reposDir.isDirectory) {
            reposDir.listFiles()?.forEach { dir ->
                if (dir.isDirectory && File(dir, ".git").isDirectory) {
                    dao.insertIfAbsent(
                        LocalRepositoryEntity(
                            path = dir.absolutePath,
                            name = dir.name,
                            remoteUrl = null,
                            currentBranch = null,
                            syncState = SyncState.SYNCED.name
                        )
                    )
                }
            }
        }

        var allSucceeded = true
        for (entity in dao.loadAll()) {
            val repoDir = File(entity.path)
            if (!repoDir.isDirectory) continue

            try {
                Git.open(repoDir).use { git ->
                    val repo = git.repository

                    // Best-effort fetch — ignore credential failures
                    try {
                        git.fetch()
                            .setRemoveDeletedRefs(false)
                            .call()
                    } catch (_: Exception) {
                        // No credentials available or no remote — skip fetch, still check state
                    }

                    val branch = repo.branch
                    val tracking = BranchTrackingStatus.of(repo, branch)
                    val syncState = when {
                        tracking == null -> SyncState.SYNCED
                        tracking.aheadCount > 0 && tracking.behindCount > 0 -> SyncState.DIVERGED
                        tracking.behindCount > 0 -> SyncState.BEHIND
                        tracking.aheadCount > 0 -> SyncState.AHEAD
                        else -> SyncState.SYNCED
                    }

                    dao.updateSyncState(
                        path = entity.path,
                        syncState = syncState.name
                    )
                }
            } catch (e: Exception) {
                allSucceeded = false
            }
        }

        if (allSucceeded) Result.success() else Result.retry()
    }
}
