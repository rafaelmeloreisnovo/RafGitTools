package com.rafgittools.execution

import com.rafgittools.library.processing.LibraryContentScope
import com.rafgittools.library.processing.LibraryLocalJob
import com.rafgittools.library.processing.LibraryNetworkPolicy
import com.rafgittools.library.processing.LibraryProcessingBudget
import com.rafgittools.library.processing.LibraryRigorLevel
import com.rafgittools.library.processing.LibrarySourceKind
import com.rafgittools.library.processing.LibrarySourceRef
import com.rafgittools.library.processing.LibraryJobStage
import com.rafgittools.library.processing.LibraryThermalPolicy
import java.security.MessageDigest

/**
 * Pure contract for the Android one-button execution gate.
 *
 * The UI may select a source, but the contract fixes authority, scope, stages,
 * resource ceilings and claim boundaries before the local executor is invoked.
 */
object ExecutionGateContractV1 {
    const val MAX_SOURCE_BYTES: Long = 16L * 1024L * 1024L
    const val MAX_WORKING_MEMORY_BYTES: Long = 32L * 1024L * 1024L
    const val CHECKPOINT_EVERY_BYTES: Long = 1024L * 1024L
    const val MIN_BATTERY_PERCENT: Int = 10
    const val OUTPUT_NAMESPACE: String = "execution-gate/v1"

    val stages: List<LibraryJobStage> = listOf(
        LibraryJobStage.DISCOVER,
        LibraryJobStage.INGEST,
        LibraryJobStage.EXTRACT,
        LibraryJobStage.VECTORIZE
    )

    fun buildJob(
        displayName: String,
        mediaType: String?,
        sizeBytes: Long,
        contentSha256: String,
        opaqueLocatorSha256: String,
        nowEpochMs: Long
    ): LibraryLocalJob {
        require(sizeBytes in 0..MAX_SOURCE_BYTES) { "source exceeds bounded V1 size" }
        require(contentSha256.matches(Regex("[0-9a-f]{64}"))) { "invalid content sha256" }
        require(opaqueLocatorSha256.matches(Regex("[0-9a-f]{64}"))) { "invalid locator sha256" }

        val sourceId = "saf:" + sha256("$opaqueLocatorSha256|$contentSha256")
        val idempotencyKey = sha256("execution-gate-v1|$sourceId|$contentSha256|STRUCTURAL")

        return LibraryLocalJob(
            jobId = "gate:" + idempotencyKey.take(24),
            source = LibrarySourceRef(
                sourceId = sourceId,
                sourceKind = LibrarySourceKind.LOCAL_SAF,
                opaqueLocatorHash = opaqueLocatorSha256,
                displayName = displayName,
                mediaType = mediaType,
                sizeBytes = sizeBytes,
                contentSha256 = contentSha256,
                modifiedTime = null,
                contentScope = LibraryContentScope.FULL_SOURCE,
                readOnly = true
            ),
            requestedStages = stages,
            rigor = LibraryRigorLevel.STRUCTURAL,
            budget = LibraryProcessingBudget(
                maxBytes = MAX_SOURCE_BYTES,
                maxWorkingMemoryBytes = MAX_WORKING_MEMORY_BYTES,
                maxItems = 1,
                maxAttempts = 2,
                checkpointEveryBytes = CHECKPOINT_EVERY_BYTES,
                minBatteryPercent = MIN_BATTERY_PERCENT,
                thermalPolicy = LibraryThermalPolicy.NORMAL,
                networkPolicy = LibraryNetworkPolicy.OFFLINE_ONLY
            ),
            outputNamespace = OUTPUT_NAMESPACE,
            idempotencyKey = idempotencyKey,
            createdAtEpochMs = nowEpochMs,
            claimAllowed = false
        )
    }

    fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
