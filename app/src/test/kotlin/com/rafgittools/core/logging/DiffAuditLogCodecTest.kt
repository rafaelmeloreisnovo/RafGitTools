package com.rafgittools.core.logging

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test

class DiffAuditLogCodecTest {

    private val gson = Gson()

    @Test
    fun `serialize and deserialize preserve escaped characters`() {
        val entry = DiffAuditEntry(
            oldPath = "old/\"quoted\"\\path\nlinha-α",
            newPath = "new/linha\nsegura/β",
            changeType = "MODIFY",
            timestamp = 1234L,
            diffSizeBytes = 56L,
            fileSizeBytes = 78L,
            md5 = "hash\\with\"quotes\n終"
        )

        val json = DiffAuditLogCodec.serialize(listOf(entry), gson)
        val decoded = DiffAuditLogCodec.deserialize(json, gson)

        assertThat(decoded).containsExactly(entry)
    }

    @Test
    fun `deserialize supports legacy array payload`() {
        val legacyJson = """
            [{"oldPath":"old\\\\path","newPath":"new\\nline","changeType":"ADD","timestamp":10,"diffSizeBytes":20,"fileSizeBytes":30,"md5":"áéí"}]
        """.trimIndent()

        val decoded = DiffAuditLogCodec.deserialize(legacyJson, gson)

        assertThat(decoded).hasSize(1)
        assertThat(decoded.first().oldPath).isEqualTo("old\\path")
        assertThat(decoded.first().newPath).isEqualTo("new\nline")
        assertThat(decoded.first().md5).isEqualTo("áéí")
    }

    @Test
    fun `deserialize returns empty list for malformed legacy content`() {
        val malformedLegacy = "[{broken}"

        val decoded = DiffAuditLogCodec.deserialize(malformedLegacy, gson)

        assertThat(decoded).isEmpty()
    }
}
