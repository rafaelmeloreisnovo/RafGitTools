package com.rafgittools.webhook

import com.google.gson.JsonParser

/**
 * Handles GitHub webhook payload validation and lightweight routing.
 *
 * Current implementation intentionally keeps processing minimal and deterministic
 * for safe use in local/internal integrations:
 * - validates basic payload shape (must be valid JSON object)
 * - validates event type against supported GitHub events
 * - routes to no-op handlers that can be expanded with real side effects
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
                "ping" -> handlePing(jsonObject)
                "push" -> handlePush(jsonObject)
                "pull_request" -> handlePullRequest(jsonObject)
                "issues" -> handleIssues(jsonObject)
                "issue_comment" -> handleIssueComment(jsonObject)
                "release" -> handleRelease(jsonObject)
                else -> Result.failure(IllegalStateException("Unexpected webhook event routing state"))
            }
        }.getOrElse { Result.failure(it) }
    }

    private fun parsePayload(payload: String): Result<com.google.gson.JsonObject> {
        if (payload.isBlank()) {
            return Result.failure(IllegalArgumentException("Webhook payload cannot be blank"))
        }

        return runCatching {
            val parsed = JsonParser.parseString(payload)
            require(parsed.isJsonObject) { "Webhook payload must be a JSON object" }
            parsed.asJsonObject
        }.mapCatching { it }
    }

    private fun handlePing(payload: com.google.gson.JsonObject): Result<Unit> {
        payload.get("zen")
        return Result.success(Unit)
    }

    private fun handlePush(payload: com.google.gson.JsonObject): Result<Unit> {
        require(payload.has("ref")) { "push payload missing 'ref'" }
        return Result.success(Unit)
    }

    private fun handlePullRequest(payload: com.google.gson.JsonObject): Result<Unit> {
        require(payload.has("action")) { "pull_request payload missing 'action'" }
        require(payload.has("pull_request")) { "pull_request payload missing 'pull_request' object" }
        return Result.success(Unit)
    }

    private fun handleIssues(payload: com.google.gson.JsonObject): Result<Unit> {
        require(payload.has("action")) { "issues payload missing 'action'" }
        require(payload.has("issue")) { "issues payload missing 'issue' object" }
        return Result.success(Unit)
    }

    private fun handleIssueComment(payload: com.google.gson.JsonObject): Result<Unit> {
        require(payload.has("action")) { "issue_comment payload missing 'action'" }
        require(payload.has("comment")) { "issue_comment payload missing 'comment' object" }
        return Result.success(Unit)
    }

    private fun handleRelease(payload: com.google.gson.JsonObject): Result<Unit> {
        require(payload.has("action")) { "release payload missing 'action'" }
        require(payload.has("release")) { "release payload missing 'release' object" }
        return Result.success(Unit)
    }
}
