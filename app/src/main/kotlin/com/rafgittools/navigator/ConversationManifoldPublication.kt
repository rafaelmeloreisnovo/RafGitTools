package com.rafgittools.navigator

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Publishes generated artifacts only. The user selects the Drive destination with SAF.
 * Git writes are routed through RafGitTools' existing live-private-target writer.
 * Publication remains bounded: only one <=480 KiB JSONL part is held in memory at a time.
 */
object ConversationManifoldPublication {
    private const val MAX_PART_BYTES = 480 * 1024

    data class Part(val filename: String, val bytes: Long, val sha256: String)
    data class Plan(
        val generationId: String,
        val driveFolderUri: String,
        val githubOwner: String,
        val githubRepository: String,
        val sourceArtifactPath: String,
        val sourceArtifactSha256: String,
        val parts: List<Part>,
        val planSha256: String
    )
    data class Published(val driveUris: List<String>, val githubPaths: List<String>, val artifactSha256: String)

    fun plan(
        generationId: String,
        driveFolderUri: Uri,
        githubOwner: String,
        githubRepository: String,
        artifact: File
    ): Plan {
        require(generationId.matches(Regex("[A-Za-z0-9._-]{1,96}")))
        require(githubOwner.matches(Regex("[A-Za-z0-9-]{1,39}")))
        require(githubRepository.matches(Regex("[A-Za-z0-9._-]{1,100}")))
        require(artifact.isFile)
        val artifactHash = sha256(artifact)
        val parts = mutableListOf<Part>()
        forEachPart(artifact) { index, content ->
            val bytes = content.toByteArray(Charsets.UTF_8)
            parts += Part(partName(artifact, plan.generationId, index), bytes.size.toLong(), sha256(bytes))
        }
        val canonicalPlan = (listOf(generationId, driveFolderUri.toString(),
            "$githubOwner/$githubRepository", artifact.absolutePath, artifactHash) +
            parts.map { "${it.filename}:${it.bytes}:${it.sha256}" }).joinToString("\n")
        return Plan(generationId, driveFolderUri.toString(), githubOwner, githubRepository,
            artifact.absolutePath, artifactHash, parts, sha256(canonicalPlan.toByteArray(Charsets.UTF_8)))
    }

    /**
     * Call only after displaying the plan and receiving user confirmation of its exact SHA-256.
     * Each Drive file and Git file is immutable and generation-scoped. A completion manifest is
     * written to both destinations last; failure leaves an incomplete generation, never a PASS.
     */
    suspend fun publish(
        resolver: ContentResolver,
        plan: Plan,
        confirmedPlanSha256: String,
        githubWrite: suspend (owner: String, repository: String, path: String, content: String, message: String) -> Result<Unit>
    ): Published {
        require(plan.planSha256 == confirmedPlanSha256) { "Publication plan confirmation does not match" }
        val artifact = File(plan.sourceArtifactPath)
        require(artifact.isFile && sha256(artifact) == plan.sourceArtifactSha256) {
            "Derived artifact changed after plan confirmation"
        }

        val tree = Uri.parse(plan.driveFolderUri)
        val githubBase = "memory_bridge/private_processing/conversation_manifold/${plan.generationId}"
        val driveUris = mutableListOf<String>()
        val githubPaths = mutableListOf<String>()
        val aggregate = MessageDigest.getInstance("SHA-256")

        forEachPart(artifact) { index, content ->
            val expected = plan.parts.getOrNull(index) ?: error("Part count changed after planning")
            val bytes = content.toByteArray(Charsets.UTF_8)
            require(expected.filename == partName(artifact, plan.generationId, index) &&
                expected.bytes == bytes.size.toLong() && expected.sha256 == sha256(bytes)) {
                "Part changed after plan confirmation"
            }
            val driveUri = DocumentsContract.createDocument(
                resolver, tree, "application/x-ndjson", expected.filename
            ) ?: error("Drive/provider refused artifact creation: ${expected.filename}")
            val stream = resolver.openOutputStream(driveUri, "w")
                ?: error("Cannot open selected Drive destination")
            stream.use {
                it.write(bytes)
                it.flush()
            }
            driveUris += driveUri.toString()

            val path = "$githubBase/${expected.filename}"
            githubWrite(plan.githubOwner, plan.githubRepository, path, content,
                "RafGitTools: publish derived manifold part ${index + 1}/${plan.parts.size}")
                .getOrElse { throw it }
            githubPaths += path
            aggregate.update(bytes)
        }
        require(plan.parts.size == githubPaths.size) { "Part count changed during publication" }

        val partsHash = aggregate.digest().hex()
        val manifest = """{"schema":"rafgittools.conversation-manifold-publication/v1","generation_id":"${plan.generationId}","parts":${plan.parts.size},"parts_sha256":"$partsHash","plan_sha256":"${plan.planSha256}","state":"PUBLISHED_BY_APP","claim_allowed":false}"""
        val manifestName = "PUBLICATION_COMPLETE.${plan.planSha256.take(16)}.json"
        val manifestUri = DocumentsContract.createDocument(resolver, tree, "application/json", manifestName)
            ?: error("Drive/provider refused completion manifest")
        val manifestStream = resolver.openOutputStream(manifestUri, "w")
            ?: error("Cannot open Drive completion manifest")
        manifestStream.use { it.write(manifest.toByteArray(Charsets.UTF_8)); it.flush() }
        driveUris += manifestUri.toString()

        val manifestPath = "$githubBase/$manifestName"
        githubWrite(plan.githubOwner, plan.githubRepository, manifestPath, manifest,
            "RafGitTools: close derived conversation manifold publication")
            .getOrElse { throw it }
        githubPaths += manifestPath
        return Published(driveUris, githubPaths, sha256(manifest.toByteArray(Charsets.UTF_8)))
    }

    private fun partName(artifact: File, generationId: String, index: Int) =
        "${artifact.nameWithoutExtension}-${generationId}-part-${index.toString().padStart(5, '0')}.jsonl"

    private fun forEachPart(file: File, consume: (Int, String) -> Unit) {
        var index = 0
        val current = StringBuilder()
        var currentBytes = 0
        file.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                val encoded = (line + "\n").toByteArray(Charsets.UTF_8)
                require(encoded.size <= MAX_PART_BYTES) {
                    "One derived record exceeds GitHub part limit; split before publication"
                }
                if (currentBytes + encoded.size > MAX_PART_BYTES && current.isNotEmpty()) {
                    consume(index++, current.toString())
                    current.setLength(0)
                    currentBytes = 0
                }
                current.append(line).append('\n')
                currentBytes += encoded.size
            }
        }
        if (current.isNotEmpty()) consume(index, current.toString())
        if (index == 0 && current.isEmpty() && file.length() == 0L) consume(0, "")
    }

    private fun sha256(file: File): String = file.inputStream().use { input ->
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read > 0) md.update(buffer, 0, read)
        }
        md.digest().hex()
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).hex()
    private fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }
}
