package com.rafgittools.library.processing

import com.rafgittools.offline.OfflineQueue

data class RigorBiasedQueuedJobV1(
    val job: LibraryLocalJob,
    val biasInput: RigorBiasInputV1
)

data class RigorBiasedDequeuedJobV1(
    val original: RigorBiasedQueuedJobV1,
    val decision: RigorBiasDecisionV1,
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
    fun enqueue(item: RigorBiasedQueuedJobV1) = queue.enqueue(item)

    fun size(): Int = queue.size()

    fun snapshot(): List<RigorBiasedQueuedJobV1> = queue.snapshot()

    fun dequeueBest(): RigorBiasedDequeuedJobV1? {
        val selected = queue.dequeueBest { item ->
            RigorBiasEngineV1.evaluate(item.biasInput).priorityBiasQ16
        } ?: return null

        val decision = RigorBiasEngineV1.evaluate(selected.biasInput)
        val effective = RigorBiasEngineV1.raiseOnly(
            selected.job,
            decision
        )

        return RigorBiasedDequeuedJobV1(
            original = selected,
            decision = decision,
            effectiveJob = effective
        )
    }
}
