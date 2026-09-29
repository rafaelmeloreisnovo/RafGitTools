package com.rafgittools.navigator

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.security.DigestInputStream
import java.security.MessageDigest

/**
 * Bounded-per-record, restartable processor for the user's conversations*.json and codex*.json
 * exports. Source streams are read-only. One top-level JSON record is held in memory at a time;
 * output is private derived JSONL, partitioned by input file and content-addressed source digest.
 *
 * The caller owns SAF permission, Drive enumeration, cancellation and publication. This processor
 * never uploads source bytes and never treats a partial file as complete.
 */
class ConversationManifoldProcessor(
    private val outputRoot: File,
    private val maxSourceBytes: Long = 2L * 1024L * 1024L * 1024L,
    private val maxRecordUtf8Bytes: Long = 16L * 1024L * 1024L
) {
    data class Result(
        val state: String,
        val sourceName: String,
        val sourceSha256: String,
        val sourceBytes: Long,
        val records: Long,
        val nodes: Long,
        val messages: Long,
        val codexRecords: Long,
        val outputFile: File,
        val outputSha256: String,
        val checkpointFile: File
    )

    private data class Counts(var records: Long = 0, var nodes: Long = 0, var messages: Long = 0, var codex: Long = 0)

    private val gson = Gson()

    fun process(sourceName: String, source: InputStream): Result {
        require(sourceName.endsWith(".json", ignoreCase = true)) { "Only JSON source files are supported" }
        require(sourceName.startsWith("conversations", ignoreCase = true) ||
            sourceName.startsWith("codex", ignoreCase = true)) {
            "Expected conversations*.json or codex*.json"
        }
        if (!outputRoot.exists() && !outputRoot.mkdirs()) error("Cannot create private output directory")
        require(outputRoot.isDirectory) { "Output path is not a directory" }

        val safeName = sourceName.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_")
        val part = File(outputRoot, ".$safeName.part")
        if (part.exists() && !part.delete()) error("Cannot clear interrupted partial output")
        val digest = MessageDigest.getInstance("SHA-256")
        val counts = Counts()
        var byteCount = 0L
        val recordDigest = MessageDigest.getInstance("SHA-256")

        try {
            DigestInputStream(source, digest).use { digestStream ->
                val counted = CountingInputStream(digestStream, maxSourceBytes)
                JsonReader(InputStreamReader(counted, Charsets.UTF_8).buffered(64 * 1024)).use { reader ->
                    reader.isLenient = false
                    BufferedWriter(OutputStreamWriter(FileOutputStream(part), Charsets.UTF_8), 64 * 1024).use { out ->
                        when (reader.peek()) {
                            com.google.gson.stream.JsonToken.BEGIN_ARRAY -> {
                                reader.beginArray()
                                while (reader.hasNext()) {
                                    val element = com.google.gson.JsonParser.parseReader(reader)
                                    writeRecord(safeName, element, out, counts)
                                }
                                reader.endArray()
                            }
                            com.google.gson.stream.JsonToken.BEGIN_OBJECT -> {
                                val element = com.google.gson.JsonParser.parseReader(reader)
                                if (element.isJsonObject) {
                                    val root = element.asJsonObject
                                    val records = sequenceField(root)
                                    if (records != null) {
                                        records.forEach { writeRecord(safeName, it, out, counts) }
                                    } else {
                                        writeRecord(safeName, element, out, counts)
                                    }
                                } else {
                                    writeRecord(safeName, element, out, counts)
                                }
                            }
                            else -> error("Expected a top-level JSON array or object")
                        }
                        if (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) {
                            error("Trailing content after top-level JSON value")
                        }
                        out.flush()
                    }
                    byteCount = counted.bytesRead
                }
            }
        } catch (t: Throwable) {
            part.delete()
            throw t
        }

        val sourceHash = digest.digest().hex()
        val outputHash = sha256(part)
        val finalFile = File(outputRoot, "${safeName}.${sourceHash.take(16)}.derived.jsonl")
        if (finalFile.exists() && sha256(finalFile) != outputHash) {
            part.delete()
            error("Content-addressed output collision")
        }
        if (finalFile.exists()) part.delete()
        else if (!part.renameTo(finalFile)) {
            part.delete()
            error("Cannot atomically promote processed output")
        }

        val checkpoint = File(outputRoot, "CHECKPOINTS.jsonl")
        FileOutputStream(checkpoint, true).bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.append(gson.toJson(mapOf(
                "event" to "FILE_COMPLETE",
                "source_name" to safeName,
                "source_sha256" to sourceHash,
                "source_bytes" to byteCount,
                "output_file" to finalFile.name,
                "output_sha256" to outputHash,
                "records" to counts.records,
                "nodes" to counts.nodes,
                "messages" to counts.messages,
                "codex_records" to counts.codex,
                "claim_allowed" to false
            ))).append('\n')
        }
        return Result("COMPLETE", safeName, sourceHash, byteCount, counts.records, counts.nodes,
            counts.messages, counts.codex, finalFile, outputHash, checkpoint)
    }

    private fun writeRecord(sourceName: String, element: JsonElement, out: BufferedWriter, counts: Counts) {
        val encoded = gson.toJson(element)
        val size = encoded.toByteArray(Charsets.UTF_8).size.toLong()
        require(size <= maxRecordUtf8Bytes) { "Single JSON record exceeds configured memory bound" }
        val recordIndex = counts.records++
        val isConversation = sourceName.startsWith("conversations", ignoreCase = true)
        if (!isConversation) {
            val hash = sha256(encoded.toByteArray(Charsets.UTF_8))
            writeLine(out, mapOf(
                "kind" to "CODEX_RECORD",
                "source_name" to sourceName,
                "record_index" to recordIndex,
                "record_sha256" to hash,
                "record" to element,
                "privacy_class" to "PRIVATE_DEFAULT_DENY",
                "claim_allowed" to false
            ))
            counts.codex++
            return
        }
        if (!element.isJsonObject) {
            writeLine(out, mapOf("kind" to "CONVERSATION_RECORD", "source_name" to sourceName,
                "record_index" to recordIndex, "record" to element, "claim_allowed" to false))
            return
        }
        val conversation = element.asJsonObject
        val conversationId = scalar(conversation.get("id") ?: conversation.get("conversation_id"))
            ?: "TOKEN_VAZIO"
        val mapping = conversation.getAsJsonObject("mapping")
        writeLine(out, mapOf("kind" to "CONVERSATION", "source_name" to sourceName,
            "record_index" to recordIndex, "conversation_id" to conversationId,
            "title" to scalar(conversation.get("title")) ?: "TOKEN_VAZIO",
            "created_at" to scalar(conversation.get("create_time")) ?: "TOKEN_VAZIO",
            "updated_at" to scalar(conversation.get("update_time")) ?: "TOKEN_VAZIO",
            "claim_allowed" to false))
        if (mapping == null) return
        mapping.entrySet().forEach { (nodeId, nodeValue) ->
            if (!nodeValue.isJsonObject) return@forEach
            val node = nodeValue.asJsonObject
            val parent = scalar(node.get("parent")) ?: "TOKEN_VAZIO"
            val msg = node.getAsJsonObject("message")
            writeLine(out, mapOf("kind" to "NODE", "conversation_id" to conversationId,
                "node_id" to nodeId, "parent_id" to parent, "has_message" to (msg != null),
                "source_name" to sourceName, "record_index" to recordIndex, "claim_allowed" to false))
            counts.nodes++
            if (msg != null) {
                val author = msg.getAsJsonObject("author")
                val content = msg.getAsJsonObject("content")
                val messageId = scalar(msg.get("id")) ?: nodeId
                val text = content?.get("parts")?.let(::flattenText) ?: "TOKEN_VAZIO"
                writeLine(out, mapOf("kind" to "MESSAGE_CHUNK", "conversation_id" to conversationId,
                    "message_id" to messageId, "node_id" to nodeId, "parent_id" to parent,
                    "role" to scalar(author?.get("role")) ?: "TOKEN_VAZIO",
                    "created_at" to scalar(msg.get("create_time")) ?: "TOKEN_VAZIO",
                    "content_type" to scalar(content?.get("content_type")) ?: "TOKEN_VAZIO",
                    "text" to text, "chunk_sha256" to sha256(text.toByteArray(Charsets.UTF_8)),
                    "source_name" to sourceName, "record_index" to recordIndex,
                    "privacy_class" to "PRIVATE_DEFAULT_DENY", "claim_allowed" to false))
                counts.messages++
            }
        }
    }

    private fun sequenceField(root: JsonObject): JsonArray? =
        listOf("records", "items", "conversations", "data").firstNotNullOfOrNull { key ->
            root.get(key)?.takeIf { it.isJsonArray }?.asJsonArray
        }

    private fun flattenText(element: JsonElement): String = when {
        element.isJsonNull -> ""
        element.isJsonPrimitive -> element.asJsonPrimitive.asString
        element.isJsonArray -> element.joinToString("\n") { flattenText(it) }
        element.isJsonObject -> element.asJsonObject.get("text")?.let(::flattenText)
            ?: element.asJsonObject.get("transcript")?.let(::flattenText)
            ?: ""
        else -> ""
    }

    private fun scalar(element: JsonElement?): String? =
        if (element == null || element.isJsonNull || !element.isJsonPrimitive) null else element.asJsonPrimitive.asString

    private fun writeLine(out: BufferedWriter, value: Any) {
        out.append(gson.toJson(value)).append('\n')
    }

    private fun sha256(file: File): String = FileInputStream(file).use { input ->
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

    private class CountingInputStream(input: InputStream, private val limit: Long) : java.io.FilterInputStream(input) {
        var bytesRead: Long = 0
            private set
        override fun read(): Int {
            val value = super.read()
            if (value >= 0) add(1)
            return value
        }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val value = super.read(buffer, offset, length)
            if (value > 0) add(value)
            return value
        }
        private fun add(count: Int) {
            bytesRead += count
            require(bytesRead <= limit) { "Source file exceeds configured byte limit" }
        }
    }
}
