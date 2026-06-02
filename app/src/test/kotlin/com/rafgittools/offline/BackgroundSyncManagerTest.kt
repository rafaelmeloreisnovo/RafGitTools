package com.rafgittools.offline

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackgroundSyncManagerTest {

    @Test
    fun `sync returns true and drains queue when all entries execute successfully`() {
        val queue = OfflineQueue<BackgroundSyncManager.QueueItem>()
        var executionCount = 0

        repeat(3) {
            queue.enqueue(
                object : BackgroundSyncManager.QueueItem {
                    override fun execute(): Result<Unit> {
                        executionCount++
                        return Result.success(Unit)
                    }
                }
            )
        }

        val synced = BackgroundSyncManager.sync(queue)

        assertThat(synced).isTrue()
        assertThat(executionCount).isEqualTo(3)
        assertThat(queue.isEmpty()).isTrue()
    }

    @Test
    fun `sync requeues failures and reports partial failure`() {
        val queue = OfflineQueue<BackgroundSyncManager.QueueItem>()
        var successItemExecutions = 0
        var flakyItemExecutions = 0

        queue.enqueue(
            object : BackgroundSyncManager.QueueItem {
                override fun execute(): Result<Unit> {
                    successItemExecutions++
                    return Result.success(Unit)
                }
            }
        )
        queue.enqueue(
            object : BackgroundSyncManager.QueueItem {
                override fun execute(): Result<Unit> {
                    flakyItemExecutions++
                    return if (flakyItemExecutions == 1) {
                        Result.failure(IllegalStateException("transient"))
                    } else {
                        Result.success(Unit)
                    }
                }
            }
        )

        val firstSync = BackgroundSyncManager.sync(queue)

        assertThat(firstSync).isFalse()
        assertThat(successItemExecutions).isEqualTo(1)
        assertThat(flakyItemExecutions).isEqualTo(1)
        assertThat(queue.size()).isEqualTo(1)

        val secondSync = BackgroundSyncManager.sync(queue)

        assertThat(secondSync).isTrue()
        assertThat(flakyItemExecutions).isEqualTo(2)
        assertThat(queue.isEmpty()).isTrue()
    }

    @Test
    fun `sync is idempotent when queue is empty`() {
        val queue = OfflineQueue<BackgroundSyncManager.QueueItem>()

        val firstSync = BackgroundSyncManager.sync(queue)
        val secondSync = BackgroundSyncManager.sync(queue)

        assertThat(firstSync).isTrue()
        assertThat(secondSync).isTrue()
        assertThat(queue.isEmpty()).isTrue()
    }
}
