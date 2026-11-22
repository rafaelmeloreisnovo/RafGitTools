package com.rafgittools.navigator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NovoexportSafInventoryTest {
    @Test fun acceptsConversationAndCodexJsonFamilies() {
        assertTrue(NovoexportSafInventory.acceptsSourceName("conversations.json"))
        assertTrue(NovoexportSafInventory.acceptsSourceName("conversation-001.json"))
        assertTrue(NovoexportSafInventory.acceptsSourceName("CODEX_004.JSON"))
        assertTrue(NovoexportSafInventory.acceptsSourceName("folder/codex.json"))
    }

    @Test fun rejectsUnrelatedOrNonJsonFiles() {
        assertFalse(NovoexportSafInventory.acceptsSourceName("manifest.json"))
        assertFalse(NovoexportSafInventory.acceptsSourceName("conversation.txt"))
        assertFalse(NovoexportSafInventory.acceptsSourceName("codex.json.zip"))
        assertFalse(NovoexportSafInventory.acceptsSourceName("TOKEN_VAZIO"))
    }
    @Test fun driveProviderMayOmitOptionalSizeColumn() {
        // A missing cloud column MUST NOT dereference the cursor.
        val unknown = NovoexportSafInventory.optionalFileSize(
            -1,
            isNull = { error("isNull must not run on missing column") },
            readValue = { error("getLong must not run on missing column") }
        )
        assertNull(unknown)
    }

    @Test fun nullOrUnreportedCloudSizeIsNotZeroBytes() {
        assertNull(NovoexportSafInventory.optionalFileSize(
            2,
            isNull = { true },
            readValue = { error("getLong must not run on null column") }
        ))
        assertNull(NovoexportSafInventory.optionalFileSize(
            2, isNull = { false }, readValue = { -1L }
        ))
        assertEquals(0L, NovoexportSafInventory.optionalFileSize(
            2, isNull = { false }, readValue = { 0L }
        ))
        assertEquals(4_294_967_296L, NovoexportSafInventory.optionalFileSize(
            2, isNull = { false }, readValue = { 4_294_967_296L }
        ))
    }

    @Test fun childPathsKeepNestedDirectorySegments() {
        assertEquals(
            "NOVOexport/Conversation chunks/2026",
            NovoexportSafInventory.childPath(
                "NOVOexport/Conversation chunks",
                "2026"
            )
        )
        assertEquals("NOVOexport", NovoexportSafInventory.childPath("", "NOVOexport"))
    }

}
