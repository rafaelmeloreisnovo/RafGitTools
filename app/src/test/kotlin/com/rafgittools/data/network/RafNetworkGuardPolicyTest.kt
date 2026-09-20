package com.rafgittools.data.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RafNetworkGuardPolicyTest {

    private val policy = RafNetworkGuardPolicy.forBaseUrl(
        baseUrl = "https://api.github.com/",
        mode = RafNetworkGuardMode.ENFORCE
    )

    @Test
    fun allowsExactBaseEndpoint() {
        val d = policy.evaluate("https://api.github.com/repos".toHttpUrl())
        assertTrue(d.allowed)
    }

    @Test
    fun blocksUnknownRemoteHost() {
        val d = policy.evaluate("https://example.invalid/".toHttpUrl())
        assertFalse(d.allowed)
    }

    @Test
    fun blocksRemoteCleartext() {
        val d = policy.evaluate("http://api.github.com/".toHttpUrl())
        assertFalse(d.allowed)
    }

    @Test
    fun allowsLoopbackCleartextForLocalBridge() {
        val d = policy.evaluate("http://127.0.0.1:18080/".toHttpUrl())
        assertTrue(d.allowed)
    }

    @Test
    fun canonicalLogIsMetadataOnly() {
        val line = RafNetworkAuditEvent(
            eventId = "e1",
            phase = RafNetworkAuditPhase.PRE,
            wallTimeMs = 1,
            monotonicNs = 2,
            method = "GET",
            scheme = "https",
            host = "api.github.com",
            port = 443,
            requestBytes = 0,
            responseBytes = null,
            action = "ALLOW",
            ruleId = "BASE_URL",
            reason = "EXPLICIT_ENDPOINT_ALLOW"
        ).toCanonicalLogLine()

        assertTrue(line.contains("payload=REDACTED_METADATA_ONLY"))
        assertFalse(line.contains("Authorization"))
        assertFalse(line.contains("token="))
        assertFalse(line.contains("/repos"))
    }
}
