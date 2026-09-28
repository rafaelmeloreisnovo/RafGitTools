package com.rafgittools.setupwizard

import android.content.Context

class SetupWizardPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("raf_setup_wizard_v1", Context.MODE_PRIVATE)

    fun decision(stepId: String): WizardDecision? =
        prefs.getString("decision_${stepId}", null)?.let {
            runCatching { WizardDecision.valueOf(it) }.getOrNull()
        }

    fun setDecision(stepId: String, decision: WizardDecision) {
        prefs.edit().putString("decision_${stepId}", decision.name).apply()
    }

    fun allDecisions(): Map<String, WizardDecision> =
        SetupWizardCatalog.steps.mapNotNull { step ->
            decision(step.id)?.let { step.id to it }
        }.toMap()

    fun setCompleted(completed: Boolean) {
        prefs.edit().putBoolean("completed", completed).apply()
    }

    fun isCompleted(): Boolean = prefs.getBoolean("completed", false)

    fun resetLocalDecisions() {
        prefs.edit().clear().apply()
    }
}
