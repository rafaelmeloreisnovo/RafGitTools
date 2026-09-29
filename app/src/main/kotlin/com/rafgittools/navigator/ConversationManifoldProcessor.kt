package com.rafgittools.navigator

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.io.ByteArrayOutputStream
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStreamWriter
import java.io.PushbackInputStream
import java.security.DigestInputStream
import java.security.MessageDigest

/**
 * Restartable file-level processor for conversations*.json and codex*.json exports.
 * Reads top-level JSON arrays and holds at most one bounded top-level record in memory.
 * Source streams are read-only; successful derived JSONL output is private by default.
 */
class ConversationManifoldProcessor(
    private val outputRoot: File,
    private val maxSourceBytes: Long = 2L * 1024L * 1024L * 1024L,
    private val maxRecordUtf8Bytes: Long = 16L * 1024L * 1024L
) {
    data class Result(
        val state: String, val sourceName: String, val sourceSha256: String, val sourceBytes: Long,
        val records: Long, val nodes: Long, val messages: Long, val codexRecords: Long,
        val outputFile: File, val outputSha256: String, val checkpointFile: File
    )
    private data class Counts(var records: Long = 0, var nodes: Long = 0, var messages: Long = 0, var codex: Long = 0)
    private val gson = Gson()

    fun process(sourceName: String, source: InputStream): Result {
        require(sourceName.endsWith(".json", ignoreCase = true)) { "Only JSON source files are supported" }
        require(sourceName.startsWith("conversations", ignoreCase = true) || sourceName.startsWith("codex", ignoreCase = true)) {
            "Expected conversations*.json or codex*.json"
        }
        if (!outputRoot.exists() && !outputRoot.mkdirs()) error("Cannot create private output directory")
        require(outputRoot.isDirectory) { "Output path is not a directory" }

        val safeName = sourceName.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_")
        val partFile = File(outputRoot, ".$safeName.part")
        if (partFile.exists() && !partFile.delete()) error("Cannot clear interrupted partial output")
        val digest = MessageDigest.getInstance("SHA-256")
        val counts = Counts()
        lateinit var counted: CountingInputStream

        try {
            val digesting = DigestInputStream(source, digest)
            counted = CountingInputStream(digesting, maxSourceBytes)
            val input = PushbackInputStream(counted, 1)
            val first = nextNonWhitespace(input)
            require(first == '['.code) { "Expected top-level JSON array; unsupported source shape is blocked" }

            FileOutputStream(partFile).use { fileOut ->
                BufferedWriter(OutputStreamWriter(fileOut, Charsets.UTF_8), 64 * 1024).use { out ->
                    var finished = false
                    while (!finished) {
                        val next = nextNonWhitespace(input)
                        if (next == ']'.code) {
                            finished = true
                        } else {
                            require(next >= 0) { "Unexpected EOF inside JSON array" }
                            input.unread(next)
                            val rawRecord = readOneValue(input, maxRecordUtf8Bytes)
                            val element = com.google.gson.JsonParser.parseString(rawRecord)
                            writeRecord(safeName, element, out, counts)
                            when (nextNonWhitespace(input)) {
                                ','.code -> Unit
                                ']'.code -> finished = true
                                else -> error("Expected comma or closing array bracket")
                            }
                        }
                    }
                    require(nextNonWhitespace(input) < 0) { "Trailing content after top-level JSON array" }
                    out.flush()
                    fileOut.fd.sync()
                }
            }
        } catch (t: Throwable) {
            partFile.delete()
            throw t
        }

        val sourceHash = digest.digest().hex()
        val sourceBytes = counted.bytesRead
        val outputHash = sha256(partFile)
        val finalFile = File(outputRoot, "$safeName.${sourceHash.take(16)}.derived.jsonl")
        if (finalFile.exists() && sha256(finalFile) != outputHash) {
            partFile.delete()
            error("Content-addressed output collision")
        }
        if (finalFile.exists()) partFile.delete()
        else if (!partFile.renameTo(finalFile)) {
            partFile.delete()
            error("Cannot atomically promote processed output")
        }

        val checkpoint = File(outputRoot, "CHECKPOINTS.jsonl")
        FileOutputStream(checkpoint, true).bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.append(gson.toJson(mapOf(
                "event" to "FILE_COMPLETE", "source_name" to safeName, "source_sha256" to sourceHash,
                "source_bytes" to sourceBytes, "output_file" to finalFile.name, "output_sha256" to outputHash,
                "records" to counts.records, "nodes" to counts.nodes, "messages" to counts.messages,
                "codex_records" to counts.codex, "claim_allowed" to false
            ))).append('\n')
        }
        return Result("COMPLETE", safeName, sourceHash, sourceBytes, counts.records, counts.nodes,
            counts.messages, counts.codex, finalFile, outputHash, checkpoint)
    }

    private fun readOneValue(input: PushbackInputStream, maxBytes: Long): String {
        val first = input.read()
        require(first >= 0) { "Unexpected EOF before JSON record" }
        val out = ByteArrayOutputStream()
        var depth = 0
        var inString = false
        var escaped = false
        var startedContainer = first == '{'.code || first == '['.code
        var primitive = !startedContainer && first != '"'.code
        var endedString = first == '"'.code
        var endedContainer = false

        fun add(value: Int) {
            out.write(value)
            require(out.size().toLong() <= maxBytes) { "Single JSON record exceeds configured memory bound" }
        }
        add(first)
        if (startedContainer) depth = 1
        if (first == '"'.code) { inString = true; endedString = false }

        while (true) {
            val value = input.read()
            if (value < 0) break
            if (primitive && (value == ','.code || value == ']'.code || value.isWhitespaceByte())) {
                input.unread(value)
                break
            }
            add(value)
            if (inString) {
                if (escaped) escaped = false
                else if (value == '\\'.code) escaped = true
                else if (value == '"'.code) {
                    inString = false
                    if (!startedContainer) { endedString = true; break }
                }
            } else if (value == '"'.code) {
                inString = true
            } else if (value == '{'.code || value == '['.code) {
                depth++
                startedContainer = true
            } else if (value == '}'.code || value == ']'.code) {
                depth--
                require(depth >= 0) { "Mismatched JSON container" }
                if (startedContainer && depth == 0) { endedContainer = true; break }
            }
        }
        require((startedContainer && endedContainer) || (!startedContainer && (endedString || primitive))) {
            "Truncated JSON record"
        }
        return out.toString(Charsets.UTF_8.name())
    }

    private fun Int.isWhitespaceByte() = this == ' '.code || this == '\n'.code || this == '\r'.code || this == '\t'.code

    private fun nextNonWhitespace(input: PushbackInputStream): Int {
        while (true) {
            val value = input.read()
            if (!value.isWhitespaceByte()) return value
        }
    }

    private fun writeRecord(sourceName: String, element: JsonElement, out: BufferedWriter, counts: Counts) {
        val encoded = gson.toJson(element)
        val recordIndex = counts.records++
        if (!sourceName.startsWith("conversations", ignoreCase = true)) {
            val recordHash = sha256(encoded.toByteArray(Charsets.UTF_8))
            val pieces = splitUtf8(encoded, 64 * 1024)
            pieces.forEachIndexed { index, piece ->
                writeLine(out, mapOf("kind" to "CODEX_RECORD_PART", "source_name" to sourceName,
                    "record_index" to recordIndex, "part_index" to index, "part_count" to pieces.size,
                    "record_sha256" to recordHash, "json_fragment" to piece,
                    "privacy_class" to "PRIVATE_DEFAULT_DENY", "claim_allowed" to false))
            }
            counts.codex++
            return
        }
        if (!element.isJsonObject) {
            writeLine(out, mapOf("kind" to "CONVERSATION_RECORD", "source_name" to sourceName,
                "record_index" to recordIndex, "record" to element, "claim_allowed" to false))
            return
        }
        val conversation = element.asJsonObject
        val conversationId = scalar(conversation.get("id") ?: conversation.get("conversation_id")) ?: "TOKEN_VAZIO"
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
            val message = node.getAsJsonObject("message")
            writeLine(out, mapOf("kind" to "NODE", "conversation_id" to conversationId, "node_id" to nodeId,
                "parent_id" to parent, "has_message" to (message != null), "source_name" to sourceName,
                "record_index" to recordIndex, "claim_allowed" to false))
            counts.nodes++
            if (message != null) {
                val author = message.getAsJsonObject("author")
                val content = message.getAsJsonObject("content")
                val messageId = scalar(message.get("id")) ?: nodeId
                val text = content?.get("parts")?.let(::flattenText) ?: "TOKEN_VAZIO"
                val textParts = splitUtf8(text, 64 * 1024)
                val wholeTextHash = sha256(text.toByteArray(Charsets.UTF_8))
                textParts.forEachIndexed { index, textPart ->
                    writeLine(out, mapOf("kind" to "MESSAGE_CHUNK", "conversation_id" to conversationId,
                        "message_id" to messageId, "node_id" to nodeId, "parent_id" to parent,
                        "role" to scalar(author?.get("role")) ?: "TOKEN_VAZIO",
                        "created_at" to scalar(message.get("create_time")) ?: "TOKEN_VAZIO",
                        "content_type" to scalar(content?.get("content_type")) ?: "TOKEN_VAZIO",
                        "chunk_index" to index, "chunk_count" to textParts.size,
                        "message_sha256" to wholeTextHash, "text" to textPart,
                        "chunk_sha256" to sha256(textPart.toByteArray(Charsets.UTF_8)),
                        "source_name" to sourceName, "record_index" to recordIndex,
                        "privacy_class" to "PRIVATE_DEFAULT_DENY", "claim_allowed" to false))
                }
                counts.messages++
            }
        }
    }

    private fun splitUtf8(value: String, maxBytes: Int): List<String> {
        require(maxBytes > 0)
        if (value.isEmpty()) return listOf("")
        val result = mutableListOf<String>()
        var start = 0
        var offset = 0
        var byteCount = 0
        while (offset < value.length) {
            val codePoint = value.codePointAt(offset)
            val chars = Character.charCount(codePoint)
            val pieceBytes = String(Character.toChars(codePoint)).toByteArray(Charsets.UTF_8).size
            if (byteCount + pieceBytes > maxBytes && offset > start) {
                result += value.substring(start, offset)
                start = offset
                byteCount = 0
            }
            byteCount += pieceBytes
            offset += chars
        }
        if (start < value.length) result += value.substring(start)
        return result
    }

    private fun flattenText(element: JsonElement): String = when {
        element.isJsonNull -> ""
        element.isJsonPrimitive -> element.asJsonPrimitive.asString
        element.isJsonArray -> element.joinToString("\n") { flattenText(it) }
        element.isJsonObject -> element.asJsonObject.get("text")?.let(::flattenText)
            ?: element.asJsonObject.get("transcript")?.let(::flattenText) ?: ""
        else -> ""
    }
    private fun scalar(element: JsonElement?): String? =
        if (element == null || element.isJsonNull || !element.isJsonPrimitive) null else element.asJsonPrimitive.asString
    private fun writeLine(out: BufferedWriter, value: Any) { out.append(gson.toJson(value)).append('\n') }

    private fun sha256(file: File): String = FileInputStream(file).use { input ->
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        while (true) { val read = input.read(buffer); if (read < 0) break; if (read > 0) md.update(buffer, 0, read) }
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
