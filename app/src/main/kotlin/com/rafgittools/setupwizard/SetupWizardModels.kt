package com.rafgittools.setupwizard

enum class WizardDecision {
    AGREE,
    DISAGREE,
    LATER
}

enum class WizardRisk {
    LOW,
    MEDIUM,
    HIGH
}

data class WizardStep(
    val id: String,
    val title: String,
    val summary: String,
    val enables: List<String>,
    val risks: List<String>,
    val dataTouched: List<String>,
    val rollback: String,
    val zeroTrustRule: String,
    val risk: WizardRisk
)
