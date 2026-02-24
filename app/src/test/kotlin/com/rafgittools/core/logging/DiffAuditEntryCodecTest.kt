package com.rafgittools.core.logging

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DiffAuditEntryCodecTest {

    @Test
    fun `serialize and deserialize preserves special characters`() {
        val entry = DiffAuditEntry(
            oldPath = "src/\"old\"\\path\n漢字",
            newPath = "dst/line1\nline2/emoji-🚀",
            changeType = "MODIFIED",
            timestamp = 123L,
            diffSizeBytes = 456L,
            fileSizeBytes = 789L,
            md5 = "hash\\with\"quotes\n"
        )

        val serialized = DiffAuditEntryCodec.serialize(listOf(entry))
        val deserialized = DiffAuditEntryCodec.deserialize(serialized)

        assertThat(deserialized).containsExactly(entry)
    }

    @Test
    fun `deserialize invalid legacy payload returns empty list`() {
        val invalidLegacy = "[{\"oldPath\":\"broken},not-valid-json"

        val deserialized = DiffAuditEntryCodec.deserialize(invalidLegacy)

        assertThat(deserialized).isEmpty()
    }
}
