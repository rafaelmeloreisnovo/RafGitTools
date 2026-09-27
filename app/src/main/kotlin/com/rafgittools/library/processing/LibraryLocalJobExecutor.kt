package com.rafgittools.library.processing

import com.rafgittools.offline.OfflineQueue
import java.security.MessageDigest

data class LibraryJobEnvironment(
    val availableWorkingMemoryBytes: Long,
    val batteryPercent: Int,
    val nowEpochMs: Long
)

data class LibraryJobInput(
    val bytes: ByteArray?,
    val text: String? = null,
    val gray8Pixels: ByteArray? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null
)

data class LibraryJobExecutionResult(
    val receipt: LibraryJobReceipt,
    val checkpoint: LibraryJobCheckpoint,
    val descriptors: LibraryDescriptorBundle?
)

interface LibraryJobStateSink {
    fun checkpoint(value: LibraryJobCheckpoint)
    fun receipt(value: LibraryJobReceipt)
}

object NoOpLibraryJobStateSink : LibraryJobStateSink {
    override fun checkpoint(value: LibraryJobCheckpoint) = Unit
    override fun receipt(value: LibraryJobReceipt) = Unit
}

class LibraryLocalJobExecutor(
    private val stateSink: LibraryJobStateSink = NoOpLibraryJobStateSink
) {
    fun execute(
        job: LibraryLocalJob,
        input: LibraryJobInput,
        environment: LibraryJobEnvironment,
        attempt: Int = 1
    ): LibraryJobExecutionResult {
        val started = environment.nowEpochMs
        val availableBytes = input.bytes?.size?.toLong() ?: job.source.sizeBytes
        val preflight = LibraryRigorLens.canSatisfy(
            job = job,
            availableBytes = availableBytes,
            availableWorkingMemoryBytes = environment.availableWorkingMemoryBytes,
            batteryPercent = environment.batteryPercent
        )

        if (preflight != LibraryJobState.PENDING) {
            return blocked(job, started, environment.nowEpochMs, attempt, preflight)
        }

        if (attempt > job.budget.maxAttempts) {
            return blocked(
                job,
                started,
                environment.nowEpochMs,
                attempt,
                LibraryJobState.BLOCKED_POLICY,
                "MAX_ATTEMPTS_EXCEEDED"
            )
        }

        val rigor = LibraryRigorLens.contract(job.rigor)
        val gaps = mutableListOf<String>()
        val completed = mutableListOf<LibraryJobStage>()
        var bytesConsumed = 0L

        completed += LibraryJobStage.DISCOVER
        completed += LibraryJobStage.INGEST

        val bytes = input.bytes
        val byteVector = if (rigor.requireByteVector) {
            if (bytes == null) {
                gaps += "BYTE_SOURCE_TOKEN_VAZIO"
                null
            } else {
                bytesConsumed = bytes.size.toLong()
                LocalDescriptorEngine.byteVector(
                    bytes = bytes,
                    includeSha256 = rigor.requireFullContentHash
                )
            }
        } else null

        val isTextLike = job.source.mediaType?.startsWith("text/") == true ||
            job.source.mediaType == "application/json" ||
            job.source.mediaType == "application/xml"

        val textVector = if (rigor.requireTextVectorWhenTextLike && isTextLike) {
            val text = input.text ?: bytes?.toString(Charsets.UTF_8)
            if (text == null) {
                gaps += "TEXT_DECODE_TOKEN_VAZIO"
                null
            } else {
                LocalDescriptorEngine.textVector(text)
            }
        } else null

        val isImageLike = job.source.mediaType?.startsWith("image/") == true
        val visualVector = if (rigor.requireVisualVectorWhenImageLike && isImageLike) {
            val pixels = input.gray8Pixels
            val width = input.imageWidth
            val height = input.imageHeight
            if (pixels == null || width == null || height == null) {
                gaps += "VISUAL_DECODE_TOKEN_VAZIO"
                null
            } else {
                LocalDescriptorEngine.visualVectorGray8(pixels, width, height)
            }
        } else null

        completed += LibraryJobStage.EXTRACT
        completed += LibraryJobStage.VECTORIZE

        val requiredGaps = mutableListOf<String>()
        if (rigor.requireByteVector && byteVector == null) {
            requiredGaps += "REQUIRED_BYTE_VECTOR_TOKEN_VAZIO"
        }
        if (rigor.requireFullContentHash && byteVector?.sha256 == null) {
            requiredGaps += "REQUIRED_FULL_HASH_TOKEN_VAZIO"
        }
        if (rigor.requireTextVectorWhenTextLike && isTextLike && textVector == null) {
            requiredGaps += "REQUIRED_TEXT_VECTOR_TOKEN_VAZIO"
        }
        if (rigor.requireVisualVectorWhenImageLike && isImageLike && visualVector == null) {
            requiredGaps += "REQUIRED_VISUAL_VECTOR_TOKEN_VAZIO"
        }

        if (requiredGaps.isNotEmpty()) {
            return checkpointed(
                job = job,
                started = started,
                finished = environment.nowEpochMs,
                attempt = attempt,
                completed = completed,
                bytesConsumed = bytesConsumed,
                gaps = (gaps + requiredGaps).distinct()
            )
        }

        val descriptors = LibraryDescriptorBundle(
            sourceId = job.source.sourceId,
            jobId = job.jobId,
            rigor = job.rigor,
            byteVector = byteVector,
            textVector = textVector,
            visualVector = visualVector,
            evidenceNotes = listOf(
                "DETERMINISTIC_DESCRIPTOR_V1",
                "SOURCE_READ_ONLY=" + job.source.readOnly
            ),
            tokenVazio = gaps.distinct()
        )

        val descriptorSha = sha256(canonicalDescriptor(descriptors))
        val requested = job.requestedStages.toSet()
        val unimplementedStageGaps = buildList {
            if (LibraryJobStage.RELATE in requested) add("RELATE_EXECUTOR_NOT_IMPLEMENTED")
            if (LibraryJobStage.MATERIALIZE in requested) add("MATERIALIZE_EXECUTOR_NOT_IMPLEMENTED")
            if (LibraryJobStage.DISTRIBUTE in requested) add("DISTRIBUTE_EXECUTOR_NOT_IMPLEMENTED")
        }

        if (unimplementedStageGaps.isNotEmpty()) {
            return checkpointed(
                job = job,
                started = started,
                finished = environment.nowEpochMs,
                attempt = attempt,
                completed = completed,
                bytesConsumed = bytesConsumed,
                gaps = (gaps + unimplementedStageGaps).distinct(),
                descriptors = descriptors,
                descriptorSha256 = descriptorSha
            )
        }

        val receipt = LibraryJobReceipt(
            jobId = job.jobId,
            sourceId = job.source.sourceId,
            idempotencyKey = job.idempotencyKey,
            requestedRigor = job.rigor,
            finalState = LibraryJobState.SUCCEEDED,
            completedStages = completed.distinct(),
            descriptorSha256 = descriptorSha,
            bytesConsumed = bytesConsumed,
            itemsConsumed = 1,
            startedAtEpochMs = started,
            finishedAtEpochMs = environment.nowEpochMs,
            gaps = gaps.distinct()
        )
        val checkpoint = LibraryJobCheckpoint(
            jobId = job.jobId,
            state = LibraryJobState.SUCCEEDED,
            completedStages = receipt.completedStages,
            bytesConsumed = bytesConsumed,
            itemsConsumed = 1,
            attempt = attempt,
            descriptorSchemaVersion = descriptors.descriptorSchemaVersion,
            message = null,
            updatedAtEpochMs = environment.nowEpochMs
        )
        stateSink.checkpoint(checkpoint)
        stateSink.receipt(receipt)
        return LibraryJobExecutionResult(receipt, checkpoint, descriptors)
    }

    private fun blocked(
        job: LibraryLocalJob,
        started: Long,
        finished: Long,
        attempt: Int,
        state: LibraryJobState,
        message: String = state.name
    ): LibraryJobExecutionResult {
        val checkpoint = LibraryJobCheckpoint(
            jobId = job.jobId,
            state = state,
            completedStages = emptyList(),
            bytesConsumed = 0,
            itemsConsumed = 0,
            attempt = attempt,
            descriptorSchemaVersion = "1.0.0",
            message = message,
            updatedAtEpochMs = finished
        )
        val receipt = LibraryJobReceipt(
            jobId = job.jobId,
            sourceId = job.source.sourceId,
            idempotencyKey = job.idempotencyKey,
            requestedRigor = job.rigor,
            finalState = state,
            completedStages = emptyList(),
            descriptorSha256 = descriptorSha256,
            bytesConsumed = 0,
            itemsConsumed = 0,
            startedAtEpochMs = started,
            finishedAtEpochMs = finished,
            gaps = listOf(message)
        )
        stateSink.checkpoint(checkpoint)
        stateSink.receipt(receipt)
        return LibraryJobExecutionResult(receipt, checkpoint, null)
    }

    private fun checkpointed(
        job: LibraryLocalJob,
        started: Long,
        finished: Long,
        attempt: Int,
        completed: List<LibraryJobStage>,
        bytesConsumed: Long,
        gaps: List<String>,
        descriptors: LibraryDescriptorBundle? = null,
        descriptorSha256: String? = null
    ): LibraryJobExecutionResult {
        val checkpoint = LibraryJobCheckpoint(
            jobId = job.jobId,
            state = LibraryJobState.CHECKPOINTED,
            completedStages = completed.distinct(),
            bytesConsumed = bytesConsumed,
            itemsConsumed = 1,
            attempt = attempt,
            descriptorSchemaVersion = "1.0.0",
            message = gaps.distinct().joinToString(","),
            updatedAtEpochMs = finished
        )
        val receipt = LibraryJobReceipt(
            jobId = job.jobId,
            sourceId = job.source.sourceId,
            idempotencyKey = job.idempotencyKey,
            requestedRigor = job.rigor,
            finalState = LibraryJobState.CHECKPOINTED,
            completedStages = completed.distinct(),
            descriptorSha256 = null,
            bytesConsumed = bytesConsumed,
            itemsConsumed = 1,
            startedAtEpochMs = started,
            finishedAtEpochMs = finished,
            gaps = gaps.distinct()
        )
        stateSink.checkpoint(checkpoint)
        stateSink.receipt(receipt)
        return LibraryJobExecutionResult(receipt, checkpoint, descriptors)
    }

    private fun canonicalDescriptor(value: LibraryDescriptorBundle): String = buildString {
        append(value.descriptorSchemaVersion).append('|')
        append(value.sourceId).append('|')
        append(value.jobId).append('|')
        append(value.rigor.name).append('|')
        append(value.byteVector).append('|')
        append(value.textVector).append('|')
        append(value.visualVector).append('|')
        append(value.evidenceNotes.sorted()).append('|')
        append(value.tokenVazio.sorted()).append('|')
        append("claim_allowed=false")
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}

class LibraryProcessingQueue(
    private val queue: OfflineQueue<LibraryLocalJob>
) {
    fun enqueue(job: LibraryLocalJob) = queue.enqueue(job)
    fun snapshot(): List<LibraryLocalJob> = queue.snapshot()

    fun runNext(
        executor: LibraryLocalJobExecutor,
        inputResolver: (LibraryLocalJob) -> LibraryJobInput?,
        environmentResolver: (LibraryLocalJob) -> LibraryJobEnvironment
    ): LibraryJobExecutionResult? {
        val job = queue.dequeue() ?: return null
        val input = inputResolver(job)
        if (input == null) {
            queue.enqueue(job)
            return null
        }

        val result = executor.execute(
            job = job,
            input = input,
            environment = environmentResolver(job)
        )

        if (result.receipt.finalState == LibraryJobState.CHECKPOINTED ||
            result.receipt.finalState == LibraryJobState.BLOCKED_RESOURCE
        ) {
            queue.enqueue(job)
        }
        return result
    }
}
