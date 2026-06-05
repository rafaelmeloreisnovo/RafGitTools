package com.rafgittools.webhook

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebhookHandlerTest {

    @Test
    fun `handle returns success for supported push payload`() {
        val payload = """{"ref":"refs/heads/main"}"""

        val result = WebhookHandler.handle("push", payload)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `handle fails for unsupported event`() {
        val payload = """{"any":"field"}"""

        val result = WebhookHandler.handle("deployment_status", payload)

        assertFalse(result.isSuccess)
    }

    @Test
    fun `handle fails for invalid json payload`() {
        val payload = "not-json"

        val result = WebhookHandler.handle("push", payload)

        assertFalse(result.isSuccess)
    }

    @Test
    fun `handle fails when required push fields are missing`() {
        val payload = """{"head":"abc123"}"""

        val result = WebhookHandler.handle("push", payload)

        assertFalse(result.isSuccess)
    }
}
