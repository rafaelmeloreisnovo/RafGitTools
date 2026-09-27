package com.rafgittools.library.processing

data class LibraryRigorContract(
    val level: LibraryRigorLevel,
    val requireFullContentHash: Boolean,
    val requireByteVector: Boolean,
    val requireTextVectorWhenTextLike: Boolean,
    val requireVisualVectorWhenImageLike: Boolean,
    val allowSampledContent: Boolean,
    val requireReplayParameters: Boolean,
    val requireReceipt: Boolean
)

object LibraryRigorLens {
    fun contract(level: LibraryRigorLevel): LibraryRigorContract = when (level) {
        LibraryRigorLevel.QUICK -> LibraryRigorContract(
            level = level,
            requireFullContentHash = false,
            requireByteVector = true,
            requireTextVectorWhenTextLike = false,
            requireVisualVectorWhenImageLike = false,
            allowSampledContent = true,
            requireReplayParameters = false,
            requireReceipt = true
        )

        LibraryRigorLevel.STRUCTURAL -> LibraryRigorContract(
            level = level,
            requireFullContentHash = true,
            requireByteVector = true,
            requireTextVectorWhenTextLike = true,
            requireVisualVectorWhenImageLike = false,
            allowSampledContent = false,
            requireReplayParameters = true,
            requireReceipt = true
        )

        LibraryRigorLevel.MULTIMODAL -> LibraryRigorContract(
            level = level,
            requireFullContentHash = true,
            requireByteVector = true,
            requireTextVectorWhenTextLike = true,
            requireVisualVectorWhenImageLike = true,
            allowSampledContent = false,
            requireReplayParameters = true,
            requireReceipt = true
        )

        LibraryRigorLevel.EVIDENCE -> LibraryRigorContract(
            level = level,
            requireFullContentHash = true,
            requireByteVector = true,
            requireTextVectorWhenTextLike = true,
            requireVisualVectorWhenImageLike = true,
            allowSampledContent = false,
            requireReplayParameters = true,
            requireReceipt = true
        )
    }

    fun canSatisfy(
        job: LibraryLocalJob,
        availableBytes: Long?,
        availableWorkingMemoryBytes: Long,
        batteryPercent: Int
    ): LibraryJobState {
        if (!job.source.readOnly) return LibraryJobState.BLOCKED_POLICY
        if (job.claimAllowed) return LibraryJobState.BLOCKED_POLICY
        if (batteryPercent < job.budget.minBatteryPercent) {
            return LibraryJobState.BLOCKED_RESOURCE
        }
        if (availableWorkingMemoryBytes < job.budget.maxWorkingMemoryBytes) {
            return LibraryJobState.BLOCKED_RESOURCE
        }
        if (availableBytes != null && availableBytes > job.budget.maxBytes) {
            return LibraryJobState.BLOCKED_RESOURCE
        }
        if (job.budget.maxItems < 1 || job.budget.maxAttempts < 1) {
            return LibraryJobState.BLOCKED_POLICY
        }
        return LibraryJobState.PENDING
    }
}
