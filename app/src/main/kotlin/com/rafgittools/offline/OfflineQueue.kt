package com.rafgittools.offline

import java.util.ArrayDeque
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Storage boundary for [OfflineQueue].
 *
 * Implementations must replace their persisted snapshot atomically. When no
 * storage is supplied the queue remains intentionally in-memory only.
 */
interface OfflineQueueStorage<T> {
    fun load(): List<T>
    fun replace(items: List<T>)
}

/**
 * Thread-safe offline queue with optional durable persistence.
 *
 * Persistence is synchronous by design: enqueue/dequeue only return after the
 * new snapshot has been committed by [OfflineQueueStorage]. This gives callers
 * a clear durability boundary and avoids silently reporting a queued operation
 * that was never written.
 */
class OfflineQueue<T>(
    private val storage: OfflineQueueStorage<T>? = null
) {
    private val lock = ReentrantLock()
    private val queue = ArrayDeque<T>()

    init {
        storage?.load()?.forEach(queue::addLast)
    }

    fun enqueue(item: T) = lock.withLock {
        queue.addLast(item)
        persistOrRollback { queue.removeLast() }
    }

    fun dequeue(): T? = lock.withLock {
        val item = queue.pollFirst() ?: return null
        persistOrRollback { queue.addFirst(item) }
        item
    }

    /**
     * Atomically removes the highest-scoring item while preserving FIFO order
     * among equal scores. Persistence uses the same rollback boundary as the
     * ordinary queue operations.
     */
    fun dequeueBest(score: (T) -> Int): T? = lock.withLock {
        val before = queue.toList()
        if (before.isEmpty()) return null

        var bestIndex = 0
        var bestScore = score(before[0])
        for (index in 1 until before.size) {
            val candidateScore = score(before[index])
            if (candidateScore > bestScore) {
                bestIndex = index
                bestScore = candidateScore
            }
        }

        val selected = before[bestIndex]
        queue.clear()
        before.forEachIndexed { index, item ->
            if (index != bestIndex) queue.addLast(item)
        }

        persistOrRollback {
            queue.clear()
            before.forEach(queue::addLast)
        }
        selected
    }

    fun peek(): T? = lock.withLock { queue.peekFirst() }

    fun isEmpty(): Boolean = lock.withLock { queue.isEmpty() }

    fun size(): Int = lock.withLock { queue.size }

    fun snapshot(): List<T> = lock.withLock { queue.toList() }

    private fun persistOrRollback(rollback: () -> Unit) {
        try {
            storage?.replace(queue.toList())
        } catch (error: Exception) {
            rollback()
            throw IllegalStateException("Offline queue persistence failed", error)
        }
    }
}
