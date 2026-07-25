package com.rafgittools.rafgitfs.assurance

enum class RafGitFsMetricState { PASS, FAIL, TOKEN_VAZIO }

data class RafGitFsMetricSample(
    val name: String,
    val observedMillis: Double?,
    val budgetMillis: Double,
    val sampleCount: Int,
    val environment: String?
)

data class RafGitFsMetricAssessment(
    val name: String,
    val state: RafGitFsMetricState,
    val observedMillis: Double?,
    val budgetMillis: Double,
    val reason: String
)

object RafGitFsPerformanceBudget {
    /** Provisional engineering targets, not measured device claims. */
    val targetsMillis: Map<String, Double> = linkedMapOf(
        "local_tree_render_p95" to 750.0,
        "cached_file_read_p95" to 250.0,
        "plan_1000_entries" to 150.0,
        "sha256_10_mib" to 2_000.0,
        "room_job_transition_p95" to 100.0
    )

    fun assess(sample: RafGitFsMetricSample): RafGitFsMetricAssessment {
        if (sample.observedMillis == null || sample.sampleCount <= 0 || sample.environment.isNullOrBlank()) {
            return RafGitFsMetricAssessment(
                sample.name,
                RafGitFsMetricState.TOKEN_VAZIO,
                sample.observedMillis,
                sample.budgetMillis,
                "MEASUREMENT_RECEIPT_INCOMPLETE"
            )
        }
        return RafGitFsMetricAssessment(
            sample.name,
            if (sample.observedMillis <= sample.budgetMillis) RafGitFsMetricState.PASS else RafGitFsMetricState.FAIL,
            sample.observedMillis,
            sample.budgetMillis,
            if (sample.observedMillis <= sample.budgetMillis) "WITHIN_PROVISIONAL_BUDGET" else "BUDGET_EXCEEDED"
        )
    }

    fun unmeasuredBaseline(): List<RafGitFsMetricAssessment> = targetsMillis.map { (name, budget) ->
        assess(RafGitFsMetricSample(name, null, budget, 0, null))
    }
}
