package com.rafgittools.navigator

import com.google.gson.GsonBuilder
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Builds a private, phone-local project pack from the canonical conversation corpus.
 *
 * Raw source bytes remain in the selected SAF/Drive tree. The builder streams each
 * source through ConversationManifoldProcessor, then concatenates only derived JSONL
 * records into bounded project-pack shards. Provider IDs and raw SAF URIs are never
 * serialized into the manifest or receipt.
 */
class ConversationProjectPackBuilder(
    private val privateRoot: File,
    private val maxShardBytes: Long = 8L * 1024L * 1024L
) {
    data class SourceSummary(
        val index: Int,
        val sourceName: String,
        val sourceSha256: String,
        val sourceBytes: Long,
        val records: Long,
        val nodes: Long,
        val messages: Long,
        val derivedFileName: String,
        val derivedSha256: String
    )

    data class ShardSummary(
        val filename: String,
        val bytes: Long,
        val sha256: String
    )

    data class Manifest(
        val schema: String = "rafgittools.gpt-project-conversation-pack/v1",
        val logicalSourceRoute: String,
        val expectedFiles: Int,
        val presentFiles: Int,
        val processedFiles: Int,
        val missingIndices: List<Int>,
        val duplicateIndices: List<Int>,
        val outOfRangeNames: List<String>,
        val gaps: List<String>,
        val sources: List<SourceSummary>,
        val shards: List<ShardSummary>,
        val privacyClass: String = "PRIVATE_DEFAULT_DENY",
        val sourceMutated: Boolean = false,
        val claimAllowed: Boolean = false,
        val state: String,
        val createdAtEpochMs: Long
    )

    data class Receipt(
        val schema: String = "rafgittools.gpt-project-conversation-pack-receipt/v1",
        val manifestFile: String,
        val manifestSha256: String,
        val shardCount: Int,
        val processedFiles: Int,
        val expectedFiles: Int,
        val gaps: List<String>,
        val sourceMutated: Boolean = false,
        val claimAllowed: Boolean = false,
        val state: String,
        val createdAtEpochMs: Long
    )

    data class Result(
        val state: String,
        val generationDir: File,
        val manifestFile: File,
        val manifestSha256: String,
        val receiptFile: File,
        val coverage: NovoexportConversationCorpus.Coverage,
        val sources: List<SourceSummary>,
        val shards: List<ShardSummary>
    )

    private val gson = GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()

    fun build(
        inventory: NovoexportSafInventory.Result,
        openSource: (NovoexportSafInventory.Entry) -> InputStream,
        createdAtEpochMs: Long = System.currentTimeMillis()
    ): Result {
        require(inventory.state == "INVENTORY_COMPLETE_METADATA_ONLY") {
            "Project pack requires completed SAF metadata inventory"
        }
        require(maxShardBytes >= 512L * 1024L) { "Project pack shard bound is too small" }
        require(createdAtEpochMs >= 0L) { "Invalid project pack timestamp" }

        val sourceFiles = inventory.allFiles.ifEmpty { inventory.candidateFiles }
        val coverage = NovoexportConversationCorpus.coverage(sourceFiles)
        require(coverage.duplicateIndices.isEmpty()) {
            "Canonical conversation corpus has duplicate indices: ${coverage.duplicateIndices}"
        }

        val canonical = sourceFiles.mapNotNull { entry ->
            NovoexportConversationCorpus.canonicalIndex(entry.name)?.let { index -> index to entry }
        }.sortedBy { it.first }
        require(canonical.isNotEmpty()) { "No canonical conversations-000..050 JSON files found" }

        if (!privateRoot.exists() && !privateRoot.mkdirs()) {
            error("Cannot create private project-pack root")
        }
        require(privateRoot.isDirectory) { "Project-pack root is not a directory" }

        val working = File(privateRoot, ".g$createdAtEpochMs.part")
        if (working.exists() && !working.deleteRecursively()) {
            error("Cannot clear interrupted project-pack generation")
        }
        if (!working.mkdirs()) error("Cannot create project-pack generation")

        try {
            val derivedRoot = File(working, "derived")
            val packRoot = File(working, "project-pack")
            if (!derivedRoot.mkdirs() || !packRoot.mkdirs()) {
                error("Cannot create project-pack working directories")
            }

            val processor = ConversationManifoldProcessor(derivedRoot)
            val processedPairs = canonical.map { (index, entry) ->
                val result = openSource(entry).use { input ->
                    processor.process(entry.name, input)
                }
                index to result
            }

            val sourceSummaries = processedPairs.map { (index, result) ->
                SourceSummary(
                    index = index,
                    sourceName = result.sourceName,
                    sourceSha256 = result.sourceSha256,
                    sourceBytes = result.sourceBytes,
                    records = result.records,
                    nodes = result.nodes,
                    messages = result.messages,
                    derivedFileName = result.outputFile.name,
                    derivedSha256 = result.outputSha256
                )
            }

            val shards = writeShards(packRoot, processedPairs.map { it.second.outputFile })
            val state = if (coverage.complete && sourceSummaries.size == NovoexportConversationCorpus.EXPECTED_COUNT) {
                "PACK_COMPLETE_51_OF_51"
            } else {
                "PACK_PARTIAL_TYPED_GAP"
            }
            val gaps = coverage.gapRefs()

            val manifest = Manifest(
                logicalSourceRoute = NovoexportConversationCorpus.LOGICAL_ROUTE,
                expectedFiles = NovoexportConversationCorpus.EXPECTED_COUNT,
                presentFiles = coverage.presentCount,
                processedFiles = sourceSummaries.size,
                missingIndices = coverage.missingIndices,
                duplicateIndices = coverage.duplicateIndices,
                outOfRangeNames = coverage.outOfRangeNames,
                gaps = gaps,
                sources = sourceSummaries,
                shards = shards,
                state = state,
                createdAtEpochMs = createdAtEpochMs
            )
            val manifestFile = File(working, "GPT_PROJECT_CONVERSATION_CORPUS_MANIFEST_V1.json")
            writeTextSynced(manifestFile, gson.toJson(manifest) + "\n")
            val manifestSha = sha256(manifestFile)

            val receipt = Receipt(
                manifestFile = manifestFile.name,
                manifestSha256 = manifestSha,
                shardCount = shards.size,
                processedFiles = sourceSummaries.size,
                expectedFiles = NovoexportConversationCorpus.EXPECTED_COUNT,
                gaps = gaps,
                state = state,
                createdAtEpochMs = createdAtEpochMs
            )
            val receiptFile = File(working, "GPT_PROJECT_CONVERSATION_CORPUS_RECEIPT_V1.json")
            writeTextSynced(receiptFile, gson.toJson(receipt) + "\n")

            val finalDir = File(privateRoot, "g${createdAtEpochMs}-${manifestSha.take(12)}")
            require(!finalDir.exists()) { "Project-pack generation already exists" }
            if (!working.renameTo(finalDir)) error("Cannot atomically promote project-pack generation")

            return Result(
                state = state,
                generationDir = finalDir,
                manifestFile = File(finalDir, manifestFile.name),
                manifestSha256 = manifestSha,
                receiptFile = File(finalDir, receiptFile.name),
                coverage = coverage,
                sources = sourceSummaries,
                shards = shards.map { it.copy(filename = "project-pack/${it.filename}") }
            )
        } catch (t: Throwable) {
            working.deleteRecursively()
            throw t
        }
    }

    private fun writeShards(packRoot: File, derivedFiles: List<File>): List<ShardSummary> {
        val completed = mutableListOf<ShardSummary>()
        var shardIndex = 0
        var currentFile: File? = null
        var currentOut: BufferedOutputStream? = null
        var currentBytes = 0L

        fun openShard() {
            val file = File(packRoot, "gpt-project-conversations-%03d.jsonl".format(shardIndex++))
            currentFile = file
            currentOut = BufferedOutputStream(FileOutputStream(file), 64 * 1024)
            currentBytes = 0L
        }

        fun closeShard() {
            val output = currentOut ?: return
            output.flush()
            output.close()
            val file = currentFile ?: error("Project-pack shard file missing")
            completed += ShardSummary(
                filename = file.name,
                bytes = file.length(),
                sha256 = sha256(file)
            )
            currentOut = null
            currentFile = null
            currentBytes = 0L
        }

        try {
            derivedFiles.forEach { derived ->
                require(derived.isFile) { "Derived conversation file missing: ${derived.name}" }
                derived.bufferedReader(Charsets.UTF_8, 64 * 1024).useLines { lines ->
                    lines.forEach { line ->
                        val bytes = (line + "\n").toByteArray(Charsets.UTF_8)
                        require(bytes.size.toLong() <= maxShardBytes) {
                            "Single derived JSONL record exceeds project-pack shard bound"
                        }
                        if (currentOut == null) openShard()
                        if (currentBytes > 0L && currentBytes + bytes.size > maxShardBytes) {
                            closeShard()
                            openShard()
                        }
                        currentOut!!.write(bytes)
                        currentBytes += bytes.size
                    }
                }
            }
            closeShard()
        } catch (t: Throwable) {
            runCatching { currentOut?.close() }
            throw t
        }
        require(completed.isNotEmpty()) { "Project pack produced no JSONL shards" }
        return completed
    }

    private fun writeTextSynced(file: File, text: String) {
        FileOutputStream(file).use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
            output.flush()
            output.fd.sync()
        }
    }

    private fun sha256(file: File): String = file.inputStream().buffered(64 * 1024).use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            digest.update(buffer, 0, read)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}
