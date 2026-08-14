package com.rafgittools.kernel

import android.util.Log
import com.rafgittools.data.git.JGitService
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRouter @Inject constructor(
    private val gate: GovernanceGate,
    private val jGitService: JGitService,
) {
    private val TAG = "RafToolRouter"
    private val termuxHealthProbe = TermuxHealthProbe()

    fun route(toolCallJson: String, userAuthenticated: Boolean = false): String {
        val call = runCatching { JSONObject(toolCallJson) }.getOrElse {
            return errorJson("invalid_json")
        }

        val tool = call.optString("tool", "")
        if (tool.isEmpty()) return errorJson("missing_tool_field")

        val decision = gate.evaluate(tool, userAuthenticated)
        if (!decision.allowed) {
            Log.w(TAG, "DENIED: $tool — ${decision.reason}")
            return gate.buildDenialJson(tool, decision.reason)
        }

        Log.d(TAG, "routing: $tool")
        return when (tool) {
            "git.status"    -> handleGitStatus(call)
            "git.diff"      -> handleGitDiff(call)
            "git.log"       -> handleGitLog(call)
            "git.branch"    -> handleGitBranch(call)
            "git.commit"    -> handleGitCommitWriteProtected(call)
            "git.push"      -> handleGitPushQueued(call)
            "git.pull"      -> handleGitPullQueued(call)
            "termux.health" -> handleTermuxHealth(call)
            else            -> tokenVazio(tool, "handler_not_implemented")
        }
    }

    private fun handleGitStatus(call: JSONObject): String {
        val repoPath = call.optString("repoPath", "")
        if (repoPath.isBlank()) return errorJson("missing_repoPath")
        return runBlocking {
            jGitService.getStatus(repoPath).fold(
                onSuccess = { status ->
                    JSONObject().apply {
                        put("tool", "git.status")
                        put("status", "ok")
                        put("branch", status.branch)
                        put("hasUncommittedChanges", status.hasUncommittedChanges)
                        put("added", JSONArray(status.added))
                        put("changed", JSONArray(status.changed))
                        put("removed", JSONArray(status.removed))
                        put("modified", JSONArray(status.modified))
                        put("untracked", JSONArray(status.untracked))
                        put("conflicting", JSONArray(status.conflicting))
                    }.toString()
                },
                onFailure = { e ->
                    JSONObject().apply {
                        put("tool", "git.status")
                        put("status", "error")
                        put("reason", e.message ?: "unknown")
                    }.toString()
                }
            )
        }
    }

    private fun handleGitDiff(call: JSONObject): String {
        val repoPath = call.optString("repoPath", "")
        if (repoPath.isBlank()) return errorJson("missing_repoPath")
        val commitHash = call.optString("commitHash", "").ifBlank { null }
        return runBlocking {
            val diffResult = if (commitHash != null) {
                jGitService.getDiffBetweenCommits(repoPath, "$commitHash^", commitHash)
            } else {
                jGitService.getDiff(repoPath)
            }
            diffResult.fold(
                onSuccess = { diffs ->
                    JSONObject().apply {
                        put("tool", "git.diff")
                        put("status", "ok")
                        put("count", diffs.size)
                        put("diffs", JSONArray(diffs.map { diff ->
                            JSONObject().apply {
                                put("oldPath", diff.oldPath ?: JSONObject.NULL)
                                put("newPath", diff.newPath ?: JSONObject.NULL)
                                put("changeType", diff.changeType.name)
                                put("hunkCount", diff.hunks.size)
                            }
                        }))
                    }.toString()
                },
                onFailure = { e ->
                    JSONObject().apply {
                        put("tool", "git.diff")
                        put("status", "error")
                        put("reason", e.message ?: "unknown")
                    }.toString()
                }
            )
        }
    }

    private fun handleGitLog(call: JSONObject): String {
        val repoPath = call.optString("repoPath", "")
        if (repoPath.isBlank()) return errorJson("missing_repoPath")
        val limit = call.optInt("limit", 20).coerceIn(1, 100)
        return runBlocking {
            jGitService.getCommits(repoPath, branch = null, limit = limit).fold(
                onSuccess = { commits ->
                    JSONObject().apply {
                        put("tool", "git.log")
                        put("status", "ok")
                        put("count", commits.size)
                        put("commits", JSONArray(commits.map { commit ->
                            JSONObject().apply {
                                put("sha", commit.sha)
                                put("message", commit.message.lines().firstOrNull() ?: "")
                                put("author", commit.author.name)
                                put("timestamp", commit.timestamp)
                            }
                        }))
                    }.toString()
                },
                onFailure = { e ->
                    JSONObject().apply {
                        put("tool", "git.log")
                        put("status", "error")
                        put("reason", e.message ?: "unknown")
                    }.toString()
                }
            )
        }
    }

    private fun handleGitBranch(call: JSONObject): String {
        val repoPath = call.optString("repoPath", "")
        if (repoPath.isBlank()) return errorJson("missing_repoPath")
        return runBlocking {
            jGitService.getBranches(repoPath).fold(
                onSuccess = { branches ->
                    JSONObject().apply {
                        put("tool", "git.branch")
                        put("status", "ok")
                        put("count", branches.size)
                        put("branches", JSONArray(branches.map { branch ->
                            JSONObject().apply {
                                put("name", branch.name)
                                put("shortName", branch.shortName)
                                put("isCurrent", branch.isCurrent)
                                put("isRemote", branch.isRemote)
                            }
                        }))
                    }.toString()
                },
                onFailure = { e ->
                    JSONObject().apply {
                        put("tool", "git.branch")
                        put("status", "error")
                        put("reason", e.message ?: "unknown")
                    }.toString()
                }
            )
        }
    }

    private fun handleGitCommitWriteProtected(@Suppress("UNUSED_PARAMETER") call: JSONObject): String =
        JSONObject().apply {
            put("tool", "git.commit")
            put("status", "WRITE_PROTECTED")
            put("reason", "git.commit is write-protected in kernel mode — use the app UI to commit")
        }.toString()

    private fun handleGitPushQueued(call: JSONObject): String {
        val repoPath = call.optString("repoPath", "")
        if (repoPath.isBlank()) return errorJson("missing_repoPath")
        return JSONObject().apply {
            put("tool", "git.push")
            put("status", "queued")
            put("note", "Push queued for background sync — use the app UI or SyncWorker will execute on next network opportunity")
        }.toString()
    }

    private fun handleGitPullQueued(call: JSONObject): String {
        val repoPath = call.optString("repoPath", "")
        if (repoPath.isBlank()) return errorJson("missing_repoPath")
        return JSONObject().apply {
            put("tool", "git.pull")
            put("status", "queued")
            put("note", "Pull queued for background sync — use the app UI or SyncWorker will execute on next network opportunity")
        }.toString()
    }

    private fun handleTermuxHealth(call: JSONObject): String {
        val endpoint = call.optString("endpoint", TermuxHealthProbe.DEFAULT_ENDPOINT)
        val timeoutMs = call.optInt("timeout_ms", TermuxHealthProbe.DEFAULT_TIMEOUT_MS)
        val result = termuxHealthProbe.probe(endpoint, timeoutMs)

        return JSONObject().apply {
            put("tool", "termux.health")
            put("status", result.state.name)
            put("transport", result.transport)
            put("endpoint", result.endpoint)
            result.responseCode?.let { put("response_code", it) }
            result.latencyMs?.let { put("latency_ms", it) }
            result.body?.let { put("body", it) }
            result.reason?.let { put("reason", it) }
        }.toString()
    }

    private fun tokenVazio(tool: String, reason: String): String =
        """{"tool":"$tool","status":"TOKEN_VAZIO","reason":"$reason"}"""

    private fun errorJson(code: String): String =
        """{"status":"ERROR","code":"$code"}"""
}
