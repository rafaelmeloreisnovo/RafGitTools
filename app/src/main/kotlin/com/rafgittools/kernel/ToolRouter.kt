package com.rafgittools.kernel

import android.util.Log
import org.json.JSONObject

class ToolRouter(private val gate: GovernanceGate) {
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

    private fun handleGitStatus(@Suppress("UNUSED_PARAMETER") call: JSONObject): String {
        /* PENDING: real git.status via JGit (already in app dependencies) */
        return tokenVazio("git.status", "handler_pending")
    }

    private fun handleGitDiff(@Suppress("UNUSED_PARAMETER") call: JSONObject): String {
        /* PENDING: real git.diff via JGit */
        return tokenVazio("git.diff", "handler_pending")
    }

    private fun tokenVazio(tool: String, reason: String): String =
        """{"tool":"$tool","status":"TOKEN_VAZIO","reason":"$reason"}"""

    private fun errorJson(code: String): String =
        """{"status":"ERROR","code":"$code"}"""
}
