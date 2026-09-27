package com.rafgittools.library.processing

private const val RIGOR_Q16_ONE = 65536

enum class RigorChannelState {
    KNOWN,
    TOKEN_VAZIO
}

data class RigorChannelQ16(
    val valueQ16: Int,
    val state: RigorChannelState
) {
    init {
        require(valueQ16 in 0..RIGOR_Q16_ONE) { "Q16 channel out of range" }
    }

    companion object {
        fun known(valueQ16: Int) = RigorChannelQ16(valueQ16, RigorChannelState.KNOWN)
        fun tokenVazio() = RigorChannelQ16(0, RigorChannelState.TOKEN_VAZIO)
    }
}

enum class RmrCtiForestPath {
    PROCESSUAL,
    VOID,
    FORGOTTEN,
    MENOSPREZADO,
    URGENT,
    TOKEN_VAZIO
}

data class RigorBiasInputV1(
    val identityCompleteness: RigorChannelQ16,
    val provenanceCompleteness: RigorChannelQ16,
    val structureCompleteness: RigorChannelQ16,
    val noveltyGapPressure: RigorChannelQ16,
    val relationSupport: RigorChannelQ16,
    val multimodalCompleteness: RigorChannelQ16,
    val contradictionPressure: RigorChannelQ16,
    val reproducibilityCompleteness: RigorChannelQ16,
    val freshnessDriftPressure: RigorChannelQ16,
    val forestPath: RmrCtiForestPath = RmrCtiForestPath.TOKEN_VAZIO,
    val curatedTop: Boolean = false,
    val requestedFloor: LibraryRigorLevel = LibraryRigorLevel.QUICK
)

data class RigorHeadContributionV1(
    val head: String,
    val sourceValueQ16: Int?,
    val pressureQ16: Int?,
    val weightQ16: Int,
    val weightedPressure: Long?,
    val state: RigorChannelState
)

data class RigorBiasDecisionV1(
    val schemaVersion: String = "1.0.0",
    val priorityBiasQ16: Int,
    val rigorPressureQ16: Int?,
    val recommendedRigor: LibraryRigorLevel,
    val requestedFloor: LibraryRigorLevel,
    val unknownHeads: List<String>,
    val contributions: List<RigorHeadContributionV1>,
    val reasons: List<String>,
    val evidencePromotionAllowed: Boolean = false,
    val claimAllowed: Boolean = false
)

object RigorBiasEngineV1 {
    private data class HeadSpec(
        val name: String,
        val channel: RigorChannelQ16,
        val weightQ16: Int,
        val pressureIsDeficit: Boolean
    )

    // Sum = 65536. The weights are a versioned engineering policy, not a
    // scientific constant.
    private const val W_IDENTITY = 8192
    private const val W_PROVENANCE = 8192
    private const val W_STRUCTURE = 7168
    private const val W_NOVELTY = 7168
    private const val W_RELATION = 7168
    private const val W_MULTIMODAL = 7168
    private const val W_CONTRADICTION = 8192
    private const val W_REPRODUCIBILITY = 8192
    private const val W_FRESHNESS = 4096

    fun evaluate(input: RigorBiasInputV1): RigorBiasDecisionV1 {
        val heads = listOf(
            HeadSpec("IDENTITY", input.identityCompleteness, W_IDENTITY, true),
            HeadSpec("PROVENANCE", input.provenanceCompleteness, W_PROVENANCE, true),
            HeadSpec("STRUCTURE", input.structureCompleteness, W_STRUCTURE, true),
            HeadSpec("NOVELTY_GAP", input.noveltyGapPressure, W_NOVELTY, false),
            HeadSpec("RELATION", input.relationSupport, W_RELATION, true),
            HeadSpec("MULTIMODAL", input.multimodalCompleteness, W_MULTIMODAL, true),
            HeadSpec("CONTRADICTION", input.contradictionPressure, W_CONTRADICTION, false),
            HeadSpec(
                "REPRODUCIBILITY",
                input.reproducibilityCompleteness,
                W_REPRODUCIBILITY,
                true
            ),
            HeadSpec("FRESHNESS", input.freshnessDriftPressure, W_FRESHNESS, false)
        )

        var weightedSum = 0L
        var knownWeight = 0L
        val unknown = mutableListOf<String>()
        val contributions = heads.map { head ->
            if (head.channel.state == RigorChannelState.TOKEN_VAZIO) {
                unknown += head.name
                RigorHeadContributionV1(
                    head = head.name,
                    sourceValueQ16 = null,
                    pressureQ16 = null,
                    weightQ16 = head.weightQ16,
                    weightedPressure = null,
                    state = head.channel.state
                )
            } else {
                val pressure = if (head.pressureIsDeficit) {
                    RIGOR_Q16_ONE - head.channel.valueQ16
                } else {
                    head.channel.valueQ16
                }
                val weighted = pressure.toLong() * head.weightQ16.toLong()
                weightedSum += weighted
                knownWeight += head.weightQ16.toLong()
                RigorHeadContributionV1(
                    head = head.name,
                    sourceValueQ16 = head.channel.valueQ16,
                    pressureQ16 = pressure,
                    weightQ16 = head.weightQ16,
                    weightedPressure = weighted,
                    state = head.channel.state
                )
            }
        }

        val rigorPressure = if (knownWeight == 0L) {
            null
        } else {
            (weightedSum / knownWeight)
                .coerceIn(0L, RIGOR_Q16_ONE.toLong())
                .toInt()
        }

        var recommended = rigorPressure?.let(::rigorFromPressure)
            ?: LibraryRigorLevel.EVIDENCE
        val reasons = mutableListOf<String>()

        if (rigorPressure == null) {
            reasons += "ALL_NINE_HEADS_TOKEN_VAZIO"
        }

        val unknownFloor = when {
            unknown.size >= 6 -> LibraryRigorLevel.EVIDENCE
            unknown.size >= 3 -> LibraryRigorLevel.MULTIMODAL
            unknown.isNotEmpty() -> LibraryRigorLevel.STRUCTURAL
            else -> LibraryRigorLevel.QUICK
        }
        recommended = maxRigor(recommended, unknownFloor)
        if (unknown.isNotEmpty()) {
            reasons += "TOKEN_VAZIO_HEADS=" + unknown.joinToString(",")
        }

        val criticalUnknown = listOf("IDENTITY", "PROVENANCE", "REPRODUCIBILITY")
            .filter(unknown::contains)
        if (criticalUnknown.isNotEmpty()) {
            recommended = maxRigor(recommended, LibraryRigorLevel.EVIDENCE)
            reasons += "CRITICAL_EVIDENCE_HEAD_TOKEN_VAZIO=" +
                criticalUnknown.joinToString(",")
        }

        recommended = maxRigor(recommended, input.requestedFloor)
        if (recommended == input.requestedFloor &&
            input.requestedFloor != LibraryRigorLevel.QUICK
        ) {
            reasons += "EXPLICIT_RIGOR_FLOOR_PRESERVED"
        }

        val priority = priorityBias(input)
        reasons += "FOREST_PATH=" + input.forestPath.name
        if (input.curatedTop) reasons += "CURATED_TOP_BONUS"

        return RigorBiasDecisionV1(
            priorityBiasQ16 = priority,
            rigorPressureQ16 = rigorPressure,
            recommendedRigor = recommended,
            requestedFloor = input.requestedFloor,
            unknownHeads = unknown.sorted(),
            contributions = contributions,
            reasons = reasons.distinct(),
            evidencePromotionAllowed = false,
            claimAllowed = false
        )
    }

    fun raiseOnly(
        job: LibraryLocalJob,
        decision: RigorBiasDecisionV1
    ): LibraryLocalJob =
        job.copy(rigor = maxRigor(job.rigor, decision.recommendedRigor))

    private fun rigorFromPressure(valueQ16: Int): LibraryRigorLevel = when {
        valueQ16 < 16384 -> LibraryRigorLevel.QUICK
        valueQ16 < 32768 -> LibraryRigorLevel.STRUCTURAL
        valueQ16 < 49152 -> LibraryRigorLevel.MULTIMODAL
        else -> LibraryRigorLevel.EVIDENCE
    }

    private fun priorityBias(input: RigorBiasInputV1): Int {
        var value = when (input.forestPath) {
            RmrCtiForestPath.URGENT -> 49152
            RmrCtiForestPath.MENOSPREZADO -> 32768
            RmrCtiForestPath.FORGOTTEN -> 24576
            RmrCtiForestPath.VOID -> 20480
            RmrCtiForestPath.PROCESSUAL -> 16384
            RmrCtiForestPath.TOKEN_VAZIO -> 8192
        }

        if (input.curatedTop) value += 8192

        if (input.noveltyGapPressure.state == RigorChannelState.KNOWN) {
            value += input.noveltyGapPressure.valueQ16 / 8
        }
        if (input.contradictionPressure.state == RigorChannelState.KNOWN) {
            value += input.contradictionPressure.valueQ16 / 8
        }

        return value.coerceIn(0, RIGOR_Q16_ONE)
    }

    private fun maxRigor(
        left: LibraryRigorLevel,
        right: LibraryRigorLevel
    ): LibraryRigorLevel =
        if (rank(left) >= rank(right)) left else right

    private fun rank(value: LibraryRigorLevel): Int = when (value) {
        LibraryRigorLevel.QUICK -> 0
        LibraryRigorLevel.STRUCTURAL -> 1
        LibraryRigorLevel.MULTIMODAL -> 2
        LibraryRigorLevel.EVIDENCE -> 3
    }
}
