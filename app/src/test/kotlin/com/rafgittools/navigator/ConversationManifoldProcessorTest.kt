package com.rafgittools.navigator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.file.Files

class ConversationManifoldProcessorTest {
    @Test fun indexesConversationNodesMessagesAndCodexRecords() {
        val root = Files.createTempDirectory("manifold-test").toFile()
        val conversations = """[{"id":"c1","mapping":{"n0":{"parent":null},"n1":{"parent":"n0","message":{"id":"m1","author":{"role":"user"},"content":{"content_type":"text","parts":["hello","world"]}}}}}]"""
        val result = ConversationManifoldProcessor(root).process("conversations-000.json", ByteArrayInputStream(conversations.toByteArray()))
        assertEquals("COMPLETE", result.state)
        assertEquals(1L, result.records)
        assertEquals(2L, result.nodes)
        assertEquals(1L, result.messages)
        val derived = result.outputFile.readText()
        assertTrue(derived.contains("\"kind\":\"CONVERSATION\""))
        assertTrue(derived.contains("\"kind\":\"NODE\""))
        assertTrue(derived.contains("\"kind\":\"MESSAGE_CHUNK\""))
        assertTrue(derived.contains("hello"))
        assertTrue(result.checkpointFile.exists())
        assertTrue(result.sourceSha256.matches(Regex("[0-9a-f]{64}")))
    }

    @Test fun processesCodexJsonAndRejectsUnexpectedSource() {
        val root = Files.createTempDirectory("manifold-codex").toFile()
        val result = ConversationManifoldProcessor(root).process("codex-000.json", ByteArrayInputStream("[{\"task\":\"build\"}]".toByteArray()))
        assertEquals(1L, result.codexRecords)
        assertTrue(result.outputFile.readText().contains("CODEX_RECORD_PART"))
        assertFalse(runCatching { ConversationManifoldProcessor(root).process("other.json", ByteArrayInputStream("[]".toByteArray())) }.isSuccess)
    }

    @Test fun splitsLongMessageIntoReconstructibleChunks() {
        val root = Files.createTempDirectory("manifold-long").toFile()
        val text = "a".repeat(200_000)
        val source = """[{"id":"c","mapping":{"n":{"parent":null,"message":{"id":"m","author":{"role":"user"},"content":{"content_type":"text","parts":["$text"]}}}}}]"""
        val result = ConversationManifoldProcessor(root).process("conversations-001.json", ByteArrayInputStream(source.toByteArray()))
        val lines = result.outputFile.readLines().filter { it.contains("MESSAGE_CHUNK") }
        assertTrue(lines.size > 1)
        val chunks = lines.map { Regex("\"text\":\"([^\"]*)\"").find(it)!!.groupValues[1] }
        assertEquals(text, chunks.joinToString(""))
        assertTrue(lines.all { it.toByteArray().size < 480 * 1024 })
    }

    @Test fun rejectsMalformedJsonAndRemovesPartialOutput() {
        val root = Files.createTempDirectory("manifold-invalid").toFile()
        assertFalse(runCatching { ConversationManifoldProcessor(root).process("conversations-000.json", ByteArrayInputStream("[{".toByteArray())) }.isSuccess)
        assertTrue(root.listFiles().orEmpty().none { it.name.endsWith(".part") })
    }

    @Test fun enforcesSourceAndRecordBounds() {
        val root = Files.createTempDirectory("manifold-bounds").toFile()
        assertFalse(runCatching {
            ConversationManifoldProcessor(root, maxSourceBytes = 4).process("codex.json", ByteArrayInputStream("[12345]".toByteArray()))
        }.isSuccess)
        assertFalse(runCatching {
            ConversationManifoldProcessor(root, maxRecordUtf8Bytes = 4).process("codex.json", ByteArrayInputStream("[{\"a\":1}]".toByteArray()))
        }.isSuccess)
    }

    @Test fun rejectsTrailingCommaInsteadOfPromotingMalformedJson() {
        val root = Files.createTempDirectory("manifold-trailing-comma").toFile()
        assertFalse(runCatching {
            ConversationManifoldProcessor(root).process("codex.json", ByteArrayInputStream("[{\"a\":1},]".toByteArray()))
        }.isSuccess)
    }
}
