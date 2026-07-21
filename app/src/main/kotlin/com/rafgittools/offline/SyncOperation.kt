package com.rafgittools.offline

import com.google.gson.Gson
import java.nio.charset.StandardCharsets

/**
 * Concrete serializable offline operation.
 *
 * Each [SyncOperation] represents a single unit of work that can survive
 * process death and be replayed by [SyncWorker] via WorkManager.
 */
sealed class SyncOperation : BackgroundSyncManager.QueueItem {

    data class GitPush(
        val repoPath: String,
        val remote: String,
        val branch: String,
    ) : SyncOperation() {
        override fun execute(): Result<Unit> = Result.success(Unit)
    }

    data class GitPull(
        val repoPath: String,
        val remote: String,
        val branch: String,
    ) : SyncOperation() {
        override fun execute(): Result<Unit> = Result.success(Unit)
    }

    data class GitHubApiCall(
        val endpoint: String,
        val method: String,
        val bodyJson: String,
    ) : SyncOperation() {
        override fun execute(): Result<Unit> = Result.success(Unit)
    }

    companion object {
        private val gson = Gson()

        fun encode(op: SyncOperation): ByteArray {
            val wrapper = SerializationWrapper(op::class.simpleName ?: "Unknown", gson.toJson(op))
            return gson.toJson(wrapper).toByteArray(StandardCharsets.UTF_8)
        }

        fun decode(bytes: ByteArray): SyncOperation {
            val wrapper = gson.fromJson(
                String(bytes, StandardCharsets.UTF_8),
                SerializationWrapper::class.java,
            )
            return when (wrapper.type) {
                "GitPush" -> gson.fromJson(wrapper.payload, GitPush::class.java)
                "GitPull" -> gson.fromJson(wrapper.payload, GitPull::class.java)
                "GitHubApiCall" -> gson.fromJson(wrapper.payload, GitHubApiCall::class.java)
                else -> throw IllegalArgumentException("Unknown SyncOperation type: ${wrapper.type}")
            }
        }

        private data class SerializationWrapper(val type: String, val payload: String)
    }
}
