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
    data class Published(
        val driveUris: List<String>,
        val githubPaths: List<String>,
        val githubCommitShas: List<String>,
        val artifactSha256: String,
        val state: String,
        val driveReadbackVerified: Boolean,
        val githubReadbackVerified: Boolean
    )

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
        for ((index, content) in partSequence(artifact)) {
            val bytes = content.toByteArray(Charsets.UTF_8)
            parts += Part(partName(artifact, generationId, index), bytes.size.toLong(), sha256(bytes))
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
        githubWrite: suspend (owner: String, repository: String, path: String, content: String, message: String) -> Result<String>,
        githubRead: suspend (owner: String, repository: String, path: String) -> Result<String>
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
        val commitShas = mutableListOf<String>()
        val aggregate = MessageDigest.getInstance("SHA-256")

        for ((index, content) in partSequence(artifact)) {
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
            val driveReadback = resolver.openInputStream(driveUri)
                ?.use { it.readBytes() }
                ?: error("Cannot read back Drive artifact: ${expected.filename}")
            verifyReadbackBytes(expected.bytes, expected.sha256, driveReadback, "Drive/${expected.filename}")
            driveUris += driveUri.toString()

            val path = "$githubBase/${expected.filename}"
            val commitSha = githubWrite(plan.githubOwner, plan.githubRepository, path, content,
                "RafGitTools: publish derived manifold part ${index + 1}/${plan.parts.size}")
                .getOrElse { throw it }
            val githubReadback = githubRead(plan.githubOwner, plan.githubRepository, path)
                .getOrElse { throw it }
                .toByteArray(Charsets.UTF_8)
            verifyReadbackBytes(expected.bytes, expected.sha256, githubReadback, "Git/${expected.filename}")
            commitShas += commitSha
            githubPaths += path
            aggregate.update(bytes)
        }
        require(plan.parts.size == githubPaths.size) { "Part count changed during publication" }

        val partsHash = aggregate.digest().hex()
        val commitList = commitShas.joinToString(prefix = "[\"", postfix = "\"]", separator = "\",\"")
        val manifest = """{"schema":"rafgittools.conversation-manifold-publication/v2","generation_id":"${plan.generationId}","parts":${plan.parts.size},"parts_sha256":"$partsHash","github_commit_shas":$commitList,"plan_sha256":"${plan.planSha256}","drive_parts_readback":"PASS","github_parts_readback":"PASS","state":"PUBLISHED_READBACK_VERIFIED","claim_allowed":false}"""
        val manifestBytes = manifest.toByteArray(Charsets.UTF_8)
        val manifestSha256 = sha256(manifestBytes)
        val manifestName = "PUBLICATION_COMPLETE.${plan.planSha256.take(16)}.json"
        val manifestUri = DocumentsContract.createDocument(resolver, tree, "application/json", manifestName)
            ?: error("Drive/provider refused completion manifest")
        val manifestStream = resolver.openOutputStream(manifestUri, "w")
            ?: error("Cannot open Drive completion manifest")
        manifestStream.use { it.write(manifestBytes); it.flush() }
        val manifestDriveReadback = resolver.openInputStream(manifestUri)
            ?.use { it.readBytes() }
            ?: error("Cannot read back Drive completion manifest")
        verifyReadbackBytes(manifestBytes.size.toLong(), manifestSha256, manifestDriveReadback, "Drive/$manifestName")
        driveUris += manifestUri.toString()

        val manifestPath = "$githubBase/$manifestName"
        val manifestCommit = githubWrite(plan.githubOwner, plan.githubRepository, manifestPath, manifest,
            "RafGitTools: close readback-verified conversation manifold publication")
            .getOrElse { throw it }
        val manifestGithubReadback = githubRead(plan.githubOwner, plan.githubRepository, manifestPath)
            .getOrElse { throw it }
            .toByteArray(Charsets.UTF_8)
        verifyReadbackBytes(manifestBytes.size.toLong(), manifestSha256, manifestGithubReadback, "Git/$manifestName")
        githubPaths += manifestPath
        commitShas += manifestCommit
        return Published(
            driveUris = driveUris,
            githubPaths = githubPaths,
            githubCommitShas = commitShas,
            artifactSha256 = manifestSha256,
            state = "PUBLISHED_READBACK_VERIFIED",
            driveReadbackVerified = true,
            githubReadbackVerified = true
        )
    }

    private fun partName(artifact: File, generationId: String, index: Int) =
        "${artifact.nameWithoutExtension}-${generationId}-part-${index.toString().padStart(5, '0')}.jsonl"

    internal fun verifyReadbackBytes(
        expectedBytes: Long,
        expectedSha256: String,
        readback: ByteArray,
        label: String
    ) {
        require(readback.size.toLong() == expectedBytes) {
            "$label byte-count mismatch: expected=$expectedBytes actual=${readback.size}"
        }
        val actualSha256 = sha256(readback)
        require(actualSha256 == expectedSha256) {
            "$label SHA-256 mismatch: expected=$expectedSha256 actual=$actualSha256"
        }
    }

    private fun partSequence(file: File): Sequence<Pair<Int, String>> = sequence {
        var index = 0
        val current = StringBuilder()
        var currentBytes = 0
        val reader = file.bufferedReader(Charsets.UTF_8)
        while (true) {
            val line = reader.readLine() ?: break
            val encoded = (line + "\n").toByteArray(Charsets.UTF_8)
            require(encoded.size <= MAX_PART_BYTES) {
                "One derived record exceeds GitHub part limit; split before publication"
            }
            if (currentBytes + encoded.size > MAX_PART_BYTES && current.isNotEmpty()) {
                yield(index++ to current.toString())
                current.setLength(0)
                currentBytes = 0
            }
            current.append(line).append('\n')
            currentBytes += encoded.size
        }
        reader.close()
        if (current.isNotEmpty()) yield(index to current.toString())
        if (index == 0 && current.isEmpty() && file.length() == 0L) yield(0 to "")
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
