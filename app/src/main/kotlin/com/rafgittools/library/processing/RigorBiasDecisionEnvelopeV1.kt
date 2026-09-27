package com.rafgittools.library.processing

import java.security.MessageDigest

data class RigorBiasSignalProvenanceV1(
    val head: String,
    val sourceIds: List<String>
)

data class RigorBiasDecisionEnvelopeV1(
    val schemaVersion: String = "1.0.0",
    val inputSha256: String,
    val decision: RigorBiasDecisionV1,
    val signalProvenance: List<RigorBiasSignalProvenanceV1>,
    val forestProvenanceIds: List<String>,
    val sourceRegistryIds: List<String>,
    val claimAllowed: Boolean = false
)

object RigorBiasDecisionEnvelopeFactoryV1 {
    private val validHeads = setOf(
        "IDENTITY",
        "PROVENANCE",
        "STRUCTURE",
        "NOVELTY_GAP",
        "RELATION",
        "MULTIMODAL",
        "CONTRADICTION",
        "REPRODUCIBILITY",
        "FRESHNESS"
    )

    fun evaluate(
        input: RigorBiasInputV1,
        signalProvenance: Map<String, List<String>>,
        forestProvenanceIds: List<String> = emptyList(),
        sourceRegistryIds: List<String> = emptyList()
    ): RigorBiasDecisionEnvelopeV1 {
        val unknownKeys = signalProvenance.keys.filterNot(validHeads::contains)
        require(unknownKeys.isEmpty()) {
            "unknown rigor provenance heads: " + unknownKeys.joinToString(",")
        }

        val decision = RigorBiasEngineV1.evaluate(input)
        val normalized = validHeads.sorted().mapNotNull { head ->
            val ids = signalProvenance[head]
                .orEmpty()
                .filter(String::isNotBlank)
                .distinct()
                .sorted()
            if (ids.isEmpty()) null
            else RigorBiasSignalProvenanceV1(head, ids)
        }

        return RigorBiasDecisionEnvelopeV1(
            inputSha256 = sha256(canonicalInput(input)),
            decision = decision,
            signalProvenance = normalized,
            forestProvenanceIds = forestProvenanceIds
                .filter(String::isNotBlank)
                .distinct()
                .sorted(),
            sourceRegistryIds = sourceRegistryIds
                .filter(String::isNotBlank)
                .distinct()
                .sorted(),
            claimAllowed = false
        )
    }

    private fun canonicalInput(input: RigorBiasInputV1): String = buildString {
        append("rigor-bias-input-v1|")
        channel("IDENTITY", input.identityCompleteness)
        channel("PROVENANCE", input.provenanceCompleteness)
        channel("STRUCTURE", input.structureCompleteness)
        channel("NOVELTY_GAP", input.noveltyGapPressure)
        channel("RELATION", input.relationSupport)
        channel("MULTIMODAL", input.multimodalCompleteness)
        channel("CONTRADICTION", input.contradictionPressure)
        channel("REPRODUCIBILITY", input.reproducibilityCompleteness)
        channel("FRESHNESS", input.freshnessDriftPressure)
        append("forest=").append(input.forestPath.name).append('|')
        append("curated=").append(input.curatedTop).append('|')
        append("floor=").append(input.requestedFloor.name).append('|')
    }

    private fun StringBuilder.channel(
        name: String,
        channel: RigorChannelQ16
    ) {
        append(name).append(':')
        append(channel.state.name).append(':')
        if (channel.state == RigorChannelState.TOKEN_VAZIO) {
            append("TOKEN_VAZIO")
        } else {
            append(channel.valueQ16)
        }
        append('|')
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
