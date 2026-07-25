package com.rafgittools.kernel

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.math.max
import kotlin.math.min

/**
 * Read-only health probe for the local Termux/RAFAELIA runtime.
 *
 * Security boundary:
 * - HTTP only;
 * - loopback hosts only;
 * - GET only;
 * - bounded timeout and response body;
 * - no redirects, cookies, credentials or arbitrary headers.
 *
 * An unreachable endpoint is TOKEN_VAZIO, not FAIL: absence of transport
 * evidence does not prove that Termux itself is broken.
 */
class TermuxHealthProbe(
    private val connectionFactory: ConnectionFactory = ConnectionFactory { url ->
        url.openConnection() as HttpURLConnection
    },
    private val nanoTime: () -> Long = System::nanoTime,
) {
    fun interface ConnectionFactory {
        fun open(url: URL): HttpURLConnection
    }

    enum class State {
        PASS,
        FAIL,
        TOKEN_VAZIO,
        ERROR,
    }

    data class Result(
        val state: State,
        val endpoint: String,
        val transport: String = "localhost_http",
        val responseCode: Int? = null,
        val latencyMs: Long? = null,
        val body: String? = null,
        val reason: String? = null,
    )

    fun probe(
        endpoint: String = DEFAULT_ENDPOINT,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS,
    ): Result {
        val normalized = try {
            normalizeEndpoint(endpoint)
        } catch (error: IllegalArgumentException) {
            return Result(
                state = State.ERROR,
                endpoint = endpoint,
                reason = "invalid_endpoint:${error.message ?: "unknown"}",
            )
        }

        val timeout = min(MAX_TIMEOUT_MS, max(MIN_TIMEOUT_MS, timeoutMs))
        val started = nanoTime()
        var connection: HttpURLConnection? = null

        return try {
            connection = connectionFactory.open(URI(normalized).toURL()).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false
                useCaches = false
                doInput = true
                doOutput = false
                connectTimeout = timeout
                readTimeout = timeout
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-store")
            }

            val code = connection.responseCode
            val body = readBoundedBody(connection, code)
            val latency = elapsedMillis(started)

            if (code in 200..299) {
                Result(
                    state = State.PASS,
                    endpoint = normalized,
                    responseCode = code,
                    latencyMs = latency,
                    body = body,
                )
            } else {
                Result(
                    state = State.FAIL,
                    endpoint = normalized,
                    responseCode = code,
                    latencyMs = latency,
                    body = body,
                    reason = "runtime_reported_unhealthy",
                )
            }
        } catch (error: IOException) {
            Result(
                state = State.TOKEN_VAZIO,
                endpoint = normalized,
                latencyMs = elapsedMillis(started),
                reason = "runtime_unreachable:${error.javaClass.simpleName}",
            )
        } catch (error: SecurityException) {
            Result(
                state = State.ERROR,
                endpoint = normalized,
                latencyMs = elapsedMillis(started),
                reason = "transport_denied:${error.javaClass.simpleName}",
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun elapsedMillis(started: Long): Long =
        max(0L, (nanoTime() - started) / 1_000_000L)

    private fun readBoundedBody(connection: HttpURLConnection, responseCode: Int): String? {
        val stream = try {
            if (responseCode >= 400) connection.errorStream else connection.inputStream
        } catch (_: IOException) {
            null
        } ?: return null

        return stream.use { input ->
            val output = ByteArrayOutputStream(min(MAX_BODY_BYTES, 512))
            val buffer = ByteArray(512)
            var remaining = MAX_BODY_BYTES
            while (remaining > 0) {
                val read = input.read(buffer, 0, min(buffer.size, remaining))
                if (read <= 0) break
                output.write(buffer, 0, read)
                remaining -= read
            }
            output.toString(StandardCharsets.UTF_8.name())
        }
    }

    companion object {
        const val DEFAULT_ENDPOINT = "http://127.0.0.1:8765/health"
        const val DEFAULT_TIMEOUT_MS = 350
        const val MIN_TIMEOUT_MS = 50
        const val MAX_TIMEOUT_MS = 1_000
        const val MAX_BODY_BYTES = 4_096

        private val ALLOWED_HOSTS = setOf("127.0.0.1", "localhost", "::1", "[::1]")
        private val ALLOWED_PATHS = setOf("/health", "/v1/health")

        fun normalizeEndpoint(raw: String): String {
            val uri = try {
                URI(raw.trim())
            } catch (error: Exception) {
                throw IllegalArgumentException("malformed_uri", error)
            }

            require(uri.scheme.equals("http", ignoreCase = true)) { "scheme_not_http" }
            require(uri.userInfo == null) { "userinfo_forbidden" }
            require(uri.query == null) { "query_forbidden" }
            require(uri.fragment == null) { "fragment_forbidden" }

            val host = uri.host?.lowercase() ?: throw IllegalArgumentException("host_missing")
            require(host in ALLOWED_HOSTS) { "host_not_loopback" }

            val port = if (uri.port == -1) 8765 else uri.port
            require(port in 1_024..65_535) { "port_out_of_range" }

            val path = if (uri.path.isNullOrBlank()) "/health" else uri.path
            require(path in ALLOWED_PATHS) { "path_not_allowed" }

            val normalizedHost = if (host == "::1" || host == "[::1]") "[::1]" else host
            return "http://$normalizedHost:$port$path"
        }
    }
}
