package com.rafgittools.kernel

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class TermuxHealthProbeTest {
    @Test
    fun normalizeEndpoint_acceptsOnlyLoopbackHealthPaths() {
        assertThat(TermuxHealthProbe.normalizeEndpoint("http://127.0.0.1:8765/health"))
            .isEqualTo("http://127.0.0.1:8765/health")
        assertThat(TermuxHealthProbe.normalizeEndpoint("http://localhost/v1/health"))
            .isEqualTo("http://localhost:8765/v1/health")
        assertThat(TermuxHealthProbe.normalizeEndpoint("http://[::1]:9876/health"))
            .isEqualTo("http://[::1]:9876/health")
    }

    @Test
    fun normalizeEndpoint_rejectsNonLoopbackAndAmbiguousUris() {
        listOf(
            "https://127.0.0.1:8765/health",
            "http://example.com:8765/health",
            "http://127.0.0.1:80/health",
            "http://user@127.0.0.1:8765/health",
            "http://127.0.0.1:8765/health?secret=1",
            "http://127.0.0.1:8765/admin",
        ).forEach { endpoint ->
            assertThrows(IllegalArgumentException::class.java) {
                TermuxHealthProbe.normalizeEndpoint(endpoint)
            }
        }
    }

    @Test
    fun probe_returnsPassForSuccessfulBoundedResponse() {
        val connection = FakeConnection(
            url = URL("http://127.0.0.1:8765/health"),
            code = 200,
            payload = "{\"status\":\"ok\"}".toByteArray(),
        )
        val probe = TermuxHealthProbe(
            connectionFactory = TermuxHealthProbe.ConnectionFactory { connection },
            nanoTime = SequenceClock(1_000_000_000L, 1_125_000_000L),
        )

        val result = probe.probe()

        assertThat(result.state).isEqualTo(TermuxHealthProbe.State.PASS)
        assertThat(result.responseCode).isEqualTo(200)
        assertThat(result.latencyMs).isEqualTo(125L)
        assertThat(result.body).isEqualTo("{\"status\":\"ok\"}")
        assertThat(connection.requestMethod).isEqualTo("GET")
        assertThat(connection.instanceFollowRedirects).isFalse()
        assertThat(connection.doOutput).isFalse()
        assertThat(connection.disconnected).isTrue()
    }

    @Test
    fun probe_returnsFailWhenRuntimeAnswersUnhealthy() {
        val connection = FakeConnection(
            url = URL("http://127.0.0.1:8765/health"),
            code = 503,
            payload = "degraded".toByteArray(),
        )
        val probe = TermuxHealthProbe(
            connectionFactory = TermuxHealthProbe.ConnectionFactory { connection },
            nanoTime = SequenceClock(0L, 10_000_000L),
        )

        val result = probe.probe()

        assertThat(result.state).isEqualTo(TermuxHealthProbe.State.FAIL)
        assertThat(result.responseCode).isEqualTo(503)
        assertThat(result.reason).isEqualTo("runtime_reported_unhealthy")
        assertThat(result.body).isEqualTo("degraded")
    }

    @Test
    fun probe_preservesUnreachableRuntimeAsTokenVazio() {
        val connection = FakeConnection(
            url = URL("http://127.0.0.1:8765/health"),
            code = 0,
            payload = byteArrayOf(),
            responseFailure = IOException("connection refused"),
        )
        val probe = TermuxHealthProbe(
            connectionFactory = TermuxHealthProbe.ConnectionFactory { connection },
            nanoTime = SequenceClock(0L, 25_000_000L),
        )

        val result = probe.probe()

        assertThat(result.state).isEqualTo(TermuxHealthProbe.State.TOKEN_VAZIO)
        assertThat(result.responseCode).isNull()
        assertThat(result.reason).isEqualTo("runtime_unreachable:IOException")
        assertThat(result.latencyMs).isEqualTo(25L)
    }

    @Test
    fun probe_rejectsEndpointBeforeOpeningTransport() {
        var opened = false
        val probe = TermuxHealthProbe(
            connectionFactory = TermuxHealthProbe.ConnectionFactory {
                opened = true
                throw AssertionError("transport must not open")
            },
        )

        val result = probe.probe("http://example.com:8765/health")

        assertThat(result.state).isEqualTo(TermuxHealthProbe.State.ERROR)
        assertThat(result.reason).startsWith("invalid_endpoint:")
        assertThat(opened).isFalse()
    }

    @Test
    fun responseBodyIsBoundedToFourKilobytes() {
        val payload = ByteArray(TermuxHealthProbe.MAX_BODY_BYTES + 512) { 'x'.code.toByte() }
        val connection = FakeConnection(
            url = URL("http://127.0.0.1:8765/health"),
            code = 200,
            payload = payload,
        )
        val probe = TermuxHealthProbe(
            connectionFactory = TermuxHealthProbe.ConnectionFactory { connection },
            nanoTime = SequenceClock(0L, 1_000_000L),
        )

        val result = probe.probe()

        assertThat(result.body).hasLength(TermuxHealthProbe.MAX_BODY_BYTES)
    }

    private class SequenceClock(vararg values: Long) : () -> Long {
        private val samples = values.toList()
        private var index = 0

        override fun invoke(): Long {
            val value = samples.getOrElse(index) { samples.last() }
            index += 1
            return value
        }
    }

    private class FakeConnection(
        url: URL,
        private val code: Int,
        private val payload: ByteArray,
        private val responseFailure: IOException? = null,
    ) : HttpURLConnection(url) {
        var disconnected: Boolean = false
            private set

        override fun connect() = Unit

        override fun disconnect() {
            disconnected = true
        }

        override fun usingProxy(): Boolean = false

        override fun getResponseCode(): Int {
            responseFailure?.let { throw it }
            return code
        }

        override fun getInputStream(): InputStream = ByteArrayInputStream(payload)

        override fun getErrorStream(): InputStream = ByteArrayInputStream(payload)
    }
}
