package com.rafgittools.kernel

import android.content.Context
import android.util.Log
import org.json.JSONObject

class GovernanceGate(context: Context) {
    private val TAG = "RafGovernance"
    private val registry: JSONObject

    init {
        val json = context.assets
            .open("kernel/protocol/tool_registry.json")
            .bufferedReader()
            .use { it.readText() }
        registry = JSONObject(json)
        Log.i(TAG, "governance gate loaded: ${registry.optJSONObject("tools")?.length() ?: 0} tools registered")
    }

    data class Decision(val allowed: Boolean, val reason: String)

    fun evaluate(toolName: String, userAuthenticated: Boolean = false): Decision {
        val tools = registry.optJSONObject("tools")
            ?: return Decision(false, "registry_missing")
        val entry = tools.optJSONObject(toolName)
            ?: return Decision(false, "tool_not_registered:$toolName")
        if (!entry.optBoolean("allowed", false))
            return Decision(false, "tool_not_allowed:$toolName")
        if (entry.optBoolean("requires_auth", false) && !userAuthenticated)
            return Decision(false, "auth_required:$toolName")
        return Decision(true, "ok")
    }

    fun buildDenialJson(toolName: String, reason: String): String =
        """{"tool":"$toolName","status":"DENIED","reason":"$reason"}"""
}
