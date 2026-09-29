package com.rafgittools.navigator

import org.junit.Assert.assertFalse
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
}
