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
            "termux.health" -> tokenVazio(tool, "runtime_transport_not_installed")
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
            jGitService.getDiff(repoPath, commitHash).fold(
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

    private fun tokenVazio(tool: String, reason: String): String =
        """{"tool":"$tool","status":"TOKEN_VAZIO","reason":"$reason"}"""

    private fun errorJson(code: String): String =
        """{"status":"ERROR","code":"$code"}"""
}
