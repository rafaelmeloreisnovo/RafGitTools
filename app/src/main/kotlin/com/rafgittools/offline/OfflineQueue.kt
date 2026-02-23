package com.rafgittools.offline

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * OfflineQueue stub.
 *
 * Provides a simple in-memory queue for requests that should be executed
 * when network connectivity is restored. In a real implementation this
 * would persist queued requests to disk and retry them with exponential
 * backoff and error handling.
 */
class OfflineQueue<T> {
    private val queue = ConcurrentLinkedQueue<T>()

    fun enqueue(item: T) {
        queue.add(item)
    }

    fun dequeue(): T? {
        return queue.poll()
    }

    fun isEmpty(): Boolean = queue.isEmpty()

    fun size(): Int = queue.size
}