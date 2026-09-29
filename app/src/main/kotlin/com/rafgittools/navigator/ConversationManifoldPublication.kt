package com.rafgittools.navigator

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.security.MessageDigest

/**
 * Publishes generated, derived artifacts only. The Drive folder URI must be selected by the user
 * through ACTION_OPEN_DOCUMENT_TREE with persistable read/write permission. Git writes are routed
 * through the app's existing private-target, live-readback API callback.
 */
object ConversationManifoldPublication {
    data class Plan(
        val generationId: String,
        val driveFolderUri: String,
        val githubOwner: String,
        val githubRepository: String,
        val files: List<Pair<String, String>>,
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
        val entries = splitUtf8Lines(artifact, 480 * 1024)
        val filenames = entries.mapIndexed { index, _ ->
            "${artifact.nameWithoutExtension}-part-${index.toString().padStart(5, '0')}.jsonl"
        }
        val planHash = sha256((listOf(
            generationId, driveFolderUri.toString(), "$githubOwner/$githubRepository", artifactHash
        ) + filenames.zip(entries).map { (name, content) -> "$name:${sha256(content.toByteArray(Charsets.UTF_8))}" })
            .joinToString("\n").toByteArray(Charsets.UTF_8))
        return Plan(generationId, driveFolderUri.toString(), githubOwner, githubRepository,
            filenames.zip(entries), planHash)
    }

    /**
     * Exact-plan confirmation is required. The completion manifest is written last on GitHub so
     * interrupted publication is detectable and restartable; existing filenames are never replaced.
     */
    suspend fun publish(
        resolver: ContentResolver,
        plan: Plan,
        confirmedPlanSha256: String,
        githubWrite: suspend (owner: String, repository: String, path: String, content: String, message: String) -> Result<Unit>
    ): Published {
        require(plan.planSha256 == confirmedPlanSha256) { "Publication plan confirmation does not match" }
        val tree = Uri.parse(plan.driveFolderUri)
        val githubBase = "memory_bridge/private_processing/conversation_manifold/${plan.generationId}"
        val driveUris = mutableListOf<String>()
        val githubPaths = mutableListOf<String>()
        var combined = MessageDigest.getInstance("SHA-256")

        plan.files.forEachIndexed { index, (filename, content) ->
            val driveUri = DocumentsContract.createDocument(resolver, tree, "application/x-ndjson", filename)
                ?: error("Drive/provider refused artifact creation: $filename")
            resolver.openOutputStream(driveUri, "w").use { output ->
                requireNotNull(output) { "Cannot open selected Drive destination" }
                output.write(content.toByteArray(Charsets.UTF_8))
                output.flush()
            }
            driveUris += driveUri.toString()

            val path = "$githubBase/$filename"
            githubWrite(plan.githubOwner, plan.githubRepository, path, content,
                "RafGitTools: publish derived conversation manifold part ${index + 1}/${plan.files.size}")
                .getOrElse { throw it }
            githubPaths += path
            combined.update(content.toByteArray(Charsets.UTF_8))
        }

        val manifestPath = "$githubBase/PUBLICATION_COMPLETE.json"
        val manifest = """{"schema":"rafgittools.conversation-manifold-publication/v1","generation_id":"${plan.generationId}","files":${plan.files.size},"parts_sha256":"${combined.digest().hex()}","plan_sha256":"${plan.planSha256}","state":"PUBLISHED_BY_APP","claim_allowed":false}"""
        githubWrite(plan.githubOwner, plan.githubRepository, manifestPath, manifest,
            "RafGitTools: close derived conversation manifold publication")
            .getOrElse { throw it }
        githubPaths += manifestPath
        return Published(driveUris, githubPaths, sha256(manifest.toByteArray(Charsets.UTF_8)))
    }

    private fun splitUtf8Lines(file: File, maxBytes: Int): List<String> {
        require(maxBytes > 0)
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var currentBytes = 0
        file.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                val encoded = (line + "\n").toByteArray(Charsets.UTF_8)
                require(encoded.size <= maxBytes) { "One derived record exceeds GitHub part limit; split the record before publication" }
                if (currentBytes + encoded.size > maxBytes && current.isNotEmpty()) {
                    parts += current.toString()
                    current.setLength(0)
                    currentBytes = 0
                }
                current.append(line).append('\n')
                currentBytes += encoded.size
            }
        }
        if (current.isNotEmpty()) parts += current.toString()
        if (parts.isEmpty()) parts += ""
        return parts
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
