package com.rafgittools.navigator

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationManifoldPublicationTest {
    private val abcSha256 =
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

    @Test fun acceptsExactReadbackBytesAndSha256() {
        val bytes = "abc".toByteArray(Charsets.UTF_8)
        ConversationManifoldPublication.verifyReadbackBytes(
            expectedBytes = bytes.size.toLong(),
            expectedSha256 = abcSha256,
            readback = bytes,
            label = "fixture"
        )
        assertTrue(true)
    }

    @Test fun rejectsReadbackHashMismatch() {
        val ok = runCatching {
            ConversationManifoldPublication.verifyReadbackBytes(
                expectedBytes = 3,
                expectedSha256 = abcSha256,
                readback = "abd".toByteArray(Charsets.UTF_8),
                label = "fixture"
            )
        }.isSuccess
        assertFalse(ok)
    }

    @Test fun rejectsReadbackByteCountMismatch() {
        val ok = runCatching {
            ConversationManifoldPublication.verifyReadbackBytes(
                expectedBytes = 4,
                expectedSha256 = abcSha256,
                readback = "abc".toByteArray(Charsets.UTF_8),
                label = "fixture"
            )
        }.isSuccess
        assertFalse(ok)
    }
}
