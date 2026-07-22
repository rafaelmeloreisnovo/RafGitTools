package com.rafgittools.webhook

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles GitHub webhook payload validation and lightweight routing.
 *
 * Validates the payload, routes to typed handlers, and dispatches optional
 * side-effects (cache invalidation, enqueue) when dependencies are wired via
 * [setDependencies].  Tests and callers that don't need side-effects can call
 * [handle] without calling [setDependencies] first — all side-effects are no-ops
 * when the optional dependencies are null.
 */
object WebhookHandler {

    private val supportedEvents = setOf(
        "ping",
        "push",
        "pull_request",
        "issues",
        "issue_comment",
        "release"
    )

    private val handlerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var onCacheInvalidate: (() -> Unit)? = null
    @Volatile private var onPushReceived: ((repoFullName: String, branch: String) -> Unit)? = null

    fun setDependencies(
        cacheInvalidate: (() -> Unit)? = null,
        pushReceived: ((repoFullName: String, branch: String) -> Unit)? = null
    ) {
        onCacheInvalidate = cacheInvalidate
        onPushReceived = pushReceived
    }

    fun handle(eventType: String, payload: String): Result<Unit> {
        val normalizedEvent = eventType.trim().lowercase()
        if (normalizedEvent.isBlank()) {
            return Result.failure(IllegalArgumentException("Webhook event type cannot be blank"))
        }

        if (normalizedEvent !in supportedEvents) {
            return Result.failure(IllegalArgumentException("Unsupported webhook event: $normalizedEvent"))
        }

        val jsonObject = parsePayload(payload).getOrElse { return Result.failure(it) }

        return runCatching {
            when (normalizedEvent) {
                "ping"          -> handlePing(jsonObject)
                "push"          -> handlePush(jsonObject)
                "pull_request"  -> handlePullRequest(jsonObject)
                "issues"        -> handleIssues(jsonObject)
                "issue_comment" -> handleIssueComment(jsonObject)
                "release"       -> handleRelease(jsonObject)
                else            -> Result.failure(IllegalStateException("Unexpected webhook event routing state"))
            }
        }.getOrElse { Result.failure(it) }
    }

    private fun parsePayload(payload: String): Result<JsonObject> {
        if (payload.isBlank()) {
            return Result.failure(IllegalArgumentException("Webhook payload cannot be blank"))
        }

        return runCatching {
            val parsed = JsonParser.parseString(payload)
            require(parsed.isJsonObject) { "Webhook payload must be a JSON object" }
            parsed.asJsonObject
        }.mapCatching { it }
    }

    private fun handlePing(payload: JsonObject): Result<Unit> {
        // zen field is optional — just acknowledge
        val zen = payload.get("zen")?.asString ?: ""
        return Result.success(Unit)
    }

    private fun handlePush(payload: JsonObject): Result<Unit> {
        require(payload.has("ref")) { "push payload missing 'ref'" }
        val ref = payload.get("ref").asString
        val branch = ref.removePrefix("refs/heads/")
        val repoFullName = payload.getAsJsonObject("repository")?.get("full_name")?.asString ?: ""
        if (repoFullName.isNotBlank()) {
            onPushReceived?.invoke(repoFullName, branch)
        }
        return Result.success(Unit)
    }

    private fun handlePullRequest(payload: JsonObject): Result<Unit> {
        require(payload.has("action")) { "pull_request payload missing 'action'" }
        require(payload.has("pull_request")) { "pull_request payload missing 'pull_request' object" }
        val action = payload.get("action").asString
        val pr = payload.getAsJsonObject("pull_request")
        val number = pr?.get("number")?.asInt ?: 0
        val title = pr?.get("title")?.asString ?: ""
        handlerScope.launch { onCacheInvalidate?.invoke() }
        return Result.success(Unit)
    }

    private fun handleIssues(payload: JsonObject): Result<Unit> {
        require(payload.has("action")) { "issues payload missing 'action'" }
        require(payload.has("issue")) { "issues payload missing 'issue' object" }
        val action = payload.get("action").asString
        val issueNumber = payload.getAsJsonObject("issue")?.get("number")?.asInt ?: 0
        handlerScope.launch { onCacheInvalidate?.invoke() }
        return Result.success(Unit)
    }

    private fun handleIssueComment(payload: JsonObject): Result<Unit> {
        require(payload.has("action")) { "issue_comment payload missing 'action'" }
        require(payload.has("comment")) { "issue_comment payload missing 'comment' object" }
        handlerScope.launch { onCacheInvalidate?.invoke() }
        return Result.success(Unit)
    }

    private fun handleRelease(payload: JsonObject): Result<Unit> {
        require(payload.has("action")) { "release payload missing 'action'" }
        require(payload.has("release")) { "release payload missing 'release' object" }
        val action = payload.get("action").asString
        val tagName = payload.getAsJsonObject("release")?.get("tag_name")?.asString ?: ""
        return Result.success(Unit)
    }
}
