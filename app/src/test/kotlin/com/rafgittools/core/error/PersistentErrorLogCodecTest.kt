package com.rafgittools.core.error

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test

class PersistentErrorLogCodecTest {

    private val gson = Gson()

    @Test
    fun `serialize and deserialize preserve special characters`() {
        val error = ErrorDetails(
            type = ErrorType.IO_ERROR,
            message = "falha \"quoted\"\\path\nlinha Ω",
            context = "ctx\\srv\nuser=Δ",
            timestamp = 42L
        )

        val json = PersistentErrorLogCodec.serialize(listOf(error), gson)
        val decoded = PersistentErrorLogCodec.deserialize(json, gson)

        assertThat(decoded).containsExactly(error.copy(stackTrace = null))
    }

    @Test
    fun `deserialize supports legacy array payload`() {
        val legacyJson = """
            [{"type":"NETWORK_ERROR","message":"m\\nç","context":"c\\\\path","timestamp":99}]
        """.trimIndent()

        val decoded = PersistentErrorLogCodec.deserialize(legacyJson, gson)

        assertThat(decoded).hasSize(1)
        assertThat(decoded.first().type).isEqualTo(ErrorType.NETWORK_ERROR)
        assertThat(decoded.first().message).isEqualTo("m\nç")
        assertThat(decoded.first().context).isEqualTo("c\\path")
    }

    @Test
    fun `deserialize returns empty list when malformed legacy payload fails`() {
        val malformedLegacy = "[{\"type\":\"UNKNOWN_ERROR\","

        val decoded = PersistentErrorLogCodec.deserialize(malformedLegacy, gson)

        assertThat(decoded).isEmpty()
    }
}
