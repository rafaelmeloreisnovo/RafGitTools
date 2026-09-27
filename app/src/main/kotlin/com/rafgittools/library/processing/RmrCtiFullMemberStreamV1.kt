package com.rafgittools.library.processing

import com.rafgittools.library.AuthorityKind
import com.rafgittools.library.LibraryAccessClass
import com.rafgittools.library.LibraryAuthorityRecord
import com.rafgittools.library.LibraryCatalogBundle
import com.rafgittools.library.LibraryCatalogGate
import com.rafgittools.library.LibraryEvidenceState
import com.rafgittools.library.LibraryExpressionRecord
import com.rafgittools.library.LibraryItemRecord
import com.rafgittools.library.LibraryManifestationRecord
import com.rafgittools.library.LibraryPreservationEventRecord
import com.rafgittools.library.LibraryRelationRecord
import com.rafgittools.library.LibraryRelationType
import com.rafgittools.library.LibrarySourceBinding
import com.rafgittools.library.LibraryWorkRecord
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

data class RmrCtiFullMemberStreamPlanV1(
    val schemaVersion: String = "1.0.0",
    val sampleJobId: String,
    val packId: String,
    val sourceArchiveLocatorSha256: String,
    val sourceMember: String,
    val expectedMemberBytes: Long,
    val expectedMemberCrc32: String,
    val targetRigor: LibraryRigorLevel,
    val maxOutputBytes: Long,
    val chunkBytes: Int,
    val producerProvenanceIds: List<String>,
    val claimAllowed: Boolean = false
)

data class RmrCtiFullMemberReceiptV1(
    val schemaVersion: String = "1.0.0",
    val sampleJobId: String,
    val fullJobId: String,
    val sourceId: String,
    val contentSha256: String,
    val crc32: String,
    val bytesRead: Long,
    val descriptorSha256: String?,
    val catalogId: String?,
    val producerProvenanceIds: List<String>,
    val gaps: List<String>,
    val receiptSha256: String,
    val claimAllowed: Boolean = false
)

data class RmrCtiFullMemberStreamResultV1(
    val plan: RmrCtiFullMemberStreamPlanV1,
    val analysis: LibraryFullSourceStreamAnalysisV1,
    val execution: LibraryJobExecutionResult,
    val catalog: LibraryCatalogBundle?,
    val receipt: RmrCtiFullMemberReceiptV1,
    val claimAllowed: Boolean = false
)

object RmrCtiFullMemberStreamV1 {
    const val PRODUCER_BLOB =
        "f8e6687ef1994f64db8bcb5ce765f8cd21c842f1"
    const val PRODUCER_PATH = "rmrCti/rmrcti_dataset_mobile.py"
    const val ZIPIO_BLOB =
        "945238b9a4911a715a19a0bfd7217d9870274239"
    const val ZIPIO_PATH = "rmrCti/rmrcti_zipio.py"

    fun plan(
        pack: RmrCtiMobileContentPackV1,
        bridged: RmrCtiMobilePackBridgeResultV1,
        targetRigor: LibraryRigorLevel,
        maxOutputBytes: Long = pack.sourceMemberUncompressedBytes,
        chunkBytes: Int = 64 * 1024
    ): RmrCtiFullMemberStreamPlanV1 {
        require(targetRigor == LibraryRigorLevel.STRUCTURAL ||
            targetRigor == LibraryRigorLevel.MULTIMODAL
        ) {
            "automatic full-member promotion is limited to STRUCTURAL or MULTIMODAL"
        }
        require(Regex("^[0-9a-fA-F]{8}$").matches(pack.sourceMemberCrc32)) {
            "RMRCTI member CRC32 must be eight hex digits"
        }
        require(pack.sourceMemberUncompressedBytes >= 0L) {
            "RMRCTI member size invalid"
        }
        require(maxOutputBytes >= pack.sourceMemberUncompressedBytes) {
            "maxOutputBytes below source member size"
        }

        return RmrCtiFullMemberStreamPlanV1(
            sampleJobId = bridged.job.jobId,
            packId = pack.packId,
            sourceArchiveLocatorSha256 = bridged.sourceArchiveLocatorSha256,
            sourceMember = pack.sourceMember,
            expectedMemberBytes = pack.sourceMemberUncompressedBytes,
            expectedMemberCrc32 = pack.sourceMemberCrc32.lowercase(),
            targetRigor = targetRigor,
            maxOutputBytes = maxOutputBytes,
            chunkBytes = chunkBytes,
            producerProvenanceIds = (
                bridged.producerProvenanceIds + listOf(
                    "github:rafaelmeloreisnovo/llamaRafaelia:" +
                        PRODUCER_PATH + "@" + PRODUCER_BLOB,
                    "github:rafaelmeloreisnovo/llamaRafaelia:" +
                        ZIPIO_PATH + "@" + ZIPIO_BLOB
                )
            ).distinct().sorted(),
            claimAllowed = false
        )
    }

    fun execute(
        pack: RmrCtiMobileContentPackV1,
        bridged: RmrCtiMobilePackBridgeResultV1,
        plan: RmrCtiFullMemberStreamPlanV1,
        streamSource: RepeatableLibraryStreamSourceV1,
        environment: LibraryJobEnvironment,
        stateSink: LibraryJobStateSink = NoOpLibraryJobStateSink
    ): RmrCtiFullMemberStreamResultV1 {
        require(plan.sampleJobId == bridged.job.jobId) { "sample job mismatch" }
        require(plan.packId == pack.packId) { "pack id mismatch" }
        require(plan.sourceMember == pack.sourceMember) { "member mismatch" }
        require(!pack.claimAllowed && !plan.claimAllowed) {
            "claim-allowed source rejected"
        }
        require(plan.targetRigor != LibraryRigorLevel.EVIDENCE) {
            "EVIDENCE requires explicit evidence workflow and is not auto-promoted"
        }

        val rigor = LibraryRigorLens.contract(plan.targetRigor)
        val analysis = LibraryStreamingDescriptorEngineV1.analyze(
            source = streamSource,
            expectedBytes = plan.expectedMemberBytes,
            expectedCrc32 = plan.expectedMemberCrc32,
            maxOutputBytes = plan.maxOutputBytes,
            chunkBytes = plan.chunkBytes,
            includeTextVector = rigor.requireTextVectorWhenTextLike
        )

        val fullSource = bridged.job.source.copy(
            sizeBytes = analysis.bytesRead,
            contentSha256 = analysis.sha256,
            contentScope = LibraryContentScope.FULL_SOURCE,
            readOnly = true
        )
        val fullJob = bridged.job.copy(
            jobId = "JOB-RMRFULL-" + analysis.sha256.take(20),
            source = fullSource,
            rigor = plan.targetRigor,
            budget = bridged.job.budget.copy(
                maxBytes = plan.maxOutputBytes.coerceAtLeast(1L),
                checkpointEveryBytes = plan.chunkBytes.toLong()
            ),
            outputNamespace = "library/rmrcti/full-member/" +
                analysis.sha256.take(16),
            idempotencyKey = "RMRFULL-" +
                fullSource.opaqueLocatorHash.take(32) + "-" +
                analysis.sha256,
            createdAtEpochMs = environment.nowEpochMs,
            claimAllowed = false
        )

        val execution = LibraryLocalJobExecutor(stateSink).execute(
            job = fullJob,
            input = LibraryJobInput(
                bytes = null,
                text = null,
                precomputedByteVector = analysis.byteVector,
                precomputedTextVector = analysis.textVector,
                observedBytes = analysis.bytesRead
            ),
            environment = environment
        )

        val catalog = if (execution.receipt.finalState == LibraryJobState.SUCCEEDED) {
            buildCatalog(
                pack = pack,
                job = fullJob,
                analysis = analysis,
                execution = execution,
                createdAtEpochMs = environment.nowEpochMs
            )
        } else null

        if (catalog != null) {
            val gate = LibraryCatalogGate.validate(catalog)
            require(gate.allowed) {
                "generated RMRCTI catalog rejected: " + gate.errors.joinToString(",")
            }
        }

        val gaps = (
            analysis.gaps +
                execution.receipt.gaps +
                if (catalog == null) listOf("CATALOG_NOT_MATERIALIZED") else emptyList()
            ).distinct().sorted()

        val receiptWithoutHash = buildString {
            append("rmrcti-full-member-receipt-v1|")
            append(plan.sampleJobId).append('|')
            append(fullJob.jobId).append('|')
            append(fullSource.sourceId).append('|')
            append(analysis.sha256).append('|')
            append(analysis.crc32).append('|')
            append(analysis.bytesRead).append('|')
            append(execution.receipt.descriptorSha256.orEmpty()).append('|')
            append(catalog?.catalogId.orEmpty()).append('|')
            append(plan.producerProvenanceIds.sorted()).append('|')
            append(gaps).append('|')
            append("claim_allowed=false")
        }
        val receiptHash = sha256(receiptWithoutHash)
        val receipt = RmrCtiFullMemberReceiptV1(
            sampleJobId = plan.sampleJobId,
            fullJobId = fullJob.jobId,
            sourceId = fullSource.sourceId,
            contentSha256 = analysis.sha256,
            crc32 = analysis.crc32,
            bytesRead = analysis.bytesRead,
            descriptorSha256 = execution.receipt.descriptorSha256,
            catalogId = catalog?.catalogId,
            producerProvenanceIds = plan.producerProvenanceIds,
            gaps = gaps,
            receiptSha256 = receiptHash,
            claimAllowed = false
        )

        return RmrCtiFullMemberStreamResultV1(
            plan = plan,
            analysis = analysis,
            execution = execution,
            catalog = catalog,
            receipt = receipt,
            claimAllowed = false
        )
    }

    private fun buildCatalog(
        pack: RmrCtiMobileContentPackV1,
        job: LibraryLocalJob,
        analysis: LibraryFullSourceStreamAnalysisV1,
        execution: LibraryJobExecutionResult,
        createdAtEpochMs: Long
    ): LibraryCatalogBundle {
        val suffix = analysis.sha256.take(20)
        val sourceBindingId = "SRC-RMRCTI-" + suffix
        val authorityId = "AUTH-RMRCTI-LLAMA"
        val workId = "WORK-RMRCTI-" + suffix
        val expressionId = "EXP-RMRCTI-" + suffix
        val manifestationId = "MAN-RMRCTI-" + suffix
        val itemId = "ITEM-RMRCTI-" + suffix
        val eventId = "EVENT-RMRCTI-STREAM-" + suffix

        return LibraryCatalogBundle(
            catalogId = "CAT-RMRCTI-" + suffix,
            sources = listOf(
                LibrarySourceBinding(
                    sourceId = sourceBindingId,
                    sourceSurface = "RMRCTI_FULL_MEMBER_STREAM",
                    sourceSlot = "PHONE_LOCAL_STREAM",
                    providerAuthority = "rafaelmeloreisnovo/llamaRafaelia",
                    locatorSha256 = job.source.opaqueLocatorHash,
                    displayLabel = pack.sourceMember,
                    readOnly = true,
                    accessClass = LibraryAccessClass.PRIVATE,
                    evidenceState = LibraryEvidenceState.HASH_VERIFIED,
                    claimAllowed = false
                )
            ),
            authorities = listOf(
                LibraryAuthorityRecord(
                    authorityId = authorityId,
                    kind = AuthorityKind.REPOSITORY,
                    preferredLabel = "llamaRafaelia/rmrCti",
                    sourceRefs = listOf(
                        PRODUCER_PATH + "@" + PRODUCER_BLOB,
                        ZIPIO_PATH + "@" + ZIPIO_BLOB
                    ),
                    evidenceState = LibraryEvidenceState.SOURCE_OBSERVED,
                    claimAllowed = false
                )
            ),
            works = listOf(
                LibraryWorkRecord(
                    workId = workId,
                    preferredTitle = pack.sourceMember,
                    workType = pack.category,
                    authorityRefs = listOf(authorityId),
                    subjectRefs = pack.terms.sorted(),
                    evidenceRefs = listOf(
                        "sha256:" + analysis.sha256,
                        "crc32:" + analysis.crc32
                    ),
                    gapRefs = analysis.gaps,
                    claimAllowed = false
                )
            ),
            expressions = listOf(
                LibraryExpressionRecord(
                    expressionId = expressionId,
                    workId = workId,
                    expressionType = "TEXT_OR_SOURCE",
                    sourceRefs = listOf(job.source.sourceId),
                    evidenceRefs = listOf("descriptor:" +
                        execution.receipt.descriptorSha256.orEmpty()),
                    claimAllowed = false
                )
            ),
            manifestations = listOf(
                LibraryManifestationRecord(
                    manifestationId = manifestationId,
                    expressionRefs = listOf(expressionId),
                    formatLabel = "ZIP_MEMBER",
                    mediaType = job.source.mediaType,
                    versionLabel = "crc32:" + analysis.crc32,
                    publisherOrProducer = "llamaRafaelia/rmrCti",
                    evidenceRefs = listOf("sha256:" + analysis.sha256),
                    claimAllowed = false
                )
            ),
            items = listOf(
                LibraryItemRecord(
                    itemId = itemId,
                    manifestationId = manifestationId,
                    sourceId = sourceBindingId,
                    sourceRefSha256 = job.source.opaqueLocatorHash,
                    displayName = pack.sourceMember,
                    mediaType = job.source.mediaType ?: "application/octet-stream",
                    sizeBytes = analysis.bytesRead,
                    contentSha256 = analysis.sha256,
                    modifiedTime = null,
                    collectionRefs = listOf("RMRCTI"),
                    shelfRef = "RMRCTI_FULL_MEMBER",
                    accessClass = LibraryAccessClass.PRIVATE,
                    evidenceState = LibraryEvidenceState.HASH_VERIFIED,
                    evidenceRefs = listOf(
                        "crc32:" + analysis.crc32,
                        "descriptor:" + execution.receipt.descriptorSha256.orEmpty()
                    ),
                    gapRefs = analysis.gaps,
                    claimAllowed = false
                )
            ),
            preservationEvents = listOf(
                LibraryPreservationEventRecord(
                    eventId = eventId,
                    eventType = "FULL_MEMBER_STREAM_VERIFY",
                    objectRef = itemId,
                    occurredAt = createdAtEpochMs.toString(),
                    outcome = "PASS_SIZE_CRC32_SHA256",
                    agentRefs = listOf(authorityId),
                    evidenceRefs = listOf("sha256:" + analysis.sha256),
                    claimAllowed = false
                )
            ),
            relations = listOf(
                LibraryRelationRecord(
                    relationId = "REL-RMRCTI-" + suffix,
                    sourceId = itemId,
                    relation = LibraryRelationType.PART_OF,
                    targetId = manifestationId,
                    evidenceRefs = listOf("sha256:" + analysis.sha256),
                    evidenceState = LibraryEvidenceState.HASH_VERIFIED,
                    claimAllowed = false
                )
            ),
            gaps = analysis.gaps,
            createdAtEpochMs = createdAtEpochMs,
            claimAllowed = false
        )
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}

/**
 * Optional adapter to the exact producer route:
 *
 * python3 rmrcti_dataset_mobile.py stream <zip>
 *   --member <member> --out - --max-output-mib <bounded>
 *
 * ProcessBuilder receives argv directly; no shell is involved.
 * Private filesystem paths are runtime-only and never enter public receipts.
 */
class RmrCtiPythonStreamSourceV1(
    private val pythonExecutable: String,
    private val producerScriptPath: String,
    private val sourceArchivePath: String,
    private val member: String,
    private val maxOutputBytes: Long
) : RepeatableLibraryStreamSourceV1 {
    init {
        require(pythonExecutable.isNotBlank()) { "python executable required" }
        require(producerScriptPath.isNotBlank()) { "producer script path required" }
        require(sourceArchivePath.isNotBlank()) { "source archive path required" }
        require(member.isNotBlank()) { "member required" }
        require(maxOutputBytes > 0L) { "positive max output required" }
    }

    fun command(): List<String> {
        val mib = ((maxOutputBytes + 1024L * 1024L - 1L) /
            (1024L * 1024L)).coerceAtLeast(1L)
        return listOf(
            pythonExecutable,
            producerScriptPath,
            "stream",
            sourceArchivePath,
            "--member",
            member,
            "--out",
            "-",
            "--max-output-mib",
            mib.toString()
        )
    }

    override fun open(): InputStream {
        val process = ProcessBuilder(command()).start()
        return VerifiedProcessInputStream(process)
    }

    private class VerifiedProcessInputStream(
        private val process: Process
    ) : FilterInputStream(process.inputStream) {
        private val stderrDrainer = Thread {
            process.errorStream.use { err ->
                val sink = ByteArray(4096)
                while (true) {
                    val n = err.read(sink)
                    if (n < 0) break
                }
            }
        }.apply {
            isDaemon = true
            name = "rmrcti-stream-stderr"
            start()
        }

        override fun close() {
            var closeFailure: Throwable? = null
            try {
                super.close()
            } catch (t: Throwable) {
                closeFailure = t
            }

            val exit = try {
                process.waitFor()
            } catch (t: Throwable) {
                process.destroy()
                throw IOException("RMRCTI stream process wait failed", t)
            }
            stderrDrainer.join(1000L)

            if (closeFailure != null) {
                throw IOException("RMRCTI stream close failed", closeFailure)
            }
            if (exit != 0) {
                throw IOException("RMRCTI stream process failed with exit=" + exit)
            }
        }
    }
}
