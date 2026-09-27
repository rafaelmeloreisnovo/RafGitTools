package com.rafgittools.library.processing

enum class LibraryJobStage {
    DISCOVER,
    INGEST,
    EXTRACT,
    VECTORIZE,
    RELATE,
    MATERIALIZE,
    DISTRIBUTE
}

enum class LibraryJobState {
    PENDING,
    RUNNING,
    CHECKPOINTED,
    SUCCEEDED,
    FAILED,
    BLOCKED_RESOURCE,
    BLOCKED_POLICY,
    TOKEN_VAZIO
}

enum class LibraryRigorLevel {
    QUICK,
    STRUCTURAL,
    MULTIMODAL,
    EVIDENCE
}

enum class LibraryThermalPolicy {
    COOL_ONLY,
    NORMAL,
    ALLOW_WARM
}

enum class LibraryNetworkPolicy {
    OFFLINE_ONLY,
    READ_ONLY_NETWORK
}

enum class LibrarySourceKind {
    GOOGLE_DRIVE_SAF,
    LOCAL_SAF,
    GITHUB,
    ZIP_ENTRY,
    GENERATED_FIXTURE
}

data class LibraryProcessingBudget(
    val maxBytes: Long,
    val maxWorkingMemoryBytes: Long,
    val maxItems: Int,
    val maxAttempts: Int,
    val checkpointEveryBytes: Long,
    val minBatteryPercent: Int,
    val thermalPolicy: LibraryThermalPolicy,
    val networkPolicy: LibraryNetworkPolicy
)

data class LibrarySourceRef(
    val sourceId: String,
    val sourceKind: LibrarySourceKind,
    val opaqueLocatorHash: String,
    val displayName: String,
    val mediaType: String?,
    val sizeBytes: Long?,
    val contentSha256: String?,
    val modifiedTime: String?,
    val readOnly: Boolean = true
)

data class LibraryLocalJob(
    val schemaVersion: String = "1.0.0",
    val jobId: String,
    val source: LibrarySourceRef,
    val requestedStages: List<LibraryJobStage>,
    val rigor: LibraryRigorLevel,
    val budget: LibraryProcessingBudget,
    val outputNamespace: String,
    val idempotencyKey: String,
    val createdAtEpochMs: Long,
    val claimAllowed: Boolean = false
)

data class LibraryJobCheckpoint(
    val jobId: String,
    val state: LibraryJobState,
    val completedStages: List<LibraryJobStage>,
    val bytesConsumed: Long,
    val itemsConsumed: Int,
    val attempt: Int,
    val descriptorSchemaVersion: String,
    val message: String?,
    val updatedAtEpochMs: Long,
    val claimAllowed: Boolean = false
)

data class ByteVectorV1(
    val sizeBytes: Int,
    val histogram16Q16: List<Int>,
    val meanByteQ16: Int,
    val nonZeroRatioQ16: Int,
    val transitionRatioQ16: Int,
    val sha256: String?
)

data class TextVectorV1(
    val tokenCount: Int,
    val uniqueTokenEstimate: Int,
    val tokenBuckets16Q16: List<Int>,
    val asciiLetterRatioQ16: Int,
    val digitRatioQ16: Int,
    val whitespaceRatioQ16: Int
)

data class VisualVectorV1(
    val width: Int,
    val height: Int,
    val intensityHistogram16Q16: List<Int>,
    val quadrantMeanQ16: List<Int>,
    val horizontalGradientQ16: Int,
    val verticalGradientQ16: Int,
    val diagonalGradientQ16: Int,
    val darkRatioQ16: Int
)

data class LibraryDescriptorBundle(
    val descriptorSchemaVersion: String = "1.0.0",
    val sourceId: String,
    val jobId: String,
    val rigor: LibraryRigorLevel,
    val byteVector: ByteVectorV1?,
    val textVector: TextVectorV1?,
    val visualVector: VisualVectorV1?,
    val evidenceNotes: List<String>,
    val tokenVazio: List<String>,
    val claimAllowed: Boolean = false
)

data class LibraryJobReceipt(
    val schemaVersion: String = "1.0.0",
    val jobId: String,
    val sourceId: String,
    val idempotencyKey: String,
    val requestedRigor: LibraryRigorLevel,
    val finalState: LibraryJobState,
    val completedStages: List<LibraryJobStage>,
    val descriptorSha256: String?,
    val bytesConsumed: Long,
    val itemsConsumed: Int,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val gaps: List<String>,
    val claimAllowed: Boolean = false
)
