package com.rafgittools.offline

import com.google.gson.Gson
import com.rafgittools.data.git.JGitService
import com.rafgittools.data.github.GithubApiService
import kotlinx.coroutines.runBlocking
import java.nio.charset.StandardCharsets

/**
 * Concrete serializable offline operation.
 *
 * Each [SyncOperation] represents a single unit of work that can survive
 * process death and be replayed by [SyncWorker] via WorkManager.
 *
 * Before calling [BackgroundSyncManager.sync], the caller (SyncWorker) must
 * set [jGitService] and [githubApiService] on the companion object so that
 * execute() can dispatch to real implementations.
 */
sealed class SyncOperation : BackgroundSyncManager.QueueItem {

    data class GitPush(
        val repoPath: String,
        val remote: String,
        val branch: String,
    ) : SyncOperation() {
        override fun execute(): Result<Unit> {
            val svc = jGitService ?: return Result.failure(
                IllegalStateException("JGitService not injected — call SyncOperation.inject() before sync")
            )
            return runBlocking { svc.push(repoPath, remote, branch, null) }
        }
    }

    data class GitPull(
        val repoPath: String,
        val remote: String,
        val branch: String,
    ) : SyncOperation() {
        override fun execute(): Result<Unit> {
            val svc = jGitService ?: return Result.failure(
                IllegalStateException("JGitService not injected — call SyncOperation.inject() before sync")
            )
            return runBlocking { svc.pull(repoPath, remote, branch, null) }
        }
    }

    data class GitHubApiCall(
        val endpoint: String,
        val method: String,
        val bodyJson: String,
    ) : SyncOperation() {
        @Suppress("UNCHECKED_CAST")
        override fun execute(): Result<Unit> {
            val svc = githubApiService ?: return Result.failure(
                IllegalStateException("GithubApiService not injected — call SyncOperation.inject() before sync")
            )
            return runBlocking {
                runCatching {
                    val parts = endpoint.trimStart('/').split('/')
                    when {
                        method == "GET" && endpoint.trimStart('/') == "user" ->
                            svc.getAuthenticatedUser()
                        method == "GET" && endpoint.trimStart('/') == "user/repos" ->
                            svc.getUserRepositories()
                        method == "GET" && parts.size == 2 && parts[0] == "users" ->
                            svc.getUser(parts[1])
                        method == "GET" && parts.size >= 4 && parts[2] == "issues" ->
                            svc.getIssues(parts[0], parts[1])
                        method == "GET" && parts.size >= 4 && parts[2] == "pulls" ->
                            svc.getPullRequests(parts[0], parts[1])
                        method == "POST" && parts.size == 4 && parts[2] == "issues" -> {
                            val body = gson.fromJson(bodyJson, Map::class.java) as Map<String, Any>
                            svc.createIssue(
                                owner = parts[0], repo = parts[1],
                                issue = com.rafgittools.data.github.CreateIssueRequest(
                                    title = body["title"] as? String ?: "",
                                    body = body["body"] as? String,
                                    labels = (body["labels"] as? List<*>)?.filterIsInstance<String>(),
                                    assignees = (body["assignees"] as? List<*>)?.filterIsInstance<String>()
                                )
                            )
                        }
                        method == "POST" && parts.size == 5 && parts[2] == "issues" && parts[4] == "comments" -> {
                            val body = gson.fromJson(bodyJson, Map::class.java) as Map<String, Any>
                            svc.createIssueComment(
                                owner = parts[0], repo = parts[1],
                                number = parts[3].toInt(),
                                comment = com.rafgittools.data.github.CreateCommentRequest(
                                    body = body["body"] as? String ?: ""
                                )
                            )
                        }
                        else -> throw UnsupportedOperationException("No typed handler for $method $endpoint")
                    }
                    Unit
                }
            }
        }
    }

    companion object {
        private val gson = Gson()

        /** Set by SyncWorker before calling BackgroundSyncManager.sync(). */
        @Volatile var jGitService: JGitService? = null
        @Volatile var githubApiService: GithubApiService? = null

        fun inject(jGit: JGitService, github: GithubApiService) {
            jGitService = jGit
            githubApiService = github
        }

        fun clearInjection() {
            jGitService = null
            githubApiService = null
        }

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
