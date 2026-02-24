package com.rafgittools.core.error

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ErrorDetailsCodecTest {

    @Test
    fun `serialize and deserialize preserves special characters`() {
        val error = ErrorDetails(
            type = ErrorType.IO_ERROR,
            message = "quote=\"x\" path=C:\\\\tmp\\\\file\nUnicode=ç漢字🚀",
            context = "line1\nline2\\context\"quoted\"",
            timestamp = 456L,
            stackTrace = "trace\\nwith\"quotes\""
        )

        val serialized = ErrorDetailsCodec.serialize(listOf(error))
        val deserialized = ErrorDetailsCodec.deserialize(serialized)

        assertThat(deserialized).containsExactly(error)
    }

    @Test
    fun `deserialize invalid legacy payload returns empty list`() {
        val invalidLegacy = "[{\"type\":\"IO_ERROR\",\"message\":\"unterminated"

        val deserialized = ErrorDetailsCodec.deserialize(invalidLegacy)

        assertThat(deserialized).isEmpty()
    }
}
