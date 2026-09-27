package com.rafgittools.library.processing

import com.rafgittools.offline.OfflineQueue

data class RigorBiasedQueuedJobV1(
    val job: LibraryLocalJob,
    val biasInput: RigorBiasInputV1,
    val signalProvenance: Map<String, List<String>> = emptyMap(),
    val forestProvenanceIds: List<String> = emptyList(),
    val sourceRegistryIds: List<String> = emptyList()
)

data class RigorBiasedDequeuedJobV1(
    val original: RigorBiasedQueuedJobV1,
    val decision: RigorBiasDecisionV1,
    val decisionEnvelope: RigorBiasDecisionEnvelopeV1,
    val effectiveJob: LibraryLocalJob
)

/**
 * Thin scheduling adapter over the existing OfflineQueue.
 *
 * Bias only selects execution order and may raise processing rigor.
 * Resource/policy gates remain enforced by LibraryLocalJobExecutor.
 */
class RigorBiasedLibraryQueueV1(
    private val queue: OfflineQueue<RigorBiasedQueuedJobV1>
) {
    fun enqueue(item: RigorBiasedQueuedJobV1) {
        validateProvenance(item)
        queue.enqueue(item)
    }

    fun size(): Int = queue.size()

    fun snapshot(): List<RigorBiasedQueuedJobV1> = queue.snapshot()

    fun dequeueBest(): RigorBiasedDequeuedJobV1? {
        val selected = queue.dequeueBest { item ->
            RigorBiasEngineV1.evaluate(item.biasInput).priorityBiasQ16
        } ?: return null

        val envelope = RigorBiasDecisionEnvelopeFactoryV1.evaluate(
            input = selected.biasInput,
            signalProvenance = selected.signalProvenance,
            forestProvenanceIds = selected.forestProvenanceIds,
            sourceRegistryIds = selected.sourceRegistryIds
        )
        val decision = envelope.decision
        val effective = RigorBiasEngineV1.raiseOnly(
            selected.job,
            decision
        )

        return RigorBiasedDequeuedJobV1(
            original = selected,
            decision = decision,
            decisionEnvelope = envelope,
            effectiveJob = effective
        )
    }

    private fun validateProvenance(item: RigorBiasedQueuedJobV1) {
        val channels = mapOf(
            "IDENTITY" to item.biasInput.identityCompleteness,
            "PROVENANCE" to item.biasInput.provenanceCompleteness,
            "STRUCTURE" to item.biasInput.structureCompleteness,
            "NOVELTY_GAP" to item.biasInput.noveltyGapPressure,
            "RELATION" to item.biasInput.relationSupport,
            "MULTIMODAL" to item.biasInput.multimodalCompleteness,
            "CONTRADICTION" to item.biasInput.contradictionPressure,
            "REPRODUCIBILITY" to item.biasInput.reproducibilityCompleteness,
            "FRESHNESS" to item.biasInput.freshnessDriftPressure
        )

        channels.forEach { (head, channel) ->
            if (channel.state == RigorChannelState.KNOWN) {
                val ids = item.signalProvenance[head].orEmpty()
                    .filter(String::isNotBlank)
                require(ids.isNotEmpty()) {
                    "known rigor head lacks provenance: " + head
                }
            }
        }

        if (item.biasInput.forestPath != RmrCtiForestPath.TOKEN_VAZIO) {
            require(item.forestProvenanceIds.any(String::isNotBlank)) {
                "known forest path lacks provenance"
            }
        }
    }
}
