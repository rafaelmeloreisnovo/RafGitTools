package com.rafgittools.setupwizard

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

class SetupWizardCustodyLedger(context: Context) {
    private val directory = File(context.filesDir, "setup_wizard")
    private val ledger = File(directory, "custody.jsonl")

    init {
        directory.mkdirs()
    }

    fun append(stepId: String, decision: WizardDecision?, eventType: String): String {
        require(stepId in SetupWizardCatalog.steps.map { it.id } || stepId == "WIZARD")
        require(eventType in setOf("DECISION", "COMPLETE", "LOCAL_ROLLBACK", "REVIEW"))
        val timestampMs = System.currentTimeMillis()
        val previousHash = readLastHash()
        val decisionName = decision?.name ?: "TOKEN_VAZIO"
        val canonical = listOf(
            "rafgittools.setup-wizard-custody.v1",
            timestampMs.toString(),
            stepId,
            decisionName,
            eventType,
            previousHash
        ).joinToString("|")
        val hash = sha256(canonical)

        val record = JSONObject()
            .put("schema", "rafgittools.setup-wizard-custody.v1")
            .put("timestamp_ms", timestampMs)
            .put("step_id", stepId)
            .put("decision", decisionName)
            .put("event_type", eventType)
            .put("previous_hash", previousHash)
            .put("hash", hash)
            .put("secret_values_recorded", false)
            .put("claim_allowed", false)

        ledger.appendText(record.toString() + "\n", Charsets.UTF_8)
        return hash
    }

    fun records(): List<String> =
        if (ledger.exists()) ledger.readLines(Charsets.UTF_8).filter { it.isNotBlank() } else emptyList()

    private fun readLastHash(): String {
        val last = records().lastOrNull() ?: return "GENESIS"
        return runCatching { JSONObject(last).optString("hash", "GENESIS") }.getOrDefault("GENESIS")
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
